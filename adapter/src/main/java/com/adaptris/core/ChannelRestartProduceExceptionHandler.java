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

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Implementation of {@link ProduceExceptionHandler} which attempts to restart the parent {@link Channel} of the {@code Workflow}
 * that had the failure.
 * 
 * 
 * @config channel-restart-produce-exception-handler
 */
@XStreamAlias("channel-restart-produce-exception-handler")
public class ChannelRestartProduceExceptionHandler extends ProduceExceptionHandlerImp {

  /**
   * @see com.adaptris.core.ProduceExceptionHandler
   *      #handle(com.adaptris.core.Workflow)
   */
  public void handle(Workflow workflow) {

    // obtain Channel lock while still holding W/f lock in onAM...
    // LewinChan - This appears to be dodgy - See Bug:870
    // So we synchronize after checking the channel availability.
    // synchronized (workflow.obtainChannel()) {
    if (workflow.obtainChannel().isAvailable()) {
      synchronized (workflow.obtainChannel()) {
        workflow.obtainChannel().toggleAvailability(false);
        super.restart(workflow.obtainChannel());
      }
    }
    else { // sthg else is rebooting the Channel...
      // do nothing?
      log.debug("Channel is not available, returning...");

    }
  }
}
