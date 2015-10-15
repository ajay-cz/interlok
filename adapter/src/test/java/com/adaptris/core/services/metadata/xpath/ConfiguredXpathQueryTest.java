/*
 * Copyright 2015 Adaptris Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.adaptris.core.services.metadata.xpath;

import org.w3c.dom.Document;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.CoreException;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.util.XmlHelper;

public class ConfiguredXpathQueryTest extends ConfiguredXpathQueryCase {

  public ConfiguredXpathQueryTest(String testName) {
    super(testName);
  }

  @Override
  protected ConfiguredXpathQuery create() {
    return new ConfiguredXpathQuery();
  }

  private ConfiguredXpathQuery init(ConfiguredXpathQuery query, String xpathQuery) throws CoreException {
    query.setMetadataKey("result");
    query.setXpathQuery(xpathQuery);
    query.verify();
    return query;
  }

  public void testResolveXpath_EmptyResults_NotAllowed() throws Exception {
    ConfiguredXpathQuery query = init(create(), "//@MissingAttribute");
    Document doc = XmlHelper.createDocument(XML);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(XML);
    try {
      MetadataElement result = query.resolveXpath(doc, null, query.createXpathQuery(msg));
      fail();
    }
    catch (CoreException expected) {

    }
  }

  public void testResolveXpath_EmptyResults_Allowed() throws Exception {
    ConfiguredXpathQuery query = init(create(), "//@MissingAttribute");
    query.setAllowEmptyResults(Boolean.TRUE);
    Document doc = XmlHelper.createDocument(XML);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(XML);
    MetadataElement result = query.resolveXpath(doc, null, query.createXpathQuery(msg));
    assertEquals("", result.getValue());
  }

  public void testResolveXpath_Attribute() throws Exception {
    ConfiguredXpathQuery query = init(create(), "//@att");
    Document doc = XmlHelper.createDocument(XML);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(XML);
    MetadataElement result = query.resolveXpath(doc, null, query.createXpathQuery(msg));
  }

  public void testResolveXpath_function() throws Exception {
    ConfiguredXpathQuery query = init(create(), "count(/message)");
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(XML);
    Document doc = XmlHelper.createDocument(XML);
    MetadataElement result = query.resolveXpath(doc, null, query.createXpathQuery(msg));
    assertEquals("1", result.getValue());
  }

  // Invalid test
  // Namedspaced document with non-namespace xpath never matches with SAXON
  // public void testResolveXpath_NamespaceNoNamespaceContext() throws Exception {
  // ConfiguredXpathQuery query = init(create(), "count(/schematron-output/failed-assert)");
  // Document doc = XmlHelper.createDocument(XML_WITH_NAMESPACE);
  // AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(XML_WITH_NAMESPACE, "UTF-8");
  //
  // MetadataElement result = query.resolveXpath(doc, null, query.createXpathQuery(msg));
  // assertEquals("2", result.getValue());
  // }

  public void testResolveXpath_NamespaceWithNamespaceContext() throws Exception {
    ConfiguredXpathQuery query = init(create(), "count(/svrl:schematron-output/svrl:failed-assert)");

    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(XML_WITH_NAMESPACE);
    StaticNamespaceContext ctx = new StaticNamespaceContext();
    Document doc = XmlHelper.createDocument(XML_WITH_NAMESPACE, ctx);
    MetadataElement result = query.resolveXpath(doc, ctx, query.createXpathQuery(msg));
    assertEquals("2", result.getValue());
  }

}
