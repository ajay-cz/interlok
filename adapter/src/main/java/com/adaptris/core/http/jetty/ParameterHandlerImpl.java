package com.adaptris.core.http.jetty;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.http.server.ParameterHandler;

/**
 * Abstract {@link ParameterHandler} implementation that provides a prefix.
 * 
 * @author lchan
 *
 */
public abstract class ParameterHandlerImpl implements ParameterHandler<HttpServletRequest> {
  protected transient Logger log = LoggerFactory.getLogger(this.getClass());

  private String parameterPrefix;

  public ParameterHandlerImpl() {

  }

  public String getParameterPrefix() {
    return parameterPrefix;
  }

  public void setParameterPrefix(String headerPrefix) {
    this.parameterPrefix = headerPrefix;
  }

  /**
   * Return the parameter prefix with null protection.
   * 
   * @return the prefix
   */
  protected String parameterPrefix() {
    return defaultIfEmpty(getParameterPrefix(), "");
  }
}