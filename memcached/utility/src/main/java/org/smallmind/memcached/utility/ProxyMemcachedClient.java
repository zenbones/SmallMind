/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.memcached.utility;

import java.util.Collection;
import java.util.Map;

/**
 * Minimal client abstraction used by implementations such as Cubby and in-memory clients.
 */
public interface ProxyMemcachedClient {

  /**
   * @return default request timeout in milliseconds
   */
  long getDefaultTimeout ();

  /**
   * Wraps a value and CAS token.
   *
   * @param cas   compare-and-swap token
   * @param value decoded value
   * @param <T>   value type
   * @return CAS response wrapper
   */
  <T> ProxyCASResponse<T> createCASResponse (long cas, T value);

  /**
   * Retrieves a value by key.
   */
  <T> T get (String key)
    throws Exception;

  /**
   * Retrieves multiple values by key.
   */
  <T> Map<String, T> get (Collection<String> keys)
    throws Exception;

  /**
   * Retrieves a value along with its CAS token.
   */
  <T> ProxyCASResponse<T> casGet (String key)
    throws Exception;

  /**
   * Stores a value with an expiration.
   */
  <T> boolean set (String key, int expiration, T value)
    throws Exception;

  /**
   * Stores a value conditionally using a CAS token.
   */
  <T> boolean casSet (String key, int expiration, T value, long cas)
    throws Exception;

  /**
   * Deletes the entry for the key.
   */
  boolean delete (String key)
    throws Exception;

  /**
   * Deletes the entry only if the CAS token matches.
   */
  boolean casDelete (String key, long cas)
    throws Exception;

  /**
   * Updates a key's expiration without returning the value.
   */
  boolean touch (String key, int expiration)
    throws Exception;

  /**
   * Retrieves and updates the expiration for a key.
   */
  <T> T getAndTouch (String key, int expiration)
    throws Exception;

  /**
   * Clears all entries.
   */
  void clear ()
    throws Exception;

  /**
   * Shuts down the client and releases resources.
   */
  void shutdown ()
    throws Exception;
}
