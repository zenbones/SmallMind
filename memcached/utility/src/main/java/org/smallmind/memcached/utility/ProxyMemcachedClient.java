/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public interface ProxyMemcachedClient {

  long getDefaultTimeout ();

  <T> ProxyCASResponse<T> createCASResponse (long cas, T value);

  <T> T get (String key)
    throws Exception;

  <T> Map<String, T> get (Collection<String> keys)
    throws Exception;

  <T> ProxyCASResponse<T> casGet (String key)
    throws Exception;

  <T> boolean set (String key, int expiration, T value)
    throws Exception;

  <T> boolean casSet (String key, int expiration, T value, long cas)
    throws Exception;

  boolean delete (String key)
    throws Exception;

  boolean casDelete (String key, long cas)
    throws Exception;

  boolean touch (String key, int expiration)
    throws Exception;

  <T> T getAndTouch (String key, int expiration)
    throws Exception;

  void clear ()
    throws Exception;

  void shutdown ()
    throws Exception;
}
