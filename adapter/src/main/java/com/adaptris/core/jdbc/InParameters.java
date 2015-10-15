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

package com.adaptris.core.jdbc;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * This class contains the IN parameters that a stored procedure will require to be executed.
 * 
 * @config jdbc-in-parameters
 * @author Aaron McGrath
 * 
 */
@XStreamAlias("jdbc-in-parameters")
public class InParameters extends JdbcParameterList<InParameter> {

  public InParameters() {
    parameters = new ArrayList<InParameter>();
  }
  
  @XStreamImplicit
  private List<InParameter> parameters;
  
  @Override
  public List<InParameter> getParameters() {
    return parameters;
  }

}
