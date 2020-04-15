package com.adaptris.core.services.cache;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ServiceException;
import com.adaptris.core.UnresolvedMetadataException;
import com.adaptris.core.cache.Cache;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.util.TimeInterval;
import com.adaptris.util.text.DateFormatUtil;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Version of {@link AddToCacheService} that doesn't use {@link CacheEntryEvaluators}.
 * 
 * <p>
 * Most of the time, you only want to add a single item to the cache rather than a list of items;
 * this simplified service allows you to do just that. It does not have a list of entries that are
 * evaluated, you simply specify a key and a {@link CacheValueTranslator} which is used to extract
 * the value for storing in the cache; no checking is done of the resulting serializable-ness (or
 * not) of the value, it is simply inserted into the cache.
 * </p>
 * 
 * @config add-single-value-to-cache
 */
@XStreamAlias("add-single-value-to-cache")
@ComponentProfile(summary = "Add a single key/value to the configured cache cache", since = "3.9.2", tag = "service,cache",
    recommended = {CacheConnection.class})
@DisplayOrder(order = {"connection", "key", "valueTranslator", "expiry"})
public class AddValueToCache extends SingleKeyValueCacheImpl {

  private static final List<ExpiryParser> EXPIRY_PARSERS =
      Collections.unmodifiableList(
          Arrays.asList(new ExpiryParser[] {
          (val) -> build(val),
          (val) -> build(DateFormatUtil.parse(val, null))
      }));

  @InputFieldHint(expression = true)
  private String expiry;

  @Override
  public void doService(AdaptrisMessage msg) throws ServiceException {
    try {
      Cache cache = retrieveCache();
      Optional<ExpiryWrapper> hasExpiry = buildExpiry(msg);
      String cacheKey = msg.resolve(getKey());
      // should be hasExpiry.ifPresentOrElse() once we goto J11...
      if (hasExpiry.isPresent()) {
        TimeInterval expiryInterval = hasExpiry.get().asInterval();
        log.trace("[{}] will expire in {}", cacheKey, expiryInterval);
        cache.put(cacheKey, getValueTranslator().getValueFromMessage(msg), hasExpiry.get().asInterval());
      } else {
        log.trace("Expiry for [{}] taken from cache settings", cacheKey);
        cache.put(cacheKey, getValueTranslator().getValueFromMessage(msg));
      }
    } catch (Exception e) {
      throw ExceptionHelper.wrapServiceException(e);
    }
  }

  private Optional<ExpiryWrapper> buildExpiry(AdaptrisMessage msg) {
    try {
      final String value = msg.resolve(getExpiry());
      return
          EXPIRY_PARSERS.stream().map((p) -> p.parse(value)).filter((o) -> o.isPresent()).findFirst().orElse(Optional.empty());
    } catch (UnresolvedMetadataException e) {
      return Optional.empty();
    }
  }

  public <T extends AddValueToCache> T withExpiry(String s) {
    setExpiry(s);
    return (T) this;
  }

  public String getExpiry() {
    return expiry;
  }

  private static Optional<ExpiryWrapper> build(final Date date) {
    return Optional.ofNullable(date).map(d -> Optional.of(new ExpiryWrapper(d))).orElse(Optional.empty());
  }

  private static Optional<ExpiryWrapper> build(final String millis) {
    // don't use NumberUtils since we don't want to support a - number...
    return Optional.ofNullable(millis).filter(t -> t.chars().allMatch(c -> c >= '0' && c <= '9'))
        .map(t -> Optional.of(new ExpiryWrapper(Long.parseLong(t)))).orElse(Optional.empty());
  }

  /**
   * Set the cache expiry for any value added to the cache (if any).
   * 
   * <p>
   * The rules once the value has been resolved are :
   * </p>
   * <ul>
   * <li>If the value is numeric and less than {@link System#currentTimeMillis()} then it is treated as relative to {@code now()};
   * expiry is in milliseconds</li>
   * <li>If the value is numberic and greater than {@link System#currentTimeMillis()} then it is treated as absolute; expiry is in
   * milliseconds</li>
   * <li>If the value is a recognised value from {@link DateFormatUtil#parse(String)} then it is treated as absolute</li>
   * <li>If it can't be resolved by any of those means, then it is ignored (i.e. treated as though there is no expiry).
   * </ul>
   * 
   * @param s the expiry, which supports the {@code %message{}} syntax to resolve metadata.
   */
  public void setExpiry(String s) {
    this.expiry = s;
  }

  // Can't do an array of generic functions so new Function<String,Optional<ExpiryWrapper>>[] isn't loved by the
  // compiler. This is basically a Function though.
  @FunctionalInterface
  private static interface ExpiryParser {
    Optional<ExpiryWrapper> parse(String s);
  }

  private static class ExpiryWrapper {
    private transient TimeInterval expiry;

    public ExpiryWrapper(long absoluteOrRelativeMs) {
      long now = System.currentTimeMillis();
      if (absoluteOrRelativeMs < now) {
        expiry = new TimeInterval(absoluteOrRelativeMs, TimeUnit.MILLISECONDS);
      } else {
        expiry = new TimeInterval(absoluteOrRelativeMs - now, TimeUnit.MILLISECONDS);
      }
    }

    public ExpiryWrapper(Date futureDate) {
      Date now = new Date();
      expiry = new TimeInterval(futureDate.getTime() - now.getTime(), TimeUnit.MILLISECONDS);
    }

    public TimeInterval asInterval() {
      return expiry;
    }


  }
}
