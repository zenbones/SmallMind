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
package org.smallmind.quorum.bucket;

/**
 * Supplier of {@link TokenBucket} instances used when lazily creating child buckets.
 * <p>
 * Implementations configure and return a fully initialised bucket ready for use.
 * A factory is passed to {@link TokenBucket#add(BucketKey, BucketFactory)} so that
 * the parent bucket can defer construction of child entries until they are first needed.
 *
 * @param <T> the type of value governed by the produced buckets
 */
public interface BucketFactory<T> {

  /**
   * Constructs and returns a new, fully configured {@link TokenBucket}.
   *
   * @return a ready-to-use token bucket
   */
  TokenBucket<T> create ();
}
