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

package com.adaptris.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.core.util.LoggingHelper;
import com.adaptris.core.util.ManagedThreadFactory;

/**
 * <p>
 * Implementation of behaviour common to {@link ProduceExceptionHandler} instances
 * </p>
 */
public abstract class ProduceExceptionHandlerImp implements ProduceExceptionHandler {

  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());

  protected void restart(final StateManagedComponent s) {
    // spin off exception handler Thread
    Thread t = new ManagedThreadFactory().newThread(new Runnable() {
      @Override
      public void run() {
        try {
          LifecycleHelper.stop(s);
          LifecycleHelper.close(s);
          LifecycleHelper.init(s);
          LifecycleHelper.start(s);
        }
        catch (Exception e) {
          log.error("Failed to restart " + LoggingHelper.friendlyName(s));
        }
      }
    });
    t.setName("Restart " + Thread.currentThread().getName());
    log.trace("Handling Produce Exception");
    t.start();
  }
}
