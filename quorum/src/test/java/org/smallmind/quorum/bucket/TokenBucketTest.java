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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.quorum.bucket.BucketDecision.Outcome;
import org.smallmind.quorum.bucket.BucketLimit.RefillStyle;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TokenBucketTest {

  private static final Stint ONE_SECOND = Stint.of(1, TimeUnit.SECONDS);
  private static final BucketPolicy ONE_TOKEN = new WholeNumberPolicy(1, 0, ONE_SECOND);

  public void testStandaloneChargesAnExactFitAndAllowsZeroCost () {

    TokenBucket<Void, Long> bucket = new TokenBucket<>(new WholeNumberPolicy(5, 0, ONE_SECOND), Long::longValue);

    Assert.assertFalse(bucket.allowed(6L));
    Assert.assertTrue(bucket.allowed(5L));
    Assert.assertFalse(bucket.allowed(1L));
    Assert.assertTrue(bucket.allowed(0L));
  }

  public void testStackedFixedLimitsTrackTheSmallestBalanceAndCapacity () {

    InherentBucketKey<String> key = InherentBucketKey.of("tenant");
    BucketPolicy policy = new WholeNumberPolicy(new BucketLimit(3, 0, ONE_SECOND),
      new BucketLimit(Long.MAX_VALUE, 0, ONE_SECOND, 1, RefillStyle.INTERVAL));
    TokenBucket<String, Long> bucket = new TokenBucket<>(1, input -> key, (selected, input) -> input, selected -> policy);

    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.RATE_LIMITED, 1, -1));
    Assert.assertEquals(bucket.tryConsume(4L), new BucketDecision(Outcome.COST_EXCEEDS_CAPACITY, 1, -1));
    Assert.assertFalse(bucket.removeIfFull(key));
    Assert.assertTrue(bucket.allowed(1L));
    Assert.assertEquals(bucket.tryConsume(0L), new BucketDecision(Outcome.ALLOWED, 0, 0));
    Assert.assertFalse(bucket.removeIfFull(key));
  }

  public void testContinuousRootReportsExactRetryFromPartialBalance () {

    AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 1);
    BucketPolicy policy = new WholeNumberPolicy(new BucketLimit(3, 2, Stint.of(8, TimeUnit.NANOSECONDS), 1, RefillStyle.CONTINUOUS));
    TokenBucket<Void, Long> bucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input, key -> policy, clock::get);

    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.RATE_LIMITED, 1, 4));
    clock.addAndGet(3);
    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.RATE_LIMITED, 1, 1));
    clock.incrementAndGet();
    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.ALLOWED, 0, 0));
    clock.addAndGet(15);
    Assert.assertEquals(bucket.tryConsume(1L), new BucketDecision(Outcome.ALLOWED, 2, 0));
  }

  public void testRefillRetainsFractionsAndCapsTheBalance () {

    AtomicLong clock = new AtomicLong();
    TokenBucket<Void, Long> bucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input,
      key -> new WholeNumberPolicy(3, 3, Stint.of(10, TimeUnit.NANOSECONDS)), clock::get);

    Assert.assertTrue(bucket.allowed(3L));
    clock.set(3);
    Assert.assertFalse(bucket.allowed(1L));
    clock.set(4);
    Assert.assertTrue(bucket.allowed(1L));
    clock.set(7);
    Assert.assertTrue(bucket.allowed(1L));
    Assert.assertFalse(bucket.allowed(1L));
    clock.set(100);
    Assert.assertTrue(bucket.allowed(3L));
    Assert.assertFalse(bucket.allowed(1L));
  }

  public void testRefillAcrossNanoTimeWrap () {

    AtomicLong clock = new AtomicLong(Long.MAX_VALUE);
    TokenBucket<Void, Long> bucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input,
      key -> new WholeNumberPolicy(1, 1, Stint.of(2, TimeUnit.NANOSECONDS)), clock::get);

    Assert.assertTrue(bucket.allowed(1L));
    Assert.assertEquals(clock.incrementAndGet(), Long.MIN_VALUE);
    Assert.assertFalse(bucket.allowed(1L));
    clock.incrementAndGet();
    Assert.assertTrue(bucket.allowed(1L));
  }

  public void testScaledLongRefillSaturatesBeforeOverflow () {

    long capacity = Long.MAX_VALUE / 3;

    for (RefillStyle style : RefillStyle.values()) {
      AtomicLong clock = new AtomicLong();
      BucketPolicy policy = new WholeNumberPolicy(new BucketLimit(capacity, Long.MAX_VALUE, Stint.of(3, TimeUnit.NANOSECONDS), 0, style));
      TokenBucket<Void, Long> bucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input, key -> policy, clock::get);

      Assert.assertEquals(bucket.tryConsume(capacity + 1), new BucketDecision(Outcome.COST_EXCEEDS_CAPACITY, 0, -1));
      Assert.assertEquals(bucket.tryConsume(capacity), new BucketDecision(Outcome.RATE_LIMITED, 0, style == RefillStyle.INTERVAL ? 3 : 1));
      clock.set(Long.MAX_VALUE - 1);
      Assert.assertEquals(bucket.tryConsume(capacity), new BucketDecision(Outcome.ALLOWED, 0, 0));
      Assert.assertFalse(bucket.allowed(1L));
    }
  }

  public void testLongAndWideLimitsShareOneDecision () {

    AtomicLong clock = new AtomicLong();
    BucketPolicy policy = new WholeNumberPolicy(
      new BucketLimit(3, 1, Stint.of(4, TimeUnit.NANOSECONDS), 0, RefillStyle.CONTINUOUS),
      new BucketLimit(Long.MAX_VALUE, 1, Stint.of(2, TimeUnit.NANOSECONDS), 0, RefillStyle.INTERVAL));
    TokenBucket<Void, Long> bucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input, key -> policy, clock::get);

    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.RATE_LIMITED, 0, 8));
    clock.set(7);
    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.RATE_LIMITED, 1, 1));
    clock.set(8);
    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.ALLOWED, 0, 0));
  }

  public void testFullRangeCapacityAndRefill () {

    AtomicLong clock = new AtomicLong();
    TokenBucket<Void, Long> bucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input,
      key -> new WholeNumberPolicy(Long.MAX_VALUE, Long.MAX_VALUE, Stint.of(2, TimeUnit.NANOSECONDS)), clock::get);

    Assert.assertTrue(bucket.allowed(Long.MAX_VALUE));
    clock.set(1);
    Assert.assertFalse(bucket.allowed(Long.MAX_VALUE / 2 + 1));
    Assert.assertTrue(bucket.allowed(Long.MAX_VALUE / 2));
    clock.set(2);
    Assert.assertTrue(bucket.allowed(Long.MAX_VALUE / 2 + 1));

    TokenBucket<Void, Long> slowBucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input,
      key -> new WholeNumberPolicy(Long.MAX_VALUE, Long.MAX_VALUE, Stint.of(Long.MAX_VALUE, TimeUnit.DAYS)), clock::get);

    Assert.assertTrue(slowBucket.allowed(Long.MAX_VALUE));
    clock.set(Long.MAX_VALUE);
    Assert.assertTrue(slowBucket.allowed(1L));
    Assert.assertFalse(slowBucket.allowed(Long.MAX_VALUE));
  }

  public void testPathCreationAndKeyAwareCost () {

    List<BucketKey<String, ?>> configured = new ArrayList<>();
    AtomicInteger selections = new AtomicInteger();
    AtomicInteger quantifications = new AtomicInteger();
    BucketSelector<String, Request> selector = request -> {
      selections.incrementAndGet();

      return request.path();
    };
    BucketQuantifier<String, Request> quantifier = (key, request) -> {
      Assert.assertSame(key, request.path());
      quantifications.incrementAndGet();

      return request.cost();
    };
    PolicyFactory<String> factory = key -> {
      configured.add(key);

      return new WholeNumberPolicy(2, 0, ONE_SECOND);
    };
    TokenBucket<String, Request> bucket = new TokenBucket<>(4, selector, quantifier, factory);
    InherentBucketKey<String> tenant = InherentBucketKey.of("tenant");
    InherentBucketKey<String> alpha = InherentBucketKey.of("tenant", "alpha");
    InherentBucketKey<String> beta = InherentBucketKey.of("tenant", "beta");

    Assert.assertTrue(configured.isEmpty());
    Assert.assertTrue(bucket.allowed(new Request(tenant, 2)));
    Assert.assertTrue(bucket.allowed(new Request(alpha, 1)));
    Assert.assertTrue(bucket.allowed(new Request(InherentBucketKey.of("tenant", "alpha"), 1)));
    Assert.assertFalse(bucket.allowed(new Request(alpha, 1)));
    Assert.assertTrue(bucket.allowed(new Request(beta, 2)));
    Assert.assertFalse(bucket.allowed(new Request(tenant, 1)));
    Assert.assertEquals(configured, List.of(tenant, alpha, beta));
    Assert.assertEquals(selections.get(), 6);
    Assert.assertEquals(quantifications.get(), 6);
  }

  public void testRoutingNodesCountTowardTheLimit () {

    List<BucketKey<String, ?>> configured = new ArrayList<>();
    TokenBucket<String, InherentBucketKey<String>> bucket = new TokenBucket<>(2, key -> key, (key, input) -> 1, key -> {
      configured.add(key);

      return ONE_TOKEN;
    });
    InherentBucketKey<String> tenant = InherentBucketKey.of("tenant");
    InherentBucketKey<String> principal = InherentBucketKey.of("tenant", "principal");

    Assert.assertFalse(bucket.allowed(InherentBucketKey.of("other", "team", "principal")));
    Assert.assertTrue(bucket.allowed(principal));
    Assert.assertFalse(bucket.allowed(principal));
    Assert.assertFalse(bucket.allowed(InherentBucketKey.of("tenant", "other")));
    Assert.assertTrue(bucket.allowed(tenant));
    Assert.assertTrue(bucket.allowed(InherentBucketKey.root()));
    Assert.assertEquals(configured, List.of(principal, tenant, InherentBucketKey.root()));
  }

  public void testUnresolvedKeysAndFailedFactoriesDoNotSpendNodeCapacity () {

    TokenBucket<String, InherentBucketKey<String>> bucket = new TokenBucket<>(1, key -> key, (key, input) -> 1, key -> {
      if (key.equals(InherentBucketKey.of("error"))) {
        throw new IllegalStateException("Policy unavailable");
      }

      return key.equals(InherentBucketKey.of("known")) ? ONE_TOKEN : null;
    });

    Assert.assertFalse(bucket.allowed(null));
    Assert.assertThrows(IllegalStateException.class, () -> bucket.allowed(InherentBucketKey.of("error")));
    Assert.assertFalse(bucket.allowed(InherentBucketKey.of("unknown")));
    Assert.assertTrue(bucket.allowed(InherentBucketKey.of("known")));
  }

  public void testConcurrentFirstUseSharesOneBalance ()
    throws Exception {

    AtomicInteger configured = new AtomicInteger();
    TokenBucket<String, String> bucket = new TokenBucket<>(2, principal -> InherentBucketKey.of("tenant", principal), (key, input) -> 1, key -> {
      configured.incrementAndGet();

      return new WholeNumberPolicy(5, 0, ONE_SECOND);
    });
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();

    try (ExecutorService executor = Executors.newFixedThreadPool(8, Thread.ofPlatform().name("quorum-bucket-test-", 0).daemon().factory())) {
      for (int index = 0; index < 32; index++) {
        results.add(executor.submit(() -> {
          start.await();

          return bucket.allowed("principal");
        }));
      }
      start.countDown();

      int admitted = 0;

      for (Future<Boolean> result : results) {
        if (result.get(5, TimeUnit.SECONDS)) {
          ++admitted;
        }
      }
      Assert.assertEquals(admitted, 5);
    }
    Assert.assertEquals(configured.get(), 1);
  }

  public void testInvalidCostsAndPolicies () {

    TokenBucket<Void, Long> bucket = new TokenBucket<>(ONE_TOKEN, Long::longValue);

    Assert.assertThrows(IllegalArgumentException.class, () -> bucket.allowed(-1L));
    Assert.assertTrue(bucket.allowed(1L));
    Assert.assertFalse(bucket.allowed(1L));

    Assert.assertThrows(IllegalArgumentException.class, () -> new WholeNumberPolicy(-1, 0, ONE_SECOND));
    Assert.assertThrows(IllegalArgumentException.class, () -> new WholeNumberPolicy(1, -1, ONE_SECOND));
    Assert.assertThrows(IllegalArgumentException.class, () -> new WholeNumberPolicy(1, 0, Stint.none()));
    Assert.assertThrows(IllegalArgumentException.class, () -> new TokenBucket<>(-1, input -> InherentBucketKey.root(), (key, input) -> 1, key -> ONE_TOKEN));
  }

  public void testKeyCopiesItsSegments () {

    List<String> segments = new ArrayList<>(List.of("tenant"));
    InherentBucketKey<String> key = new InherentBucketKey<>(segments);

    segments.add("principal");

    Assert.assertEquals(key, InherentBucketKey.of("tenant"));
    String tenant = key.segments().getFirst();
    Assert.assertEquals(tenant, "tenant");
    Assert.assertThrows(UnsupportedOperationException.class, () -> key.segments().clear());

    InherentBucketKey<String> root = InherentBucketKey.root();
    Assert.assertSame(root, InherentBucketKey.root());
    Assert.assertTrue(root.segments().isEmpty());
  }

  public void testDecisionDistinguishesDenialReasons () {

    TokenBucket<String, InherentBucketKey<String>> bucket = new TokenBucket<>(1, key -> key, (key, input) -> 2,
      key -> key.equals(InherentBucketKey.of("known")) ? ONE_TOKEN : null);

    Assert.assertEquals(bucket.tryConsume(null), new BucketDecision(Outcome.KEY_REJECTED, -1, -1));
    Assert.assertEquals(bucket.tryConsume(InherentBucketKey.of("unknown")), new BucketDecision(Outcome.KEY_REJECTED, -1, -1));
    Assert.assertEquals(bucket.tryConsume(InherentBucketKey.of("known")), new BucketDecision(Outcome.COST_EXCEEDS_CAPACITY, 1, -1));
    Assert.assertEquals(bucket.tryConsume(InherentBucketKey.of("other")), new BucketDecision(Outcome.NODE_LIMIT_REACHED, -1, -1));

    TokenBucket<Void, Long> fixed = new TokenBucket<>(ONE_TOKEN, Long::longValue);

    Assert.assertEquals(fixed.tryConsume(1L), new BucketDecision(Outcome.ALLOWED, 0, 0));
    Assert.assertEquals(fixed.tryConsume(1L), new BucketDecision(Outcome.RATE_LIMITED, 0, -1));
  }

  public void testStackedLimitsUseLongestRetryWithoutPartialDebit () {

    AtomicLong clock = new AtomicLong();
    BucketPolicy policy = new WholeNumberPolicy(
      new BucketLimit(3, 2, Stint.of(3, TimeUnit.NANOSECONDS), 0, RefillStyle.CONTINUOUS),
      new BucketLimit(4, 1, Stint.of(5, TimeUnit.NANOSECONDS), 1, RefillStyle.INTERVAL));
    TokenBucket<Void, Long> bucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input, key -> policy, clock::get);

    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.RATE_LIMITED, 0, 5));
    clock.set(4);
    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.RATE_LIMITED, 1, 1));
    clock.set(5);
    Assert.assertEquals(bucket.tryConsume(2L), new BucketDecision(Outcome.ALLOWED, 0, 0));
    clock.set(7);
    Assert.assertEquals(bucket.tryConsume(1L), new BucketDecision(Outcome.RATE_LIMITED, 0, 3));
    clock.set(10);
    Assert.assertEquals(bucket.tryConsume(1L), new BucketDecision(Outcome.ALLOWED, 0, 0));
  }

  public void testRetryRoundsUpAndSaturates () {

    AtomicLong clock = new AtomicLong();
    BucketPolicy continuous = new WholeNumberPolicy(new BucketLimit(3, 2, Stint.of(3, TimeUnit.NANOSECONDS), 0, RefillStyle.CONTINUOUS));
    TokenBucket<Void, Long> bucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input, key -> continuous, clock::get);
    BucketPolicy slow = new WholeNumberPolicy(new BucketLimit(1, 1, Stint.of(Long.MAX_VALUE, TimeUnit.DAYS), 0, RefillStyle.INTERVAL));
    TokenBucket<Void, Long> slowBucket = new TokenBucket<>(0, input -> InherentBucketKey.root(), (key, input) -> input, key -> slow, clock::get);

    Assert.assertEquals(bucket.tryConsume(1L), new BucketDecision(Outcome.RATE_LIMITED, 0, 2));
    Assert.assertEquals(slowBucket.tryConsume(1L), new BucketDecision(Outcome.RATE_LIMITED, 0, Long.MAX_VALUE));
  }

  public void testSharedQuotasChargeTheEntirePathOrNothing () {

    AtomicLong clock = new AtomicLong();
    BucketPolicy sharedTenant = new WholeNumberPolicy(2, 2, Stint.of(10, TimeUnit.NANOSECONDS));
    BucketPolicy sharedRoot = new WholeNumberPolicy(3, 3, Stint.of(30, TimeUnit.NANOSECONDS));
    TokenBucket<String, InherentBucketKey<String>> bucket = new TokenBucket<>(8, key -> key, (key, input) -> 1, key -> ONE_TOKEN, key -> {
      if (key.equals(InherentBucketKey.root())) {
        return sharedRoot;
      }

      return (key.segments().size() == 1) ? sharedTenant : null;
    }, clock::get);
    InherentBucketKey<String> first = InherentBucketKey.of("tenant", "first");
    InherentBucketKey<String> second = InherentBucketKey.of("tenant", "second");
    InherentBucketKey<String> third = InherentBucketKey.of("tenant", "third");
    InherentBucketKey<String> other = InherentBucketKey.of("other", "first");

    Assert.assertTrue(bucket.allowed(first));
    Assert.assertEquals(bucket.tryConsume(first), new BucketDecision(Outcome.RATE_LIMITED, 0, -1));
    Assert.assertTrue(bucket.allowed(second));
    Assert.assertEquals(bucket.tryConsume(third), new BucketDecision(Outcome.RATE_LIMITED, 0, 5));
    Assert.assertTrue(bucket.allowed(other));
    clock.set(5);
    Assert.assertEquals(bucket.tryConsume(third), new BucketDecision(Outcome.RATE_LIMITED, 0, 5));
    clock.set(10);
    Assert.assertTrue(bucket.allowed(third));
  }

  public void testUnlimitedPoliciesAdmitWithoutReadingTheClock () {

    InherentBucketKey<String> key = InherentBucketKey.of("tenant", "principal");
    TokenBucket<String, Long> bucket = new TokenBucket<>(2, input -> key, (selected, input) -> input,
      selected -> UnlimitedPolicy.instance(), prefix -> UnlimitedPolicy.instance(), () -> {
        throw new AssertionError("Unlimited admission and reclamation do not need the clock");
      });

    Assert.assertThrows(IllegalArgumentException.class, () -> bucket.allowed(-1L));
    Assert.assertTrue(bucket.allowed(Long.MAX_VALUE));
    Assert.assertEquals(bucket.tryConsume(Long.MAX_VALUE), new BucketDecision(Outcome.ALLOWED, -1, 0));
    Assert.assertTrue(bucket.removeIfFull(key));
    Assert.assertTrue(bucket.allowed(Long.MAX_VALUE));
  }

  public void testUnlimitedTargetChargesOnlyApplicableSharedBalances () {

    InherentBucketKey<String> tenant = InherentBucketKey.of("tenant");
    InherentBucketKey<String> first = InherentBucketKey.of("tenant", "first");
    InherentBucketKey<String> second = InherentBucketKey.of("tenant", "second");
    AtomicLong clock = new AtomicLong();
    WholeNumberPolicy rootPolicy = new WholeNumberPolicy(4, 0, ONE_SECOND);
    WholeNumberPolicy tenantPolicy = new WholeNumberPolicy(3, 1, Stint.of(10, TimeUnit.NANOSECONDS));
    WholeNumberPolicy secondPolicy = new WholeNumberPolicy(2, 0, ONE_SECOND);
    TokenBucket<String, Request> bucket = new TokenBucket<>(3, Request::path, (key, request) -> request.cost(),
      key -> UnlimitedPolicy.instance(), prefix -> {
        if (prefix.equals(InherentBucketKey.root())) {
          return rootPolicy;
        }

        if (prefix.equals(tenant)) {
          return tenantPolicy;
        }

        return prefix.equals(second) ? secondPolicy : null;
      }, clock::get);

    Assert.assertTrue(bucket.allowed(new Request(first, 2)));
    Assert.assertTrue(bucket.removeIfFull(first));
    Assert.assertFalse(bucket.allowed(new Request(first, 2)));
    Assert.assertEquals(bucket.tryConsume(new Request(second, 2)), new BucketDecision(Outcome.RATE_LIMITED, 1, 10));
    clock.set(10);
    Assert.assertEquals(bucket.tryConsume(new Request(second, 2)), new BucketDecision(Outcome.ALLOWED, 0, 0));
    Assert.assertFalse(bucket.removeIfFull(second));
  }

  public void testRejectedPoliciesRefuseKeysAndSubtreesWithoutSpendingQuota () {

    InherentBucketKey<String> direct = InherentBucketKey.of("direct");
    InherentBucketKey<String> blocked = InherentBucketKey.of("blocked");
    InherentBucketKey<String> child = InherentBucketKey.of("blocked", "child");
    InherentBucketKey<String> legacy = InherentBucketKey.of("legacy");
    InherentBucketKey<String> admitted = InherentBucketKey.of("admitted");
    TokenBucket<String, InherentBucketKey<String>> bucket = new TokenBucket<>(2, key -> key, (key, input) -> 1,
      key -> key.equals(direct) ? RejectedPolicy.instance() : key.equals(legacy) ? null : UnlimitedPolicy.instance(),
      prefix -> {
        if (prefix.equals(InherentBucketKey.root())) {
          return new WholeNumberPolicy(1, 0, ONE_SECOND);
        }

        return prefix.equals(blocked) ? RejectedPolicy.instance() : UnlimitedPolicy.instance();
      });
    BucketDecision rejected = new BucketDecision(Outcome.KEY_REJECTED, -1, -1);

    Assert.assertEquals(bucket.tryConsume(direct), rejected);
    Assert.assertEquals(bucket.tryConsume(legacy), rejected);
    Assert.assertEquals(bucket.tryConsume(blocked), rejected);
    Assert.assertEquals(bucket.tryConsume(child), rejected);
    Assert.assertTrue(bucket.allowed(admitted));
    Assert.assertFalse(bucket.allowed(admitted));
  }

  public void testRejectedSharedRootSkipsTargetResolution () {

    TokenBucket<String, InherentBucketKey<String>> bucket = new TokenBucket<>(0, key -> key, (key, input) -> 0, key -> {
      throw new AssertionError("A rejected root rules out every target");
    }, prefix -> RejectedPolicy.instance(), () -> {
      throw new AssertionError("A rejected policy has no balance to initialize");
    });
    BucketDecision rejected = new BucketDecision(Outcome.KEY_REJECTED, -1, -1);

    Assert.assertEquals(bucket.tryConsume(InherentBucketKey.root()), rejected);
    Assert.assertEquals(bucket.tryConsume(InherentBucketKey.of("child")), rejected);
  }

  public void testConcurrentSiblingsShareAnAggregateBudget ()
    throws Exception {

    TokenBucket<Integer, Integer> bucket = new TokenBucket<>(32, input -> InherentBucketKey.of(input), (key, input) -> 1, key -> ONE_TOKEN,
      key -> key.equals(InherentBucketKey.root()) ? new WholeNumberPolicy(5, 0, ONE_SECOND) : null);
    List<Future<Boolean>> results = new ArrayList<>();

    try (ExecutorService executor = Executors.newFixedThreadPool(8, Thread.ofPlatform().name("quorum-bucket-shared-test-", 0).daemon().factory())) {
      for (int index = 0; index < 32; index++) {
        int input = index;

        results.add(executor.submit(() -> bucket.allowed(input)));
      }

      int admitted = 0;

      for (Future<Boolean> result : results) {
        if (result.get(5, TimeUnit.SECONDS)) {
          ++admitted;
        }
      }
      Assert.assertEquals(admitted, 5);
    }
  }

  public void testReclaimsOnlyFullRestartableLeaves () {

    AtomicLong clock = new AtomicLong();
    InherentBucketKey<String> parent = InherentBucketKey.of("tenant");
    InherentBucketKey<String> leaf = InherentBucketKey.of("tenant", "principal");
    InherentBucketKey<String> replacement = InherentBucketKey.of("replacement");
    BucketPolicy policy = new WholeNumberPolicy(1, 1, Stint.of(10, TimeUnit.NANOSECONDS));
    TokenBucket<String, InherentBucketKey<String>> bucket = new TokenBucket<>(2, key -> key, (key, input) -> 1, key -> policy, clock::get);

    Assert.assertTrue(bucket.allowed(leaf));
    Assert.assertFalse(bucket.removeIfFull(parent));
    Assert.assertFalse(bucket.removeIfFull(leaf));
    Assert.assertFalse(bucket.allowed(replacement));
    clock.set(10);
    Assert.assertTrue(bucket.removeIfFull(leaf));
    Assert.assertTrue(bucket.removeIfFull(parent));
    Assert.assertTrue(bucket.allowed(replacement));

    BucketPolicy interval = new WholeNumberPolicy(new BucketLimit(1, 1, Stint.of(10, TimeUnit.NANOSECONDS), 1, RefillStyle.INTERVAL));
    TokenBucket<String, InherentBucketKey<String>> intervalBucket = new TokenBucket<>(1, key -> key, (key, input) -> 0, key -> interval, clock::get);

    Assert.assertTrue(intervalBucket.allowed(replacement));
    Assert.assertFalse(intervalBucket.removeIfFull(replacement));
  }

  public void testAdmissionRetriesIfItsNodeIsReclaimed ()
    throws Exception {

    InherentBucketKey<String> key = InherentBucketKey.of("principal");
    CountDownLatch reading = new CountDownLatch(1);
    CountDownLatch proceed = new CountDownLatch(1);
    AtomicInteger firstRead = new AtomicInteger();
    BucketPolicy policy = new WholeNumberPolicy(1, 1, ONE_SECOND);
    TokenBucket<String, Long> bucket = new TokenBucket<>(1, input -> key, (selected, input) -> input, selected -> policy, () -> {
      if (Thread.currentThread().getName().equals("quorum-bucket-reclaim-test") && (firstRead.getAndIncrement() == 0)) {
        reading.countDown();
        try {
          if (!proceed.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out waiting for reclamation");
          }
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();

          throw new AssertionError(interrupted);
        }
      }

      return 0;
    });

    Assert.assertTrue(bucket.allowed(0L));
    try (ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("quorum-bucket-reclaim-test").daemon().factory())) {
      Future<Boolean> pending = executor.submit(() -> bucket.allowed(1L));

      try {
        Assert.assertTrue(reading.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(bucket.removeIfFull(key));
        Assert.assertTrue(bucket.allowed(1L));
      } finally {
        proceed.countDown();
      }
      Assert.assertFalse(pending.get(5, TimeUnit.SECONDS));
    }
  }

  public void testBooleanDenialLeavesAllSharedQuotasUnspent () {

    BucketPolicy perTenant = new WholeNumberPolicy(2, 0, ONE_SECOND);
    TokenBucket<String, InherentBucketKey<String>> bucket = new TokenBucket<>(3, key -> key, (key, input) -> 1, key -> ONE_TOKEN,
      prefix -> prefix.segments().size() == 1 ? perTenant : null);
    InherentBucketKey<String> first = InherentBucketKey.of("tenant", "first");
    InherentBucketKey<String> second = InherentBucketKey.of("tenant", "second");

    Assert.assertTrue(bucket.allowed(first));
    Assert.assertFalse(bucket.allowed(first));
    Assert.assertTrue(bucket.allowed(second));
    Assert.assertEquals(bucket.tryConsume(second), new BucketDecision(Outcome.RATE_LIMITED, 0, -1));
  }

  public void testReclaimingAChildRetainsItsSharedQuota () {

    AtomicLong clock = new AtomicLong();
    InherentBucketKey<String> tenant = InherentBucketKey.of("tenant");
    InherentBucketKey<String> principal = InherentBucketKey.of("tenant", "principal");
    BucketPolicy perPrincipal = new WholeNumberPolicy(1, 1, Stint.of(1, TimeUnit.NANOSECONDS));
    BucketPolicy perTenant = new WholeNumberPolicy(1, 1, Stint.of(10, TimeUnit.NANOSECONDS));
    TokenBucket<String, InherentBucketKey<String>> bucket = new TokenBucket<>(2, key -> key, (key, input) -> 1, key -> perPrincipal,
      key -> key.equals(tenant) ? perTenant : null, clock::get);

    Assert.assertTrue(bucket.allowed(principal));
    clock.set(1);
    Assert.assertTrue(bucket.removeIfFull(principal));
    Assert.assertFalse(bucket.removeIfFull(tenant));
    Assert.assertEquals(bucket.tryConsume(principal), new BucketDecision(Outcome.RATE_LIMITED, 0, 9));
    Assert.assertTrue(bucket.removeIfFull(principal));
    clock.set(10);
    Assert.assertTrue(bucket.removeIfFull(tenant));
    Assert.assertTrue(bucket.allowed(principal));
    clock.set(11);
    Assert.assertTrue(bucket.removeIfFull(principal));
    Assert.assertFalse(bucket.allowed(tenant));
    Assert.assertFalse(bucket.removeIfFull(tenant));
    clock.set(20);
    Assert.assertTrue(bucket.removeIfFull(tenant));
  }

  private record Request (InherentBucketKey<String> path, long cost) {

  }
}
