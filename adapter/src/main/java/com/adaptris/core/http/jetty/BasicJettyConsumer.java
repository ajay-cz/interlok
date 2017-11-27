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

package com.adaptris.core.http.jetty;

import static com.adaptris.core.CoreConstants.HTTP_METHOD;
import static com.adaptris.core.CoreConstants.JETTY_QUERY_STRING;
import static com.adaptris.core.CoreConstants.JETTY_URI;
import static com.adaptris.core.CoreConstants.JETTY_URL;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageConsumerImp;
import com.adaptris.core.ClosedState;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.CoreException;
import com.adaptris.core.WorkflowImp;
import com.adaptris.core.WorkflowInterceptor;
import com.adaptris.core.http.client.RequestMethodProvider.RequestMethod;
import com.adaptris.util.TimeInterval;

/**
 * This is the abstract class for all implementations that make use of Jetty to receive messages.
 * 
 * 
 * @author lchan
 * @author $Author: lchan $
 */
public abstract class BasicJettyConsumer extends AdaptrisMessageConsumerImp {
  private static final long DEFAULT_EXPECT_INTERVAL = TimeUnit.SECONDS.toMillis(20);

  private static final TimeInterval DEFAULT_INTERMEDIATE_WAIT_TIME = new TimeInterval(1L, TimeUnit.SECONDS);
  private static final String COMMA = ",";
  private static final List<String> HTTP_METHODS;
  private static final String EXPECT_102_PROCESSING = "102-Processing";
  private static final String HEADER_EXPECT = "Expect";

  private transient Servlet jettyServlet;
  private transient ServletWrapper servletWrapper = null;
  private transient List<String> acceptedMethods = new ArrayList<>(HTTP_METHODS);

  @InputFieldDefault(value = "false")
  @AdvancedConfig
  private Boolean additionalDebug;

  @AdvancedConfig
  @Deprecated
  private TimeInterval maxWaitTime;

  @AdvancedConfig
  @InputFieldDefault(value = "return 202")
  private TimeoutAction timeoutAction;

  @AdvancedConfig
  @InputFieldDefault(value = "Never")
  private TimeInterval warnAfter;
  @AdvancedConfig
  @InputFieldDefault(value = "20 Seconds")
  private TimeInterval sendProcessingInterval;

  @Deprecated
  @AdvancedConfig
  private Long warnAfterMessageHangMillis = null;

  static {
    List<String> methods = new ArrayList<>();
    for (RequestMethod m : RequestMethod.values()) {
      methods.add(m.name());
    }
    HTTP_METHODS = Collections.unmodifiableList(methods);
  }

  public BasicJettyConsumer() {
    super();
    jettyServlet = new BasicServlet();
    changeState(ClosedState.getInstance());
  }

  /**
   * Create an AdaptrisMessage from the incoming servlet request and response.
   * 
   * @param request the HttpServletRequest
   * @param response the HttpServletResponse
   * @return an AdaptrisMessage instance.
   * @throws IOException
   * @throws ServletException
   * @see HttpServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
   */
  public abstract AdaptrisMessage createMessage(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException;

  private boolean submitToWorkflow(AdaptrisMessage msg) {
    boolean waitForCompletion = false;
    if (retrieveAdaptrisMessageListener() instanceof WorkflowImp) {
      List<WorkflowInterceptor> interceptors = ((WorkflowImp) retrieveAdaptrisMessageListener()).getInterceptors();
      for (WorkflowInterceptor i : interceptors) {
        if (i.getClass().equals(JettyPoolingWorkflowInterceptor.class)) {
          waitForCompletion = true;
          break;
        }
      }
    }
    retrieveAdaptrisMessageListener().onAdaptrisMessage(msg);
    return waitForCompletion;
  }

  protected void logHeaders(HttpServletRequest req) {
    if (additionalDebug()) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintWriter p = new PrintWriter(out);
      p.println("Received HTTP Headers");
      p.println("URL " + req.getRequestURL());
      p.println("URI " + req.getRequestURI());
      p.println("Query " + req.getQueryString());
      for (Enumeration e = req.getHeaderNames(); e.hasMoreElements();) {
        String key = (String) e.nextElement();
        Enumeration values = req.getHeaders(key);
        StringBuffer sb = new StringBuffer();
        while (values.hasMoreElements()) {
          sb.append(values.nextElement()).append(",");
        }
        String s = sb.toString();
        p.println(key + ": " + s.substring(0, s.lastIndexOf(",")));
      }
      p.close();
      log.trace(out.toString());
    }
  }

  protected ServletWrapper asServletWrapper() throws CoreException {
    if (servletWrapper == null) {
      String destination = ensureIsPath(getDestination().getDestination());
      servletWrapper = new ServletWrapper(jettyServlet, destination);
    }
    return servletWrapper;
  }

  // Ensure that if someone types in http://localhost:8080/fred/blah/blah then
  // we make sure that we return /fred/blah/blah
  protected String ensureIsPath(String s) throws CoreException {
    String result = s;
    try {
      URI uri = new URI(s);
      result = uri.getPath();
    }
    catch (URISyntaxException e) {
      throw new CoreException(e);
    }
    return result;
  }

  /**
   * 
   * @see com.adaptris.core.AdaptrisComponent#init()
   */
  public void init() throws CoreException {
    if (!isEmpty(getDestination().getFilterExpression())) {
      acceptedMethods = Arrays.asList(getDestination().getFilterExpression().split(","));
      log.trace("Acceptable HTTP methods set to : {}", acceptedMethods);
    }
  }

  /**
   * 
   * @see com.adaptris.core.AdaptrisComponent#start()
   */
  public void start() throws CoreException {
    retrieveConnection(JettyServletRegistrar.class).addServlet(asServletWrapper());
  }

  /**
   * 
   * @see com.adaptris.core.AdaptrisComponent#stop()
   */
  public void stop() {
    String loggingUrl = "";
    try {
      loggingUrl = asServletWrapper().getUrl();
      retrieveConnection(JettyServletRegistrar.class).removeServlet(asServletWrapper());
    }
    catch (CoreException e) {
      log.warn("Could not remove servlet from jetty engine; Path=[{}]", loggingUrl, e);
    }
  }

  /**
   * 
   * @see com.adaptris.core.AdaptrisComponent#close()
   */
  public void close() {
  }

  /**
   * @return the maxWaitTime
   * @deprecated since 3.6.6 use {@link #getTimeoutAction()} instead.
   */
  @Deprecated
  public TimeInterval getMaxWaitTime() {
    return maxWaitTime;
  }

  /**
   * Set the max wait time for an individual worker in a workflow to finish.
   * <p>
   * This setting only has an impact if the consumer is the entry point for a {@link com.adaptris.core.PoolingWorkflow} instance. In
   * the event that the wait time is exceeded, then the internal {@link javax.servlet.http.HttpServlet} instance commits the
   * response in its current state and returns control back to the Jetty engine.
   * </p>
   * 
   * @param maxWait the maxWaitTime to set (default 10 minutes)
   * @deprecated since 3.6.6 use {@link #setTimeoutAction(TimeoutAction)} instead.
   */
  @Deprecated
  public void setMaxWaitTime(TimeInterval maxWait) {
    maxWaitTime = maxWait;
  }

  TimeoutAction timeoutAction() {
    if (getMaxWaitTime() != null) {
      log.warn("max-wait-time is deprecated); use timeout-action instead");
      return new TimeoutAction(getMaxWaitTime());
    }
    return getTimeoutAction() != null ? getTimeoutAction() : new TimeoutAction();
  }

  public TimeoutAction getTimeoutAction() {
    return timeoutAction;
  }

  /**
   * Set the behaviour that should occur when the workflow takes too long to finish.
   * <p>
   * This setting only has an impact if the consumer is the entry point for a {@link com.adaptris.core.PoolingWorkflow} instance. In
   * the event that the wait time is exceeded, then the behaviour specified here is done.
   * </p>
   * 
   * @param action
   */
  public void setTimeoutAction(TimeoutAction action) {
    this.timeoutAction = action;
  }

  public Boolean getAdditionalDebug() {
    return additionalDebug;
  }

  public void setAdditionalDebug(Boolean additionalDebug) {
    this.additionalDebug = additionalDebug;
  }

  boolean additionalDebug() {
    return getAdditionalDebug() != null ? getAdditionalDebug().booleanValue() : false;
  }

  /**
   * @deprecated since 3.5.1 use {@link #getWarnAfter()} instead.
   */
  @Deprecated
  public Long getWarnAfterMessageHangMillis() {
    return warnAfterMessageHangMillis;
  }

  /**
   * @deprecated since 3.5.1 use {@link #setWarnAfter(TimeInterval)} instead.
   */
  @Deprecated
  public void setWarnAfterMessageHangMillis(Long w) {
    this.warnAfterMessageHangMillis = w;
  }

  /**
   * @return the warnAfter
   */
  public TimeInterval getWarnAfter() {
    return warnAfter;
  }

  /**
   * Log a warning after this interval.
   * 
   * @param t the warnAfter to set, default is to never warn.
   */
  public void setWarnAfter(TimeInterval t) {
    this.warnAfter = t;
  }

  long warnAfter() {
    long result = Long.MAX_VALUE;
    if (getWarnAfterMessageHangMillis() != null) {
      log.warn("Use of deprecated warn-after-message-hand-millis; use warn-after instead");
      result = getWarnAfterMessageHangMillis().longValue();
    } else {
      result = getWarnAfter() != null ? getWarnAfter().toMilliseconds() : Long.MAX_VALUE;
    }
    return result;
  }

  /**
   * @return the sendExpectEvery
   */
  public TimeInterval getSendProcessingInterval() {
    return sendProcessingInterval;
  }

  /**
   * If required send a 102 upon this interval.
   * 
   * @param t the sendExpectEvery to set, default is 20 seconds.
   */
  public void setSendProcessingInterval(TimeInterval t) {
    this.sendProcessingInterval = t;
  }

  long sendProcessingInterval() {
    return getSendProcessingInterval() != null ? getSendProcessingInterval().toMilliseconds() : DEFAULT_EXPECT_INTERVAL;
  }

  protected class BasicServlet extends HttpServlet {

    private static final long serialVersionUID = 2007082301L;
    private transient Map<String, HttpOperation> httpHandlers = null;
    private transient Timer processingTimer;

    protected BasicServlet() {
    }

    @Override
    public void destroy() {
      if (processingTimer != null) {
        processingTimer.cancel();
        processingTimer = null;
      }
      super.destroy();
    }

    @Override
    public void init() throws ServletException {
      super.init();
      processingTimer = new Timer(true);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
      String oldName = renameThread();
      try {
        String method = request.getMethod().toUpperCase();
        if (handlers().containsKey(method)) {
          handlers().get(method).handle(request, response);
        }
        else {
          addAllow(response).sendError(HttpURLConnection.HTTP_BAD_METHOD, "Method Not Allowed");
        }
      }
      finally {
        Thread.currentThread().setName(oldName);
      }
    }

    protected Map<String, HttpOperation> handlers() {
      if (httpHandlers == null) {
        httpHandlers = new HashMap<String, HttpOperation>();
        HttpOperation defaultHandler = new HttpOperation() {
          public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            processRequest(request, response);
          }
        };
        for (String m : acceptedMethods) {
          httpHandlers.put(m.toUpperCase().trim(), defaultHandler);
        }
        httpHandlers.put(RequestMethod.OPTIONS.name(), new HttpOperation() {
          public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            addAllow(response).setStatus(HttpURLConnection.HTTP_OK);
          }
        });
      }
      return httpHandlers;
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
      AdaptrisMessage msg = createMessage(request, response);
      ProcessingTimerTask task = null;
      // If we have a Expect: 102-Processing head, then let's fork a little timer thread to write one
      //
      if (hasExpect102(request)) {
        task = schedule(new ProcessingTimerTask(response));
      }
      msg.addMetadata(JETTY_URL, request.getRequestURL().toString());
      msg.addMetadata(JETTY_URI, request.getRequestURI());
      msg.addMetadata(HTTP_METHOD, request.getMethod());
      if (!isEmpty(request.getQueryString())) {
        msg.addMetadata(JETTY_QUERY_STRING, request.getQueryString());
      }
      msg.addObjectHeader(CoreConstants.JETTY_RESPONSE_KEY, response);
      msg.addObjectHeader(CoreConstants.JETTY_REQUEST_KEY, request);
      JettyConsumerMonitor monitor = new JettyConsumerMonitor();
      monitor.setStartTime(System.currentTimeMillis());
      msg.addObjectHeader(JettyPoolingWorkflowInterceptor.MESSAGE_MONITOR, monitor);
      waitFor(submitToWorkflow(msg), monitor, response, msg.getUniqueId());
      cancel(task);
    }

    private void waitFor(boolean waitFor, JettyConsumerMonitor monitor, HttpServletResponse response, String loggingId)
        throws IOException, ServletException {
      if (waitFor) {
        TimeoutAction timeout = timeoutAction();
        try {
          synchronized (monitor) {
            while (!monitor.isMessageComplete()) {
              timeout.checkTimeout(monitor);
              monitor.wait(DEFAULT_INTERMEDIATE_WAIT_TIME.toMilliseconds());
            }
          }
        }
        catch (InterruptedException e) {
        }
        catch (TimeoutException e) {
          timeout.handleTimeout(response);
        }
        if ((monitor.getEndTime() - monitor.getStartTime()) > warnAfter()) {
          log.warn("Message ({}) took longer than expected; {}ms", loggingId,
              ((monitor.getEndTime() - monitor.getStartTime())));
        }
      }
    }

    private void cancel(TimerTask task) {
      if (task != null) {
        task.cancel();
      }
    }

    private ProcessingTimerTask schedule(ProcessingTimerTask task) {
      // Every 20 seconds as per RFC2518
      long interval = sendProcessingInterval();
      log.trace("Scheduling a 102 Processing Response every {}ms", interval);
      processingTimer.schedule(task, interval, interval);
      return task;
    }

    private boolean hasExpect102(HttpServletRequest request) {
      String expectHeader = null;
      for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
        String hdr = e.nextElement();
        if (StringUtils.equalsIgnoreCase(HEADER_EXPECT, hdr)) {
          expectHeader = request.getHeader(hdr);
          break;
        }
      }
      return StringUtils.containsIgnoreCase(expectHeader, EXPECT_102_PROCESSING);
    }

    private HttpServletResponse addAllow(HttpServletResponse response) throws IOException, ServletException {
      response.addHeader("Allow", join(handlers().keySet(), COMMA));
      return response;
    }
  }

  public interface HttpOperation {
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;
  }

  class ProcessingTimerTask extends TimerTask {
    private transient HttpServletResponse myResponse;

    public ProcessingTimerTask(HttpServletResponse response) {
      myResponse = response;
    }

    @Override
    public void run() {
      try {
        if (!myResponse.isCommitted()) {
          myResponse.sendError(102);
        }
      }
      catch (Exception e) {
        // In the event of an exception, cancel ourselves.
        this.cancel();
      }
    }
  }


}
