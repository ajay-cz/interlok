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

package com.adaptris.interlok.config;

/**
 * <p>
 * DataDestinations are a generic configuration item, that allow you to specify where data from needed by a service
 * will come from or be saved to.
 * </p>
 * <p>
 * Please check the service documentation to make sure that service supports DataDestinations.
 * </p>
 * @author Aaron
 *
 */
public interface DataDestination<S, T> extends DataInputParameter<S>, DataOutputParameter<T> {
  
}
