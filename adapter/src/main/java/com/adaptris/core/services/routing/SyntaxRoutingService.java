package com.adaptris.core.services.routing;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceImp;
import com.adaptris.util.license.License;
import com.adaptris.util.license.License.LicenseType;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * Extracts data from an AdaptrisMessage and stores it against metadata.
 * <p>
 * This is somewhat similar to the available MetadataService, however it uses a list of <code>SyntaxIdentifiers</code> in order to
 * determine the value that should be stored against a particular metadata key.
 * </p>
 * <p>
 * Each <code>SyntaxIdentifier</code> is tried in turn, until <b>true</b> is returned by the method
 * <code>isThisSyntax(AdaptrisMessage)</code>. At this point, the value returned by <code>getDestination()</code> is stored against
 * the configured key.
 * </p>
 * 
 * @config syntax-routing-service
 * @license STANDARD
 * @author sellidge
 */
@XStreamAlias("syntax-routing-service")
public class SyntaxRoutingService extends ServiceImp {
  private String routingKey = null;
  @XStreamImplicit
  private List<SyntaxIdentifier> syntaxIdentifiers = new ArrayList<SyntaxIdentifier>();

  public SyntaxRoutingService() {

  }
  /**
   * @see com.adaptris.core.Service#doService(AdaptrisMessage)
   */
  public void doService(AdaptrisMessage msg) throws ServiceException {
    String message = msg.getStringPayload();
    String destination = null;

    for (int i = 0; i < syntaxIdentifiers.size(); i++) {
      SyntaxIdentifier ident = syntaxIdentifiers.get(i);

      if (ident.isThisSyntax(message)) {
        destination = ident.getDestination();
        break;
      }
    }

    if (destination == null) {
      throw new ServiceException("Unable to identify the message syntax for routing");
    }

    msg.addMetadata(routingKey, destination);
    return;
  }

  /**
   * Add a SyntaxIdentifier to the configured list.
   *
   * @param ident the SyntaxIdentifier.
   */
  public void addSyntaxIdentifier(SyntaxIdentifier ident) {
    if (ident == null) {
      throw new IllegalArgumentException("Identifier is null");
    }

    syntaxIdentifiers.add(ident);
  }

  /**
   * Return the list of configured SyntaxIdentifers.
   *
   * @return the list.
   */
  public List<SyntaxIdentifier> getSyntaxIdentifiers() {
    return syntaxIdentifiers;
  }

  /**
   * Sets the list of configured SyntaxIdentifers.
   *
   * @param l the list.
   */
  public void setSyntaxIdentifiers(List<SyntaxIdentifier> l) {
    if (l == null) {
      throw new IllegalArgumentException("List is null");
    }
    syntaxIdentifiers = l;
  }

  /**
   * Set the metadata key that the value will be stored against.
   *
   * @param key the key.
   */
  public void setRoutingKey(String key) {
    if (isBlank(key)) {
      throw new IllegalArgumentException("Null routing Key");
    }
    routingKey = key;
  }

  /**
   * Get the metadata key that the value will be stored against.
   *
   * @return the key.
   */
  public String getRoutingKey() {
    return routingKey;
  }

  /** @see com.adaptris.core.AdaptrisComponent#init() */
  public void init() throws CoreException {
    if (isBlank(routingKey)) {
      throw new CoreException("No Routing Key defined");
    }
  }

  /** @see com.adaptris.core.AdaptrisComponent#close() */
  public void close() {
    // do nothing
  }

  @Override
  public boolean isEnabled(License license) throws CoreException {
    return license.isEnabled(LicenseType.Standard);
  }

}
