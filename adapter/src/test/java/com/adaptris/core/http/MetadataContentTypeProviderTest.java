package com.adaptris.core.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultMessageFactory;

public class MetadataContentTypeProviderTest {

  @Rule
  public TestName testName = new TestName();

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testGetContentType() throws Exception {
    MetadataContentTypeProvider provider = new MetadataContentTypeProvider(testName.getMethodName());

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("");
    msg.addMetadata(testName.getMethodName(), "text/complicated");

    String contentType = provider.getContentType(msg);
    assertEquals("text/complicated", contentType);
  }


  @Test
  public void testGetContentType_WithCharset() throws Exception {
    MetadataContentTypeProvider provider = new MetadataContentTypeProvider(testName.getMethodName());

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("");
    msg.setCharEncoding("UTF-8");
    msg.addMetadata(testName.getMethodName(), "text/complicated");

    String contentType = provider.getContentType(msg);
    assertEquals("text/complicated; charset=UTF-8", contentType);
  }


  @Test
  public void testGetContentType_MetadataKeyNonExistent() throws Exception {
    MetadataContentTypeProvider provider = new MetadataContentTypeProvider(testName.getMethodName());

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("");
    String contentType = provider.getContentType(msg);
    assertEquals("text/plain", contentType);
  }

  @Test
  public void testGetContentType_NullMetadataKey() throws Exception {
    MetadataContentTypeProvider provider = new MetadataContentTypeProvider();

    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("");
    msg.addMetadata(testName.getMethodName(), "text/complicated");
    try {
      String contentType = provider.getContentType(msg);
      fail();
    } catch (CoreException expected) {

    }
  }


}