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

import java.util.List;

/**
 * An ordered path from the root to the bucket selected for an input.
 * Segments must have stable equality and hash codes. An empty path selects the root.
 *
 * @param <S> the type of the path segments
 * @param <B> the concrete key type, returned when deriving ancestor paths
 */
public abstract class BucketKey<S, B extends BucketKey<S, B>> {

  private final List<S> segments;

  public BucketKey (List<S> segments) {

    this.segments = List.copyOf(segments);
  }

  /**
   * Returns the key for the leading {@code length} segments of this path, carrying any additional
   * state of the concrete type. {@link TokenBucket} calls this to name each prefix of a selected key
   * for the shared policy factory, with {@code length} from 1 through {@code segments().size()}. The
   * empty root path is supplied by the bucket itself and is never requested here.
   *
   * @param length the number of leading segments to retain
   * @return the key for that prefix
   */
  public abstract B ancestor (int length);

  /**
   * Returns the immutable ordered segments of this path.
   */
  public List<S> segments () {

    return segments;
  }

  /**
   * Keys are equal when their segments are equal. Subclasses carrying additional state should override
   * this method and {@link #hashCode()} consistently.
   */
  @Override
  public boolean equals (Object obj) {

    return (this == obj) || ((obj instanceof BucketKey<?, ?> other) && segments.equals(other.segments()));
  }

  @Override
  public int hashCode () {

    return segments.hashCode();
  }

  @Override
  public String toString () {

    return "BucketKey[segments=" + segments + "]";
  }
}
