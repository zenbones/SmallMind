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

import java.util.Objects;
import org.smallmind.nutsnbolts.time.Stint;

/**
 * One independently refilled token limit. A refill quantity of zero gives the limit a fixed budget.
 *
 * @param capacity       maximum balance in whole tokens
 * @param refillQuantity number of tokens accrued over one refill period
 * @param refillPeriod   strictly positive duration of one refill period
 * @param initialTokens  initial whole-token balance between zero and capacity
 * @param refillStyle    continuous or interval refill
 */
public record BucketLimit (long capacity, long refillQuantity, Stint refillPeriod, long initialTokens, RefillStyle refillStyle) {

  public BucketLimit {

    Objects.requireNonNull(refillPeriod, "refillPeriod");
    Objects.requireNonNull(refillStyle, "refillStyle");

    if ((capacity < 0) || (refillQuantity < 0) || (refillPeriod.getTime() <= 0)) {
      throw new IllegalArgumentException("Capacity and refill quantity must be non-negative and the refill period must be positive");
    }
    if ((initialTokens < 0) || (initialTokens > capacity)) {
      throw new IllegalArgumentException("Initial tokens must be between zero and capacity");
    }
  }

  /**
   * Creates a continuous limit with a full initial balance.
   */
  public BucketLimit (long capacity, long refillQuantity, Stint refillPeriod) {

    this(capacity, refillQuantity, refillPeriod, capacity, RefillStyle.CONTINUOUS);
  }

  public enum RefillStyle {

    /**
     * Accrues tokens proportionally to elapsed time, carrying fractional tokens forward.
     */
    CONTINUOUS,

    /**
     * Credits the full refill quantity at the end of each refill period.
     */
    INTERVAL
  }
}
