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
 * The outcome of one admission attempt. The balance and delay describe the decision
 * at the time it was made. Other admissions can change the balance before a retry.
 *
 * @param outcome         why the input was admitted or denied
 * @param remainingTokens smallest available whole-token balance across applicable limits,
 *                        after any debit, or {@code -1} when no balance applies
 *                        or the key could not be admitted to the tree
 * @param retryAfterNanos time for all applicable balances to cover the same cost if
 *                        there are no further debits. Zero on admission, {@code -1}
 *                        when there is no finite refill-based retry, and
 *                        {@link Long#MAX_VALUE} for delays beyond the representable range.
 */
public record BucketDecision (Outcome outcome, long remainingTokens, long retryAfterNanos) {

  static final BucketDecision ALLOWED_WITHOUT_BALANCE = new BucketDecision(Outcome.ALLOWED, -1, 0);
  static final BucketDecision RATE_LIMITED_WITHOUT_DETAILS = new BucketDecision(Outcome.RATE_LIMITED, -1, -1);

  /**
   * Returns whether the input was admitted and its cost charged to any applicable balances.
   */
  public boolean allowed () {

    return outcome == Outcome.ALLOWED;
  }

  public enum Outcome {

    /**
     * The input was admitted and its cost charged.
     */
    ALLOWED,

    /**
     * An applicable balance lacked the tokens to cover the cost.
     */
    RATE_LIMITED,

    /**
     * The cost exceeds the capacity of an applicable limit, so no wait can admit it.
     */
    COST_EXCEEDS_CAPACITY,

    /**
     * The selector returned {@code null} or a policy rejected the key or one of its prefixes.
     */
    KEY_REJECTED,

    /**
     * Admitting the key would exceed the maximum number of child nodes.
     */
    NODE_LIMIT_REACHED
  }
}
