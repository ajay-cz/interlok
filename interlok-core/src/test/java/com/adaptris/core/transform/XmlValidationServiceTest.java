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

package com.adaptris.core.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.ServiceException;
import com.adaptris.core.cache.ExpiringMapCache;
import com.adaptris.core.services.cache.CacheConnection;
import com.adaptris.core.stubs.MessageHelper;
import com.adaptris.core.util.DocumentBuilderFactoryBuilder;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.transform.validate.NotNullContentValidation;
import com.adaptris.transform.validate.ValidationStage;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;

@SuppressWarnings("deprecation")
public class XmlValidationServiceTest extends TransformServiceExample {

  public static final String KEY_WILL_VALIDATE_SCHEMA = "XmlValidationServiceTest.schemaUrl";
  public static final String KEY_WILL_NOT_VALIDATE = "XmlValidationServiceTest.schemaUrl2";
  public static final String KEY_INVALID_SCHEMA_URL = "XmlValidationServiceTest.invalidSchemaUrl";

  public static final String KEY_INPUT_FILE = "XmlValidationServiceTest.input.xml";

  @Override
  public boolean isAnnotatedForJunit4() {
    return true;
  }

  @Test
  public void testSchemaValidator_SetUrl() {
    XmlSchemaValidator v = new XmlSchemaValidator();
    assertNull(v.getSchema());
    v.setSchema("schema");
    assertEquals("schema", v.getSchema());
    v.setSchema(null);
    assertNull(v.getSchema());

  }

  @Test
  public void testSchemaValidator_SetMetadataKey() {
    XmlSchemaValidator v = new XmlSchemaValidator();
    assertNull(v.getSchemaMetadataKey());
    v.setSchemaMetadataKey("schema");
    assertEquals("schema", v.getSchemaMetadataKey());
    v.setSchemaMetadataKey(null);
    assertNull(v.getSchemaMetadataKey());
  }

  @Test
  public void testSchemaValidator_InitDefault() {
    XmlValidationService service = new XmlValidationService(new XmlSchemaValidator());
    try {
      LifecycleHelper.init(service);
      fail();
    }
    catch (CoreException expected) {
    }
    finally {
      LifecycleHelper.close(service);
    }
  }

  @Test
  public void testSchemaValidator_ValidXmlConfiguredSchemaOnly() throws Exception {
    String schemaUrl = PROPERTIES.getProperty(KEY_WILL_VALIDATE_SCHEMA);
    // if (!ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl))) {
    // log.debug(schemaUrl + " not available, ignoring testValidXmlConfiguredSchemaOnly test");
    // return;
    // }
    XmlSchemaValidator validator = new XmlSchemaValidator(schemaUrl);
    AdaptrisMessage msg = MessageHelper.createMessage(PROPERTIES.getProperty(KEY_INPUT_FILE));
    XmlValidationService service = new XmlValidationService(validator);
    execute(service, msg);
  }

  @Test
  public void testSchemaValidator_ValidSchema_MessageFails() throws Exception {
    String schemaUrl = PROPERTIES.getProperty(KEY_WILL_NOT_VALIDATE);
    // if (!ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl))) {
    // log.debug(schemaUrl + " not available, ignoring testValidXmlConfiguredSchemaOnly test");
    // return;
    // }
    XmlSchemaValidator validator = new XmlSchemaValidator(schemaUrl);
    AdaptrisMessage msg = MessageHelper.createMessage(PROPERTIES.getProperty(KEY_INPUT_FILE));
    XmlValidationService service = new XmlValidationService(validator);
    try {
      execute(service, msg);
      fail();
    }
    catch (ServiceException expected) {

    }
  }

  @Test
  public void testSchemaValidator_MetadataSchemaKeyDoesNotExist() throws Exception {
    String schemaUrl = PROPERTIES.getProperty(KEY_WILL_VALIDATE_SCHEMA);
    // if (!ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl))) {
    // log.debug(schemaUrl + " not available, ignoring testMetadataSchemaKeyDoesNotExist test");
    // return;
    // }
    XmlSchemaValidator validator = new XmlSchemaValidator(schemaUrl, "schema-key");
    AdaptrisMessage msg = MessageHelper.createMessage(PROPERTIES.getProperty(KEY_INPUT_FILE));
    XmlValidationService service = new XmlValidationService(validator);
    try {
      execute(service, msg);
    }
    catch (ServiceException expected) {
    }
  }

  @Test
  public void testSchemaValidator_MetadataSchemaKeyIsEmpty() throws Exception {
    String schemaUrl = PROPERTIES.getProperty(KEY_WILL_VALIDATE_SCHEMA);
    // if (!ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl))) {
    // log.debug(schemaUrl + " not available, ignoring testMetadataSchemaKeyIsEmpty test");
    // return;
    // }

    XmlSchemaValidator validator = new XmlSchemaValidator(schemaUrl, "schema-key");
    AdaptrisMessage msg = MessageHelper.createMessage(PROPERTIES.getProperty(KEY_INPUT_FILE));
    msg.addMetadata("schema-key", "");
    XmlValidationService service = new XmlValidationService(validator);
    try {
      execute(service, msg);
    }
    catch (ServiceException expected) {
    }
  }

  @Test
  public void testValidXmlSchemaInMetadataOnly() throws Exception {
    String schemaUrl = PROPERTIES.getProperty(KEY_WILL_VALIDATE_SCHEMA);
    // if (!ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl))) {
    // log.debug(schemaUrl + " not available, ignoring testMetadataSchemaKeyIsEmpty test");
    // return;
    // }
    XmlSchemaValidator validator = new XmlSchemaValidator();
    validator.setSchemaMetadataKey("schema-key");
    AdaptrisMessage msg = MessageHelper.createMessage(PROPERTIES.getProperty(KEY_INPUT_FILE));
    msg.addMetadata("schema-key", schemaUrl);
    XmlValidationService service = new XmlValidationService(validator);
    execute(service, msg);
  }

  @Test
  public void testValidXmlSchema_Expression() throws Exception {
    String schemaUrl = PROPERTIES.getProperty(KEY_WILL_VALIDATE_SCHEMA);
    // if (!ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl))) {
    // log.debug(schemaUrl + " not available, ignoring testMetadataSchemaKeyIsEmpty test");
    // return;
    // }
    XmlSchemaValidator validator = new XmlSchemaValidator("%message{schema-key}")
        .withSchemaCache(new CacheConnection(new ExpiringMapCache().withMaxEntries(1)));
    AdaptrisMessage msg = MessageHelper.createMessage(PROPERTIES.getProperty(KEY_INPUT_FILE));
    msg.addMetadata("schema-key", schemaUrl);
    XmlValidationService service = new XmlValidationService(validator);
    try {
      LifecycleHelper.initAndStart(service);
      service.doService(msg);
      // Hits the cache the 2nd time round
      service.doService(msg);
    } finally {
      LifecycleHelper.stopAndClose(service);
    }
  }

  @Test
  public void testValidXmlSchemaInMetadataAndConfiguredSchema() throws Exception {
    String schemaUrl = PROPERTIES.getProperty(KEY_WILL_VALIDATE_SCHEMA);
    String schemaUrl2 = PROPERTIES.getProperty(KEY_WILL_NOT_VALIDATE);
    // if (!ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl))
    // || !ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl2))) {
    // log.debug(schemaUrl + " or " + schemaUrl2 + " not available, ignoring testValidXmlSchemaInMetadataAndConfiguredSchema test");
    // return;
    // }

    XmlSchemaValidator validator = new XmlSchemaValidator();
    validator.setSchema(schemaUrl2);
    validator.setSchemaMetadataKey("schema-key");
    AdaptrisMessage msg = MessageHelper.createMessage(PROPERTIES.getProperty(KEY_INPUT_FILE));
    msg.addMetadata("schema-key", schemaUrl);
    XmlValidationService service = new XmlValidationService(validator);

    execute(service, msg);
  }

  @Test
  public void testSchemaValidator_InvalidXml() throws Exception {
    String schemaUrl = PROPERTIES.getProperty(KEY_WILL_VALIDATE_SCHEMA);
    // if (!ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl))) {
    // log.debug(schemaUrl + " not available, ignoring testInvalidXml test");
    // return;
    // }
    XmlSchemaValidator validator = new XmlSchemaValidator();
    validator.setSchema(schemaUrl);
    XmlValidationService service = new XmlValidationService(validator);
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("<?xml version=\"1.0\"?><Root></Root>");
    try {
      execute(service, msg);
      fail("Success on a file that isn't valid XML!");
    }
    catch (ServiceException e) {
      ;
    }
  }

  @Test
  public void testSchemaValidator_InvalidXmlSchemaInMetadata() throws Exception {
    String schemaUrl = PROPERTIES.getProperty(KEY_INVALID_SCHEMA_URL);
    // if (!ExternalResourcesHelper.isExternalServerAvailable(new URLString(schemaUrl))) {
    // log.debug(schemaUrl + " not available, ignoring testInvalidXmlSchemaInMetadata test");
    // return;
    // }

    XmlSchemaValidator validator = new XmlSchemaValidator();
    validator.setSchemaMetadataKey("schema-key");
    AdaptrisMessage msg = MessageHelper.createMessage(PROPERTIES.getProperty(KEY_INPUT_FILE));
    msg.addMetadata("schema-key", schemaUrl);
    XmlValidationService service = new XmlValidationService(validator);
    try {
      execute(service, msg);
      fail("Success with an invalid schema");
    }
    catch (ServiceException e) {
      ;
    }
  }

  @Test
  public void testSimpleValidator_IsXml() throws Exception {
    XmlValidationService service = new XmlValidationService();
    XmlBasicValidator validator = new XmlBasicValidator();
    service.setValidators(new ArrayList<MessageValidator>(Arrays.asList(validator)));
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("<?xml version=\"1.0\"?><Root>></Root>");
    execute(service, msg);
  }

  @Test
  public void testSimpleValidator_IsNotXml() throws Exception {
    XmlBasicValidator validator = new XmlBasicValidator();
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("BLABLAH");
    XmlValidationService service = new XmlValidationService(validator);
    try {
      execute(service, msg);
      fail("Success with invalid XML");
    }
    catch (ServiceException e) {
      ;
    }
  }

  @Test
  public void testSimpleValidator_IsInvalidXml_AmpersandNoEntity() throws Exception {
    XmlBasicValidator validator = new XmlBasicValidator();
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("<?xml version=\"1.0\"?><Root>&</Root>");
    XmlValidationService service = new XmlValidationService(validator);
    try {
      execute(service, msg);
      fail("Success with invalid XML");
    }
    catch (ServiceException e) {
      ;
    }
  }

  @Test
  public void testSimpleValidator_IsInvalidXml_LessThan() throws Exception {
    XmlBasicValidator validator = new XmlBasicValidator(new DocumentBuilderFactoryBuilder());
    AdaptrisMessage msg = new DefaultMessageFactory().newMessage("<?xml version=\"1.0\"?><Root><</Root>");
    XmlValidationService service = new XmlValidationService(validator);
    try {
      execute(service, msg);
      fail("Success with invalid XML");
    }
    catch (ServiceException e) {
      ;
    }
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    ValidationStage vs = new ValidationStage();
    vs.setIterationXpath("/document/names");
    vs.setElementXpath("fullName");
    vs.addRule(new NotNullContentValidation());
    KeyValuePair disableExternalEntities = new KeyValuePair("http://xml.org/sax/features/external-general-entities", "false");
    KeyValuePair disableDoctypeDecl = new KeyValuePair("http://apache.org/xml/features/disallow-doctype-decl", "true");
    return new XmlValidationService(new XmlBasicValidator(new DocumentBuilderFactoryBuilder().withNamespaceAware(true).withFeatures(
        new KeyValuePairSet(Arrays.asList(disableExternalEntities, disableDoctypeDecl)))),
        new XmlSchemaValidator("http://host/schema.xsd or %message{metadatKey}"),
        new XmlRuleValidator(vs));
  }
  
  
  @Override
  protected boolean doStateTests() {
    return false;
  }
}
