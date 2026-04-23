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
 * Strategy that translates an input value into the number of tokens it costs.
 * <p>
 * Implementations allow the token cost to vary per input — for example, weighting
 * a request by its payload size or priority — rather than charging a fixed amount
 * for every item that passes through a {@link TokenBucket}.
 *
 * @param <T> the type of value whose cost is being calculated
 */
public interface BucketQuantifier<T> {

  /**
   * Returns the token cost for {@code input}.
   * <p>
   * The returned value must be non-negative. A value of {@code 0} means the input
   * is always free; a value larger than the bucket's capacity means the input can
   * never be permitted.
   *
   * @param input the value whose token cost is to be calculated
   * @return the number of tokens to deduct from the bucket for this input
   */
  double quantity (T input);
}
