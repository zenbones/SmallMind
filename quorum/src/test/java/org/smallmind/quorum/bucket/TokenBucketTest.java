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

import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TokenBucketTest {

  // A refill rate of zero tokens keeps every test that uses it fully deterministic: elapsed wall-clock
  // time can never grant additional capacity, so the only budget available is the initial charge.
  private static final Stint ONE_SECOND = Stint.of(1, TimeUnit.SECONDS);

  public void testStartsFullyChargedAndExhaustsWithoutRefill () {

    TokenBucket<String> bucket = new TokenBucket<>(new FixedCostQuantifier(1.0d), new NullSelector(), 5.0d, 0.0d, ONE_SECOND);

    for (int index = 0; index < 5; index++) {
      Assert.assertTrue(bucket.allowed("request"), "the bucket should permit exactly its initial charge");
    }

    Assert.assertFalse(bucket.allowed("request"), "the bucket should deny once the charge is spent");
  }

  public void testInputCostingMoreThanCapacityIsNeverPermitted () {

    TokenBucket<String> bucket = new TokenBucket<>(new FixedCostQuantifier(6.0d), new NullSelector(), 5.0d, 0.0d, ONE_SECOND);

    Assert.assertFalse(bucket.allowed("request"));
    // The over-budget input must not have consumed anything, so a cheaper input remains affordable.
    Assert.assertTrue(new TokenBucket<String>(new FixedCostQuantifier(5.0d), new NullSelector(), 5.0d, 0.0d, ONE_SECOND).allowed("request"));
  }

  public void testZeroCostInputIsAlwaysPermitted () {

    TokenBucket<String> bucket = new TokenBucket<>(new FixedCostQuantifier(0.0d), new NullSelector(), 0.0d, 0.0d, ONE_SECOND);

    for (int index = 0; index < 3; index++) {
      Assert.assertTrue(bucket.allowed("request"), "a zero-cost input is affordable even against an empty bucket");
    }
  }

  public void testRefillAccruesOverTimeAndIsCappedAtLimit ()
    throws InterruptedException {

    // One token accrues every 200ms. The drain-then-immediate-retry pair relies only on the fact that
    // far less than 200ms elapses between two adjacent calls, and the post-sleep pair relies only on at
    // least one full token (but no more than the cap) being available after a 500ms wait.
    TokenBucket<String> bucket = new TokenBucket<>(new FixedCostQuantifier(1.0d), new NullSelector(), 1.0d, 1.0d, Stint.of(200, TimeUnit.MILLISECONDS));

    Assert.assertTrue(bucket.allowed("request"), "the bucket starts full");
    Assert.assertFalse(bucket.allowed("request"), "the charge is spent and no meaningful time has elapsed");

    Thread.sleep(500L);

    Assert.assertTrue(bucket.allowed("request"), "at least one token has accrued after the wait");
    Assert.assertFalse(bucket.allowed("request"), "accrual is clamped to the limit, so only a single token was banked");
  }

  public void testInputIsDeniedWhenItsChildBucketIsExhausted ()
    throws Exception {

    // The parent has ample budget; routing sends each input to a per-key child holding a single token.
    TokenBucket<String> parent = new TokenBucket<>(new FixedCostQuantifier(1.0d), new RoutingSelector(), 100.0d, 0.0d, ONE_SECOND);

    parent.add(new StringBucketKey("alpha"), new ChildBucketFactory(1.0d));
    parent.add(new StringBucketKey("beta"), new ChildBucketFactory(1.0d));

    Assert.assertTrue(parent.allowed("alpha"), "the alpha child has its single token");
    Assert.assertFalse(parent.allowed("alpha"), "the alpha child is now exhausted");
    // A different child is still funded, proving the parent itself was neither blocked nor charged by the
    // denied alpha request.
    Assert.assertTrue(parent.allowed("beta"), "the beta child is independent of alpha");
  }

  public void testNullSelectionBypassesAnyChildConstraint ()
    throws Exception {

    // The registered child can never permit anything (it holds zero tokens against a unit cost), but a
    // null selection skips the child lookup entirely.
    TokenBucket<String> parent = new TokenBucket<>(new FixedCostQuantifier(1.0d), new NullSelector(), 5.0d, 0.0d, ONE_SECOND);

    parent.add(new StringBucketKey("ignored"), new ChildBucketFactory(0.0d));

    Assert.assertTrue(parent.allowed("request"), "a null selection means only the parent budget applies");
  }

  public void testAddDoesNotReplaceAnExistingChild ()
    throws Exception {

    TokenBucket<String> parent = new TokenBucket<>(new FixedCostQuantifier(1.0d), new RoutingSelector(), 100.0d, 0.0d, ONE_SECOND);
    ChildBucketFactory factory = new ChildBucketFactory(1.0d);

    parent.add(new StringBucketKey("alpha"), factory);
    parent.add(new StringBucketKey("alpha"), factory);

    Assert.assertEquals(factory.getCreationCount(), 1, "a second add for an existing key must not construct another child");
  }

  private static class FixedCostQuantifier implements BucketQuantifier<String> {

    private final double cost;

    private FixedCostQuantifier (double cost) {

      this.cost = cost;
    }

    @Override
    public double quantity (String input) {

      return cost;
    }
  }

  private static class NullSelector implements BucketSelector<String> {

    @Override
    public BucketKey<String> selection (String input) {

      return null;
    }
  }

  private static class RoutingSelector implements BucketSelector<String> {

    @Override
    public BucketKey<String> selection (String input) {

      return new StringBucketKey(input);
    }
  }

  private static class StringBucketKey implements BucketKey<String> {

    private final String id;

    private StringBucketKey (String id) {

      this.id = id;
    }

    @Override
    public int hashCode () {

      return id.hashCode();
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof StringBucketKey) && ((StringBucketKey)obj).id.equals(id);
    }
  }

  private static class ChildBucketFactory implements BucketFactory<String> {

    private final double limit;
    private int creationCount = 0;

    private ChildBucketFactory (double limit) {

      this.limit = limit;
    }

    private int getCreationCount () {

      return creationCount;
    }

    @Override
    public TokenBucket<String> create () {

      creationCount++;

      return new TokenBucket<>(new FixedCostQuantifier(1.0d), new NullSelector(), limit, 0.0d, ONE_SECOND);
    }
  }
}
