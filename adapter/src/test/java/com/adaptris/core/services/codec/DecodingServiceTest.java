package com.adaptris.core.services.codec;

import com.adaptris.core.*;
import com.adaptris.core.stubs.MockEncoder;
import com.adaptris.core.stubs.StubMessageFactory;
import com.adaptris.core.util.LifecycleHelper;

public class DecodingServiceTest extends CodecServiceCase {

  private static final String OVERRIDE_HEADER_VALUE = "value";

  public DecodingServiceTest(String name) {
    super(name);
  }

  public void testInit() throws Exception {
    DecodingService service = new DecodingService();
    try {
      LifecycleHelper.init(service);
      fail();
    }
    catch (CoreException expected) {
    }
    service.setEncoder(new MockEncoder());
    LifecycleHelper.init(service);
    service = new DecodingService(new MockEncoder());
    LifecycleHelper.init(service);
  }

  public void testSetEncoder() throws Exception {
    DecodingService s = new DecodingService();
    assertNull(s.getEncoder());
    MockEncoder me = new MockEncoder();
    s = new DecodingService(me);
    assertEquals(me, s.getEncoder());
    s = new DecodingService();
    s.setEncoder(me);
    assertEquals(me, s.getEncoder());
  }

  public void testSetMessageFactory() throws Exception {
    DecodingService s = new DecodingService();
    assertNull(s.getMessageFactory());
    s = new DecodingService(new MockEncoder());
    assertNull(s.getMessageFactory());
    assertTrue(s.getEncoder().currentMessageFactory() instanceof DefaultMessageFactory);
    s = new DecodingService(new MockEncoder());
    AdaptrisMessageFactory amf = new StubMessageFactory();
    s.setMessageFactory(amf);
    assertEquals(amf, s.getMessageFactory());
    assertTrue(s.getEncoder().currentMessageFactory() instanceof StubMessageFactory);
    assertEquals(amf, s.getEncoder().currentMessageFactory());
  }

  public void testSetOverrideMetadata() throws Exception {

    DecodingService s = new DecodingService();

    assertNull(s.getOverrideMetadata());
    assertFalse(s.isOverrideMetadata());

    s.setOverrideMetadata(true);
    assertTrue(s.isOverrideMetadata());

    s.setOverrideMetadata(false);
    assertFalse(s.isOverrideMetadata());
  }

  public void testMockEncoder() throws Exception {
    DecodingService service = new DecodingService(new MockEncoder());
    AdaptrisMessage msg = createSimpleMessage();
    execute(service, msg);
    assertEquals(TEST_PAYLOAD, new String(msg.getPayload()));
  }

  public void testMimeEncoder() throws Exception {
    DecodingService service = new DecodingService(new MimeEncoder());
    AdaptrisMessage msg = createMimeMessage();
    assertFalse(msg.headersContainsKey(TEST_METADATA_KEY));
    assertFalse(msg.headersContainsKey(TEST_METADATA_KEY_2));
    execute(service, msg);
    assertTrue(msg.headersContainsKey(TEST_METADATA_KEY));
    assertTrue(msg.headersContainsKey(TEST_METADATA_KEY_2));
    assertEquals(TEST_METADATA_VALUE, msg.getMetadataValue(TEST_METADATA_KEY));
    assertEquals(TEST_METADATA_VALUE_2, msg.getMetadataValue(TEST_METADATA_KEY_2));
    assertEquals(TEST_PAYLOAD, new String(msg.getPayload()));
  }

  public void testMimeEncoder_OverrideHeader() throws Exception {
    DecodingService service = new DecodingService(new MimeEncoder());
    service.setOverrideMetadata(true);
    AdaptrisMessage msg = createMimeMessage();
    msg.addMetadata(TEST_METADATA_KEY, OVERRIDE_HEADER_VALUE);
    assertTrue(msg.headersContainsKey(TEST_METADATA_KEY));
    assertEquals(OVERRIDE_HEADER_VALUE, msg.getMetadataValue(TEST_METADATA_KEY));
    execute(service, msg);
    assertTrue(msg.headersContainsKey(TEST_METADATA_KEY));
    assertTrue(msg.headersContainsKey(TEST_METADATA_KEY_2));
    assertEquals(TEST_METADATA_VALUE, msg.getMetadataValue(TEST_METADATA_KEY));
    assertEquals(TEST_METADATA_VALUE_2, msg.getMetadataValue(TEST_METADATA_KEY_2));
    assertEquals(TEST_PAYLOAD, new String(msg.getPayload()));
  }

  public void testMimeEncoder_DoNotOverrideHeader() throws Exception {
    DecodingService service = new DecodingService(new MimeEncoder());
    service.setOverrideMetadata(false);
    AdaptrisMessage msg = createMimeMessage();
    msg.addMetadata(TEST_METADATA_KEY, OVERRIDE_HEADER_VALUE);
    assertTrue(msg.headersContainsKey(TEST_METADATA_KEY));
    assertEquals(OVERRIDE_HEADER_VALUE, msg.getMetadataValue(TEST_METADATA_KEY));
    execute(service, msg);
    assertTrue(msg.headersContainsKey(TEST_METADATA_KEY));
    assertTrue(msg.headersContainsKey(TEST_METADATA_KEY_2));
    assertEquals(OVERRIDE_HEADER_VALUE, msg.getMetadataValue(TEST_METADATA_KEY));
    assertEquals(TEST_METADATA_VALUE_2, msg.getMetadataValue(TEST_METADATA_KEY_2));
    assertEquals(TEST_PAYLOAD, new String(msg.getPayload()));
  }


  @Override
  protected Object retrieveObjectForSampleConfig() {
    DecodingService decodingService = new DecodingService();
    decodingService.setEncoder(new MimeEncoder());
    decodingService.setOverrideMetadata(false);
    return decodingService;
  }

}
