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

package com.adaptris.core.services.duplicate;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ServiceException;
import com.adaptris.core.util.Args;
import com.adaptris.core.util.ExceptionHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * <p>
 * <code>Service</code> implementation which stores the value held against a configured metadata key in a persistent list, generally
 * for use by <code>CheckMetadataValueService</code>. If the configured key returns null or empty, a <code>ServiceException</code>
 * is thrown. The underlying store may contain duplicate values.
 * </p>
 * <p>
 * The store of previous values has a configurable maximum size. After a new value is added, if the store exceeds the maximum size
 * the oldest value is removed. The store is then persisted.
 * </p>
 * <p>
 * Storage will be spun off into a separate interface and imps if required.
 * </p>
 * 
 * @config store-metadata-value-service
 */
@XStreamAlias("store-metadata-value-service")
@ComponentProfile(summary = "Store metadata values ready for checking by check-metadata-value-service",
    tag = "service,duplicate")
@DisplayOrder(order = {"metadataKey", "nextServiceIdIfUnique", "nextServiceIdIfDuplicate", "storeFileUrl"})
public class StoreMetadataValueService extends DuplicateMetadataValueService {

  @InputFieldDefault(value = "1000")
  private int numberOfPreviousValuesToStore;

  /**
   * <p>
   * Creates a new instance. Default history size is 1000.
   * </p>
   */
  public StoreMetadataValueService() {
    setNumberOfPreviousValuesToStore(1000);
  }

  /** @see com.adaptris.core.Service
   *   #doService(com.adaptris.core.AdaptrisMessage) */
  @Override
  public void doService(AdaptrisMessage msg) throws ServiceException {
    try {
      String value = msg.getMetadataValue(getMetadataKey());
      Args.notBlank(value, "metadataKey");
      previousValuesStore.add(value);
      while (previousValuesStore.size() > getNumberOfPreviousValuesToStore()) {
        previousValuesStore.remove(0);
      }
      storePreviouslyReceivedValues();
    }
    catch (Exception e) {
      throw ExceptionHelper.wrapServiceException(e);
    }
  }

  private void storePreviouslyReceivedValues() throws Exception {
    try (FileOutputStream out = new FileOutputStream(store); ObjectOutputStream o = new ObjectOutputStream(out)) {
      o.writeObject(previousValuesStore);
    }
  }

  // properties...

  /**
   * <p>
   * Returns the number of previous values to keep.
   * </p>
   * @return the number of previous values to keep
   */
  public int getNumberOfPreviousValuesToStore() {
    return numberOfPreviousValuesToStore;
  }

  /**
   * <p>
   * Sets the number of previous values to keep. Must be greater than 0.
   * </p>
   * @param i the number of previous values to keep
   */
  public void setNumberOfPreviousValuesToStore(int i) {
    if (i < 1) {
      throw new IllegalArgumentException("history size is 0 or negative");
    }
    numberOfPreviousValuesToStore = i;
  }
}
