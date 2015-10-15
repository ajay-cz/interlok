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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.core.lms.FileBackedMessage;
import com.adaptris.util.stream.StreamUtil;

/**
 * Abstract Request Reply enabled producer that may be extended by concrete
 * sub-classes.
 *
 * @author lchan
 * @author $Author: lchan $
 */
public abstract class RequestReplyProducerImp extends AdaptrisMessageProducerImp {

  @AdvancedConfig
  private Boolean ignoreReplyMetadata;

  public RequestReplyProducerImp() {
  }

  /**
   * @see AdaptrisMessageProducerImp #produce(AdaptrisMessage)
   */
  @Override
  public final void produce(AdaptrisMessage msg) throws ProduceException {
    produce(msg, getDestination());
  }

  /**
   * @see AdaptrisMessageProducerImp#request(AdaptrisMessage)
   */
  @Override
  public final AdaptrisMessage request(AdaptrisMessage msg) throws ProduceException {
    return request(msg, getDestination(), defaultTimeout());
  }

  /**
   * @see AdaptrisMessageProducerImp#request(AdaptrisMessage, long)
   */
  @Override
  public final AdaptrisMessage request(AdaptrisMessage msg, long timeout) throws ProduceException {
    return request(msg, getDestination(), timeout);
  }

  /**
   * @see AdaptrisMessageProducerImp
   *      #request(AdaptrisMessage,ProduceDestination)
   */
  @Override
  public final AdaptrisMessage request(AdaptrisMessage msg, ProduceDestination destination) throws ProduceException {
    return request(msg, destination, defaultTimeout());
  }

  /**
   * @see AdaptrisMessageProducerImp #request(AdaptrisMessage,
   *      ProduceDestination, long)
   */
  @Override
  public final AdaptrisMessage request(AdaptrisMessage msg, ProduceDestination destination, long timeout) throws ProduceException {
    AdaptrisMessage reply = doRequest(msg, destination, timeout);
    if (reply == msg) {
      return msg;
    }
    try {
      if (reply instanceof FileBackedMessage && msg instanceof FileBackedMessage) {
        ((FileBackedMessage) msg).initialiseFrom(((FileBackedMessage) reply).currentSource());
      }
      else {
        InputStream in = null;
        OutputStream out = null;
        try {
          in = reply.getInputStream();
          out = msg.getOutputStream();
          StreamUtil.copyStream(in, out);
        }
        finally {
          IOUtils.closeQuietly(in);
          IOUtils.closeQuietly(out);
        }

      }
    }
    catch (IOException e) {
      throw new ProduceException(e);
    }
    if (!shouldIgnoreReplyMetadata()) {
      for (Iterator i = reply.getMetadata().iterator(); i.hasNext();) {
        MetadataElement e = (MetadataElement) i.next();
        msg.addMetadata(e);
      }
      msg.getObjectMetadata().putAll(reply.getObjectMetadata());
    }
    if (reply.getContentEncoding() != null) {
      msg.setContentEncoding(reply.getContentEncoding());
    }
    return msg;
  }

  /**
   * The default timeout for request messages when not supplied.
   *
   * @return the default timeout.
   */
  protected abstract long defaultTimeout();

  /**
   * Actually do the request.
   *
   * @see AdaptrisMessageProducerImp #request(AdaptrisMessage,
   *      ProduceDestination, long)
   */
  protected abstract AdaptrisMessage doRequest(AdaptrisMessage msg, ProduceDestination dest, long timeout) throws ProduceException;

  public Boolean getIgnoreReplyMetadata() {
    return ignoreReplyMetadata;
  }

  /**
   * Specify whether to ignore metadata from the reply.
   *
   * @param b
   */
  public void setIgnoreReplyMetadata(Boolean b) {
    ignoreReplyMetadata = b;
  }

  public boolean shouldIgnoreReplyMetadata() {
    return ignoreReplyMetadata != null ? ignoreReplyMetadata.booleanValue() : false;
  }
}
