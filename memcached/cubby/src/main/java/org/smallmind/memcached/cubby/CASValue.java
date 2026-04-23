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
package org.smallmind.memcached.cubby;

import java.io.IOException;
import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.command.Result;
import org.smallmind.memcached.utility.ProxyCASResponse;

/**
 * An immutable holder that pairs a deserialized cache value with its memcached CAS token.
 *
 * <p>{@code CASValue} is returned by {@link CubbyMemcachedClient#casGet casGet} and
 * {@link CubbyMemcachedClient#casGetAndTouch casGetAndTouch}. The embedded CAS token must be
 * supplied to a subsequent {@link CubbyMemcachedClient#casSet casSet} or
 * {@link CubbyMemcachedClient#casDelete casDelete} call to perform an optimistic-locking
 * update. If the server value has been modified since the token was issued the operation
 * will fail.</p>
 *
 * @param <T> the type of the cached value
 */
public class CASValue<T> implements ProxyCASResponse<T> {

  private final T value;
  private final long cas;

  /**
   * Constructs a {@code CASValue} by deserializing the payload contained in a raw command result.
   *
   * @param result the command result carrying the serialized bytes and the CAS token
   * @param codec  the codec used to deserialize the payload into the target type
   * @throws IOException            if an I/O error occurs during deserialization
   * @throws ClassNotFoundException if the class of the deserialized object cannot be found
   */
  public CASValue (Result result, CubbyCodec codec)
    throws IOException, ClassNotFoundException {

    this(result.getCas(), (T)codec.deserialize(result.getValue()));
  }

  /**
   * Constructs a {@code CASValue} from an already-decoded value and its associated CAS token.
   *
   * @param cas   the compare-and-swap token returned by the memcached server
   * @param value the decoded cache value
   */
  public CASValue (long cas, T value) {

    this.cas = cas;
    this.value = value;
  }

  /**
   * Returns the CAS token associated with this value.
   *
   * @return the compare-and-swap token, as assigned by the memcached server
   */
  @Override
  public long getCas () {

    return cas;
  }

  /**
   * Returns the deserialized cache value.
   *
   * @return the cached value; may be {@code null} if the server stored a null
   */
  @Override
  public T getValue () {

    return value;
  }
}
