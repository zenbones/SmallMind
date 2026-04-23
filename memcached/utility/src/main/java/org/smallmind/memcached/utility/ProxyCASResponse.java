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

/**
 * Pairs a decoded cached value with the CAS (compare-and-swap) token returned by the server.
 *
 * <p>Instances are returned by {@link ProxyMemcachedClient#casGet(String)} and are used by
 * callers that need to perform optimistic-lock updates via
 * {@link ProxyMemcachedClient#casSet(String, int, Object, long)} or
 * {@link ProxyMemcachedClient#casDelete(String, long)}.</p>
 *
 * @param <T> the type of the cached value
 */
public interface ProxyCASResponse<T> {

  /**
   * Returns the decoded cached value associated with the key.
   *
   * @return the cached value; may be {@code null} if the server explicitly stored a null
   */
  T getValue ();

  /**
   * Returns the CAS token that must be supplied in a subsequent conditional write to
   * verify that no concurrent modification has occurred.
   *
   * @return the server-issued compare-and-swap token
   */
  long getCas ();
}
