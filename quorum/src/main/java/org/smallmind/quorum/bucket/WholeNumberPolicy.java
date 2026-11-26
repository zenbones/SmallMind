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

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.quorum.bucket.BucketDecision.Outcome;

/**
 * One or more token limits evaluated together. Each limit uses primitive
 * arithmetic when its scaled quantities fit in a long and exact arithmetic otherwise.
 */
public final class WholeNumberPolicy extends BucketPolicy {

  private static final BigInteger MAX_RETRY_NANOS = BigInteger.valueOf(Long.MAX_VALUE);

  /**
   * Creates a standard policy with one or more limits.
   */
  public WholeNumberPolicy (List<BucketLimit> limits) {

    super(limits);
  }

  /**
   * Creates a standard policy with one or more limits.
   */
  public WholeNumberPolicy (BucketLimit... limits) {

    this(List.of(limits));
  }

  /**
   * Creates a single continuous limit that starts full.
   */
  public WholeNumberPolicy (long capacity, long refillQuantity, Stint refillPeriod) {

    this(new BucketLimit(capacity, refillQuantity, refillPeriod));
  }

  static TokenBucket.Target optimizedTarget (BucketPolicy policy, boolean rootTarget, LongSupplier clock) {

    long capacity = Long.MAX_VALUE;
    long initial = Long.MAX_VALUE;
    boolean startsFull = true;

    for (BucketLimit limit : policy.limits()) {
      if (limit.refillQuantity() != 0) {
        if (rootTarget && (policy.limits().size() == 1) && (limit.refillStyle() == BucketLimit.RefillStyle.CONTINUOUS)) {
          long tokenNanos = exactTokenNanos(limit);

          if (tokenNanos > 0) {
            return new ContinuousBudget(limit, tokenNanos, clock.getAsLong());
          }
        }

        return null;
      }
      capacity = Math.min(capacity, limit.capacity());
      initial = Math.min(initial, limit.initialTokens());
      startsFull &= limit.initialTokens() == limit.capacity();
    }

    return new FixedBudget(capacity, initial, startsFull);
  }

  private static long exactTokenNanos (BucketLimit limit) {

    long duration = limit.refillPeriod().getTime();
    long nanosPerUnit = limit.refillPeriod().getTimeUnit().toNanos(1);

    if (duration > Long.MAX_VALUE / nanosPerUnit) {
      return 0;
    }

    long periodNanos = duration * nanosPerUnit;
    long refill = limit.refillQuantity();
    long tokenNanos = periodNanos / refill;

    return ((tokenNanos > 0) && (periodNanos % refill == 0) && (limit.capacity() <= Long.MAX_VALUE / tokenNanos)) ? tokenNanos : 0;
  }

  interface BalanceState {

  }

  static class FixedBudget implements TokenBucket.Target {

    private static final long RETIRED = -1;

    private final AtomicLong remaining;
    private final boolean startsFull;
    private final long capacity;

    FixedBudget (long capacity, long initial, boolean startsFull) {

      this.capacity = capacity;
      this.startsFull = startsFull;

      remaining = new AtomicLong(initial);
    }

    BucketDecision consume (long quantity, boolean detailed) {

      long available = remaining.get();

      while (available != RETIRED) {
        if (quantity > available) {
          if (!detailed) {
            return BucketDecision.RATE_LIMITED_WITHOUT_DETAILS;
          }

          Outcome outcome = (quantity > capacity) ? Outcome.COST_EXCEEDS_CAPACITY : Outcome.RATE_LIMITED;

          return new BucketDecision(outcome, available, -1);
        }

        long next = available - quantity;
        long observed = (quantity == 0) ? available : remaining.compareAndExchange(available, next);

        if (observed == available) {
          return detailed ? new BucketDecision(Outcome.ALLOWED, next, 0) : BucketDecision.ALLOWED_WITHOUT_BALANCE;
        }
        available = observed;
      }

      return null;
    }

    boolean retire () {

      return startsFull && remaining.compareAndSet(capacity, RETIRED);
    }
  }

  static class ContinuousBudget implements TokenBucket.Target {

    // Only used for the root, where every value is available for a timestamp.
    private final AtomicLong fullAt;
    private final long capacity;
    private final long capacityNanos;
    private final long tokenNanos;

    ContinuousBudget (BucketLimit limit, long tokenNanos, long now) {

      capacity = limit.capacity();
      this.tokenNanos = tokenNanos;
      capacityNanos = capacity * tokenNanos;
      fullAt = new AtomicLong(now + (capacity - limit.initialTokens()) * tokenNanos);
    }

    BucketDecision consume (long quantity, boolean detailed, LongSupplier clock) {

      if (!detailed) {
        if (quantity == 0) {
          return BucketDecision.ALLOWED_WITHOUT_BALANCE;
        }
        if (quantity > capacity) {
          return BucketDecision.RATE_LIMITED_WITHOUT_DETAILS;
        }
      }

      long previous = fullAt.get();

      while (true) {
        long now = clock.getAsLong();
        long available = Math.max(0, capacityNanos - Math.max(0, previous - now));

        if (quantity > capacity) {
          return new BucketDecision(Outcome.COST_EXCEEDS_CAPACITY, available / tokenNanos, -1);
        }

        long cost = quantity * tokenNanos;

        if (cost > available) {
          return detailed ? new BucketDecision(Outcome.RATE_LIMITED, available / tokenNanos, cost - available) : BucketDecision.RATE_LIMITED_WITHOUT_DETAILS;
        }

        long next = now + (capacityNanos - available) + cost;
        long observed = (quantity == 0) ? previous : fullAt.compareAndExchange(previous, next);

        if (observed == previous) {
          return detailed ? new BucketDecision(Outcome.ALLOWED, (available - cost) / tokenNanos, 0) : BucketDecision.ALLOWED_WITHOUT_BALANCE;
        }
        previous = observed;
      }
    }
  }

  static class Attempt {

    private Outcome outcome = Outcome.ALLOWED;
    private long available = Long.MAX_VALUE;
    private long retry;

    void reset () {

      outcome = Outcome.ALLOWED;
      available = Long.MAX_VALUE;
      retry = 0;
    }

    BucketDecision decision (long quantity) {

      long remaining = (outcome == Outcome.ALLOWED) ? available - quantity : available;

      return new BucketDecision(outcome, remaining, retry);
    }
  }

  private record LimitState(long tokens, BigInteger wideTokens, long timestamp) implements BalanceState {

  }

  private record MultipleLimitState(LimitState[] limits) implements BalanceState {

  }

  static class Calculator {

    private final Limit[] limits;
    private final boolean refills;

    Calculator (BucketPolicy policy) {

      List<BucketLimit> configured = policy.limits();
      boolean hasRefillingLimit = false;

      limits = new Limit[configured.size()];
      for (int index = 0; index < limits.length; index++) {
        Limit limit = new Limit(configured.get(index));

        limits[index] = limit;
        hasRefillingLimit |= limit.refillQuantity > 0;
      }
      refills = hasRefillingLimit;
    }

    BalanceState initialState (long now) {

      if (limits.length == 1) {
        return limits[0].initialState(now);
      }

      LimitState[] initial = new LimitState[limits.length];

      for (int index = 0; index < limits.length; index++) {
        initial[index] = limits[index].initialState(now);
      }

      return new MultipleLimitState(initial);
    }

    boolean refills () {

      return refills;
    }

    BalanceState evaluate (BalanceState previous, long now, long quantity, Attempt attempt) {

      if (previous instanceof LimitState single) {
        return limits[0].evaluate(single, now, quantity, attempt);
      }

      LimitState[] previousLimits = ((MultipleLimitState)previous).limits();
      LimitState[] next = null;

      for (int index = 0; index < previousLimits.length; index++) {
        LimitState update = limits[index].evaluate(previousLimits[index], now, quantity, attempt);

        if (update == null) {
          if (attempt == null) {
            return null;
          }
        } else if ((quantity != 0) && ((attempt == null) || (attempt.outcome == Outcome.ALLOWED))) {
          if (next == null) {
            next = new LimitState[previousLimits.length];
          }
          next[index] = update;
        }
      }

      if ((attempt != null) && (attempt.outcome != Outcome.ALLOWED)) {
        return null;
      }

      return (next == null) ? previous : new MultipleLimitState(next);
    }

    boolean canRetire (BalanceState previous, long now) {

      if (previous instanceof LimitState single) {
        return limits[0].canRetire(single, now);
      }

      LimitState[] previousLimits = ((MultipleLimitState)previous).limits();

      for (int index = 0; index < previousLimits.length; index++) {
        if (!limits[index].canRetire(previousLimits[index], now)) {
          return false;
        }
      }

      return true;
    }
  }

  private static class Limit {

    private final long capacity;
    private final long refillQuantity;
    private final long initialTokens;
    private final boolean interval;
    private final boolean reclaimable;
    private final long periodNanos;
    private final long scaledCapacity;
    private final long creditPerStep;
    private final long maximumRefillSteps;
    private final WideLimit wide;

    private Limit (BucketLimit configuration) {

      capacity = configuration.capacity();
      refillQuantity = configuration.refillQuantity();
      initialTokens = configuration.initialTokens();
      interval = configuration.refillStyle() == BucketLimit.RefillStyle.INTERVAL;
      reclaimable = (initialTokens == capacity) && (!interval || (refillQuantity == 0));

      long duration = configuration.refillPeriod().getTime();
      long nanosPerUnit = configuration.refillPeriod().getTimeUnit().toNanos(1);
      long period = 1;

      if (refillQuantity != 0) {
        period = (duration <= Long.MAX_VALUE / nanosPerUnit) ? duration * nanosPerUnit : 0;
      }

      // One token occupies one period of scaled units, retaining fractional accrual.
      if ((period > 0) && (capacity <= Long.MAX_VALUE / period)) {
        periodNanos = period;
        scaledCapacity = capacity * period;
        creditPerStep = interval ? Math.min(capacity, refillQuantity) * period : refillQuantity;
        maximumRefillSteps = (creditPerStep == 0) ? Long.MAX_VALUE : Long.MAX_VALUE / creditPerStep;
        wide = null;
      } else {
        periodNanos = 0;
        scaledCapacity = 0;
        creditPerStep = 0;
        maximumRefillSteps = 0;
        wide = new WideLimit(BigInteger.valueOf(duration).multiply(BigInteger.valueOf(nanosPerUnit)));
      }
    }

    private LimitState initialState (long now) {

      return (wide == null) ? new LimitState(initialTokens * periodNanos, null, now) : new LimitState(0, wide.scale(initialTokens), now);
    }

    private LimitState evaluate (LimitState previous, long now, long quantity, Attempt attempt) {

      if (wide != null) {
        return wide.evaluate(previous, now, quantity, attempt);
      }

      long elapsed = Math.max(0, now - previous.timestamp());
      long steps = interval ? elapsed / periodNanos : elapsed;
      long available = refill(previous.tokens(), steps);

      if (attempt != null) {
        attempt.available = Math.min(attempt.available, available / periodNanos);
      }
      if (quantity > capacity) {
        if (attempt != null) {
          attempt.outcome = Outcome.COST_EXCEEDS_CAPACITY;
          attempt.retry = -1;
        }

        return null;
      }

      long cost = quantity * periodNanos;

      if (cost > available) {
        if ((attempt != null) && requiresRetryDelay(attempt)) {
          long deficit = cost - available;
          long delay = interval ? intervalDelay(deficit, elapsed % periodNanos) : Math.ceilDiv(deficit, refillQuantity);

          attempt.retry = Math.max(attempt.retry, delay);
        }

        return null;
      }

      long timestamp = previous.timestamp();

      if ((steps > 0) && (refillQuantity > 0)) {
        timestamp = interval ? now - elapsed % periodNanos : now;
      }

      return ((cost == 0) || ((attempt != null) && (attempt.outcome != Outcome.ALLOWED)))
               ? previous : new LimitState(available - cost, null, timestamp);
    }

    private long refill (long tokens, long steps) {

      if ((steps == 0) || (tokens == scaledCapacity)) {
        return tokens;
      }

      long credit = (steps > maximumRefillSteps) ? Long.MAX_VALUE : steps * creditPerStep;

      return tokens + Math.min(scaledCapacity - tokens, credit);
    }

    private long intervalDelay (long deficit, long offset) {

      // Each refill credits at least one whole token, so the delay fits in scaledCapacity.
      return Math.ceilDiv(deficit, creditPerStep) * periodNanos - offset;
    }

    private boolean requiresRetryDelay (Attempt attempt) {

      if (attempt.outcome == Outcome.ALLOWED) {
        attempt.outcome = Outcome.RATE_LIMITED;
      }
      if (refillQuantity == 0) {
        attempt.retry = -1;
      }

      return attempt.retry >= 0;
    }

    private boolean canRetire (LimitState previous, long now) {

      if (!reclaimable) {
        return false;
      }
      if (wide != null) {
        return wide.refill(previous, now).wideTokens().equals(wide.scaledCapacity);
      }

      long elapsed = Math.max(0, now - previous.timestamp());
      long steps = interval ? elapsed / periodNanos : elapsed;

      return refill(previous.tokens(), steps) == scaledCapacity;
    }

    private class WideLimit {

      private final BigInteger period;
      private final BigInteger rate;
      private final BigInteger scaledCapacity;
      private final BigInteger intervalCredit;

      private WideLimit (BigInteger period) {

        this.period = period;
        rate = BigInteger.valueOf(refillQuantity);
        scaledCapacity = scale(capacity);
        intervalCredit = scale(refillQuantity);
      }

      private BigInteger scale (long tokens) {

        return period.multiply(BigInteger.valueOf(tokens));
      }

      private LimitState evaluate (LimitState previous, long now, long quantity, Attempt attempt) {

        LimitState refilled = refill(previous, now);

        if (attempt != null) {
          attempt.available = Math.min(attempt.available, refilled.wideTokens().divide(period).longValue());
        }
        if (quantity > capacity) {
          if (attempt != null) {
            attempt.outcome = Outcome.COST_EXCEEDS_CAPACITY;
            attempt.retry = -1;
          }

          return null;
        }

        BigInteger difference = refilled.wideTokens().subtract(scale(quantity));

        if (difference.signum() < 0) {
          if ((attempt != null) && requiresRetryDelay(attempt)) {
            BigInteger deficit = difference.negate();
            BigInteger delay;

            if (interval) {
              delay = ceilDivide(deficit, intervalCredit).multiply(period)
                        .subtract(BigInteger.valueOf(Math.max(0, now - refilled.timestamp())));
            } else {
              delay = ceilDivide(deficit, rate);
            }
            attempt.retry = Math.max(attempt.retry, delay.min(MAX_RETRY_NANOS).longValue());
          }

          return null;
        }

        return ((quantity == 0) || ((attempt != null) && (attempt.outcome != Outcome.ALLOWED)))
                 ? previous : new LimitState(0, difference, refilled.timestamp());
      }

      private LimitState refill (LimitState state, long now) {

        long elapsed = now - state.timestamp();

        if ((elapsed <= 0) || (refillQuantity == 0)) {
          return state;
        }

        BigInteger credit;
        long timestamp;

        if (interval) {
          BigInteger[] periods = BigInteger.valueOf(elapsed).divideAndRemainder(period);

          if (periods[0].signum() == 0) {
            return state;
          }
          credit = intervalCredit.multiply(periods[0]);
          timestamp = now - periods[1].longValue();
        } else {
          credit = rate.multiply(BigInteger.valueOf(elapsed));
          timestamp = now;
        }

        return new LimitState(0, state.wideTokens().add(credit).min(scaledCapacity), timestamp);
      }

      private BigInteger ceilDivide (BigInteger numerator, BigInteger denominator) {

        return numerator.add(denominator).subtract(BigInteger.ONE).divide(denominator);
      }
    }
  }
}
