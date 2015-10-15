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

import static com.adaptris.core.util.XmlHelper.createDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceException;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.transform.validate.ContentValidation;
import com.adaptris.transform.validate.ValidationStage;
import com.adaptris.util.KeyValuePairSet;
import com.adaptris.util.text.xml.SimpleNamespaceContext;
import com.adaptris.util.text.xml.XPath;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * Used with {@link XmlValidationService} to validate an XML message against various rules.
 * 
 * @config xml-rule-validator
 * @license BASIC
 * @author lchan
 * @see ValidationStage
 */
@XStreamAlias("xml-rule-validator")
public class XmlRuleValidator extends MessageValidatorImpl {

  private static final String I_COUNT = "@I_COUNT@";
  private static final String I_XP = "@I_XP@";
  private static final String E_XP = "@E_XP@";
  private static final String CONTENTS = "@CONTENTS@";
  private static final String VALIDATION_MSG = "@VALIDATION_MSG@";

  private static final String ERR_MSG = "NodeList entry " + I_COUNT + " from [" + I_XP + "] with element retrieved by XPath ["
      + E_XP + "][" + CONTENTS + "] was invalid : " + VALIDATION_MSG;

  @XStreamImplicit(itemFieldName = "validation-stage")
  private List<ValidationStage> validationStages = new ArrayList<ValidationStage>();
  private KeyValuePairSet namespaceContext;

  public XmlRuleValidator() {
    super();
  }

  public XmlRuleValidator(ValidationStage... stages) {
    this();
    setValidationStages(new ArrayList(Arrays.asList(stages)));
  }

  @Override
  public void validate(AdaptrisMessage msg) throws CoreException {
    try {
      NamespaceContext namespaceCtx = SimpleNamespaceContext.create(getNamespaceContext(), msg);
      Document doc = createDocument(msg, namespaceCtx);
      XPath xp = new XPath(namespaceCtx);
      for (int stageIndex = 0; stageIndex < validationStages.size(); stageIndex++) {
        ValidationStage v = (ValidationStage) validationStages.get(stageIndex);
        NodeList n = xp.selectNodeList(doc, v.getIterationXpath());
        validate(n, v.getIterationXpath(), v.failOnIteratorFailure());
        for (int i = 0; i < n.getLength(); i++) {
          Node node = n.item(i);
          String contents = xp.selectSingleTextItem(node, v.getElementXpath());
          for (ContentValidation cv : v.getRules()) {
            if (!cv.isValid(contents)) {
              throw new ServiceException(ERR_MSG.replaceAll(I_COUNT, "" + i).replaceAll(I_XP, v.getIterationXpath())
                  .replaceAll(E_XP, v.getElementXpath()).replaceAll(CONTENTS, contents).replaceAll(VALIDATION_MSG, cv.getMessage()));
            }
          }
        }
      }
    }
    catch (Exception e) {
      ExceptionHelper.rethrowCoreException(e);
    }
  }

  private void validate(NodeList n, String xpath, boolean fail) throws ServiceException {
    if (n.getLength() == 0 && fail) {
      throw new ServiceException(xpath + " returned zero length nodelist");
    }
  }

  /**
   * 
   * @param vs a {@link ValidationStage} to apply.
   */
  public void addValidationStage(ValidationStage vs) {
    validationStages.add(vs);
  }

  public List<ValidationStage> getValidationStages() {
    return validationStages;
  }

  public void setValidationStages(List<ValidationStage> l) {
    validationStages = l;
  }

  public KeyValuePairSet getNamespaceContext() {
    return namespaceContext;
  }

  /**
   * Set the namespace context for resolving namespaces.
   * <ul>
   * <li>The key is the namespace prefix</li>
   * <li>The value is the namespace uri</li>
   * </ul>
   * 
   * @param set the mapping for the namespace context.
   */
  public void setNamespaceContext(KeyValuePairSet set) {
    this.namespaceContext = set;
  }
}
