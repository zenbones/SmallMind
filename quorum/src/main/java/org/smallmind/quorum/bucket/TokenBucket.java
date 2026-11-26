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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;
import org.smallmind.quorum.bucket.BucketDecision.Outcome;
import org.smallmind.quorum.bucket.WholeNumberPolicy.Attempt;
import org.smallmind.quorum.bucket.WholeNumberPolicy.BalanceState;
import org.smallmind.quorum.bucket.WholeNumberPolicy.Calculator;
import org.smallmind.quorum.bucket.WholeNumberPolicy.ContinuousBudget;
import org.smallmind.quorum.bucket.WholeNumberPolicy.FixedBudget;

/**
 * An in-memory tree of on-demand token buckets with optional shared quotas.
 *
 * @param <S> the path segment type for this tree
 * @param <T> the input type used to select a bucket and determine the cost
 */
public final class TokenBucket<S, T> {

  private enum UnlimitedTarget implements Target {

    INSTANCE
  }

  private static final BucketDecision KEY_REJECTED = new BucketDecision(Outcome.KEY_REJECTED, -1, -1);
  private static final BucketDecision NODE_LIMIT_REACHED = new BucketDecision(Outcome.NODE_LIMIT_REACHED, -1, -1);
  private static final Resolution UNRESOLVED_KEY = new Resolution(null, KEY_REJECTED);
  private static final Resolution TREE_FULL = new Resolution(null, NODE_LIMIT_REACHED);
  private final Node<S> root;
  private final int maximumChildNodes;
  private final BucketSelector<S, ? super T> selector;
  private final BucketQuantifier<S, ? super T> quantifier;
  private final PolicyFactory<S> factory;
  private final PolicyFactory<S> sharedFactory;
  private final LongSupplier clock;
  private int childNodes;

  /**
   * Creates a standalone bucket with a fixed policy and input-dependent cost.
   */
  public TokenBucket (BucketPolicy policy, ToLongFunction<? super T> quantifier) {

    this(0, _ -> InherentBucketKey.root(), (_, input) -> quantifier.applyAsLong(input), _ -> policy);
    Objects.requireNonNull(policy, "policy");
    Objects.requireNonNull(quantifier, "quantifier");
  }

  /**
   * Creates a tree whose inputs charge only the selected bucket.
   */
  public TokenBucket (int maximumChildNodes, BucketSelector<S, ? super T> selector, BucketQuantifier<S, ? super T> quantifier, PolicyFactory<S> factory) {

    this(maximumChildNodes, selector, quantifier, factory, System::nanoTime);
  }

  /**
   * Creates a tree with optional quotas shared by all keys under a path prefix.
   *
   * @param maximumChildNodes maximum number of non-root nodes, including routing nodes
   * @param selector          resolves the input to a complete bucket key
   * @param quantifier        determines the token cost from the key and original input
   * @param factory           configures a target bucket when first selected
   * @param sharedFactory     configures each prefix, including the root, when its node
   *                          is created. An unlimited policy (or legacy null result)
   *                          leaves the prefix without a shared quota
   */
  public TokenBucket (int maximumChildNodes, BucketSelector<S, ? super T> selector, BucketQuantifier<S, ? super T> quantifier, PolicyFactory<S> factory, PolicyFactory<S> sharedFactory) {

    this(maximumChildNodes, selector, quantifier, factory, sharedFactory, System::nanoTime);
  }

  TokenBucket (int maximumChildNodes, BucketSelector<S, ? super T> selector, BucketQuantifier<S, ? super T> quantifier, PolicyFactory<S> factory, LongSupplier clock) {

    this(maximumChildNodes, selector, quantifier, factory, clock, null);
  }

  TokenBucket (int maximumChildNodes, BucketSelector<S, ? super T> selector, BucketQuantifier<S, ? super T> quantifier, PolicyFactory<S> factory, PolicyFactory<S> sharedFactory, LongSupplier clock) {

    this(maximumChildNodes, selector, quantifier, factory, clock, Objects.requireNonNull(sharedFactory, "sharedFactory"));
  }

  private TokenBucket (int maximumChildNodes, BucketSelector<S, ? super T> selector, BucketQuantifier<S, ? super T> quantifier, PolicyFactory<S> factory, LongSupplier clock, PolicyFactory<S> sharedFactory) {

    if (maximumChildNodes < 0) {
      throw new IllegalArgumentException("Maximum child nodes must be non-negative");
    }

    this.maximumChildNodes = maximumChildNodes;
    this.selector = Objects.requireNonNull(selector, "selector");
    this.quantifier = Objects.requireNonNull(quantifier, "quantifier");
    this.factory = Objects.requireNonNull(factory, "factory");
    this.sharedFactory = sharedFactory;
    this.clock = Objects.requireNonNull(clock, "clock");

    root = createNode(InherentBucketKey.root(), 0);
  }

  interface Target {

  }

  /**
   * Attempts to charge the input without waiting for tokens.
   *
   * @param input the original input to evaluate
   * @return whether the input was admitted
   * @throws IllegalArgumentException if the quantifier returns a negative cost
   */
  public boolean allowed (T input) {

    return consume(input, false).allowed();
  }

  /**
   * Attempts to charge the input and reports the decision, balance, and retry delay
   * from that same attempt.
   *
   * @param input the original input to evaluate
   * @return the admission decision
   * @throws IllegalArgumentException if the quantifier returns a negative cost
   */
  public BucketDecision tryConsume (T input) {

    return consume(input, true);
  }

  /**
   * Reclaims a childless non-root node. All limits owned by that node must
   * start full, be currently full, and have continuous refill or a fixed budget.
   * Its policies are resolved anew on reuse.
   *
   * @param key the path of the node to remove
   * @return whether a node was removed and a slot was reclaimed
   */
  public boolean removeIfFull (BucketKey<S, ?> key) {

    List<S> segments = key.segments();

    if (segments.isEmpty()) {
      return false;
    }

    synchronized (root) {
      Node<S> parent = root;

      for (int index = 0; index < segments.size() - 1; index++) {
        if ((parent = parent.getChild(segments.get(index))) == null) {
          return false;
        }
      }

      S segment = segments.getLast();
      Node<S> node = parent.getChild(segment);

      if ((node == null) || (node.children != null)) {
        return false;
      }
      if (!retire(node)) {
        return false;
      }

      parent.removeChild(segment);
      --childNodes;

      return true;
    }
  }

  private BucketDecision consume (T input, boolean detailed) {

    BucketKey<S, ?> key = selector.select(input);

    if (key == null) {
      return KEY_REJECTED;
    }

    long quantity = quantifier.quantity(key, input);

    if (quantity < 0) {
      throw new IllegalArgumentException("Token cost must be non-negative");
    }

    if (root.rejected) {
      return KEY_REJECTED;
    }

    Target target = findTarget(key);

    while (true) {
      if (target == null) {
        Resolution resolution = initialize(key);

        if (resolution.denial() != null) {
          return resolution.denial();
        }
        target = resolution.target();
      }

      BucketDecision decision;

      if (target == UnlimitedTarget.INSTANCE) {
        return BucketDecision.ALLOWED_WITHOUT_BALANCE;
      } else if (target instanceof FixedBudget fixed) {
        decision = fixed.consume(quantity, detailed);
      } else if (target instanceof ContinuousBudget continuous) {
        return continuous.consume(quantity, detailed, clock);
      } else {
        decision = consumeQuota((QuotaTarget)target, quantity, detailed);
      }

      if (decision != null) {
        return decision;
      }
      // Recheck under the tree lock if the previous target was retired.
      target = null;
    }
  }

  private Target findTarget (BucketKey<S, ?> key) {

    List<S> segments = key.segments();
    Node<S> node = root;

    for (S segment : segments) {
      if ((node = node.getChild(segment)) == null) {
        return null;
      }
    }

    return node.target;
  }

  private Resolution initialize (BucketKey<S, ?> key) {

    synchronized (root) {
      List<S> segments = key.segments();
      List<SharedBalance> shared = new ArrayList<>();
      Node<S> node = root;
      int firstMissing = 0;

      addShared(shared, node);
      for (; firstMissing < segments.size(); firstMissing++) {
        Node<S> child = node.getChild(segments.get(firstMissing));

        if (child == null) {
          break;
        }
        node = child;
        addShared(shared, node);
      }

      int missing = segments.size() - firstMissing;

      if ((missing == 0) && (node.target != null)) {
        return new Resolution(node.target, null);
      }
      if (missing > (maximumChildNodes - childNodes)) {
        return TREE_FULL;
      }

      BucketPolicy policy = Objects.requireNonNullElse(factory.createPolicy(key), RejectedPolicy.instance());

      if (policy == RejectedPolicy.instance()) {
        return UNRESOLVED_KEY;
      }

      Node<S> parent = node;
      Node<S> firstNew = null;

      for (int index = firstMissing; index < segments.size(); index++) {
        Node<S> child = createNode(key, index + 1);

        if (child.rejected) {
          return UNRESOLVED_KEY;
        }
        if (firstNew == null) {
          firstNew = child;
        } else {
          node.addChild(segments.get(index), child);
        }
        node = child;
        addShared(shared, node);
      }

      Target target = createTarget(policy, shared, node, segments.isEmpty());

      node.target = target;
      if (firstNew != null) {
        parent.addChild(segments.get(firstMissing), firstNew);
        childNodes += missing;
      }

      return new Resolution(target, null);
    }
  }

  private Target createTarget (BucketPolicy policy, List<SharedBalance> shared, Node<S> node, boolean rootTarget) {

    if (policy == UnlimitedPolicy.instance()) {
      return shared.isEmpty() ? UnlimitedTarget.INSTANCE : new QuotaTarget(null, List.copyOf(shared), node);
    }
    if (shared.isEmpty()) {
      Target optimized = WholeNumberPolicy.optimizedTarget(policy, rootTarget, clock);

      if (optimized != null) {
        return optimized;
      }
    }

    return new QuotaTarget(new Balance(policy, clock.getAsLong()), List.copyOf(shared), node);
  }

  private Node<S> createNode (BucketKey<S, ?> key, int length) {

    if (sharedFactory == null) {
      return new Node<>(null, false);
    }

    BucketKey<S, ?> prefix = (length == 0) ? InherentBucketKey.root() : key.ancestor(length);
    BucketPolicy policy = Objects.requireNonNullElse(sharedFactory.createPolicy(prefix), UnlimitedPolicy.instance());

    if (policy == RejectedPolicy.instance()) {
      return new Node<>(null, true);
    }

    return new Node<>((policy == UnlimitedPolicy.instance()) ? null : new SharedBalance(new Balance(policy, clock.getAsLong()), new ReentrantLock()), false);
  }

  private void addShared (List<SharedBalance> shared, Node<S> node) {

    if (node.shared != null) {
      shared.add(node.shared);
    }
  }

  private BucketDecision consumeQuota (QuotaTarget target, long quantity, boolean detailed) {

    if (!detailed && (quantity == 0)) {
      return (target.node().target == target) ? BucketDecision.ALLOWED_WITHOUT_BALANCE : null;
    }

    Attempt attempt = detailed ? new Attempt() : null;

    return target.shared().isEmpty() ? consumeTarget(target.balance(), quantity, attempt) : consumeShared(target, quantity, attempt);
  }

  private BucketDecision consumeTarget (Balance balance, long quantity, Attempt attempt) {

    BalanceState previous = balance.state().get();

    while (previous != null) {
      BalanceState next = balance.calculator().evaluate(previous, balance.calculator().refills() ? clock.getAsLong() : 0, quantity, attempt);

      if ((next == null) || (quantity == 0)) {
        return decision(next, quantity, attempt);
      }

      BalanceState observed = balance.state().compareAndExchange(previous, next);

      if (observed == previous) {
        return decision(next, quantity, attempt);
      }
      if (attempt != null) {
        attempt.reset();
      }
      previous = observed;
    }

    return null;
  }

  private BucketDecision consumeShared (QuotaTarget target, long quantity, Attempt attempt) {

    List<SharedBalance> shared = target.shared();

    target.lock();
    try {
      if (target.node().target != target) {
        return null;
      }

      long now = clock.getAsLong();
      Balance firstShared = shared.getFirst().balance();
      BalanceState firstUpdate = firstShared.calculator().evaluate(firstShared.state().get(), now, quantity, attempt);

      if ((attempt == null) && (firstUpdate == null)) {
        return BucketDecision.RATE_LIMITED_WITHOUT_DETAILS;
      }

      BalanceState[] otherUpdates = (shared.size() == 1) ? null : new BalanceState[shared.size() - 1];

      for (int index = 1; index < shared.size(); index++) {
        Balance sharedBalance = shared.get(index).balance();
        BalanceState update = sharedBalance.calculator().evaluate(sharedBalance.state().get(), now, quantity, attempt);

        if ((attempt == null) && (update == null)) {
          return BucketDecision.RATE_LIMITED_WITHOUT_DETAILS;
        }
        otherUpdates[index - 1] = update;
      }

      Balance balance = target.balance();
      BalanceState next = (balance == null) ? null : balance.calculator().evaluate(balance.state().get(), now, quantity, attempt);
      BucketDecision decision = (balance == null)
                                  ? ((attempt == null) ? BucketDecision.ALLOWED_WITHOUT_BALANCE : attempt.decision(quantity))
                                  : decision(next, quantity, attempt);

      if (decision.allowed() && (quantity != 0)) {
        firstShared.state().set(firstUpdate);
        for (int index = 1; index < shared.size(); index++) {
          assert otherUpdates != null;
          shared.get(index).balance().state().set(otherUpdates[index - 1]);
        }
        if (balance != null) {
          balance.state().set(next);
        }
      }

      return decision;
    } finally {
      target.unlock();
    }
  }

  private BucketDecision decision (BalanceState next, long quantity, Attempt attempt) {

    return (attempt == null)
             ? ((next == null) ? BucketDecision.RATE_LIMITED_WITHOUT_DETAILS : BucketDecision.ALLOWED_WITHOUT_BALANCE)
             : attempt.decision(quantity);
  }

  private boolean retire (Node<S> node) {

    Target target = node.target;
    if (target instanceof FixedBudget fixed) {
      return fixed.retire();
    }

    Balance shared = (node.shared == null) ? null : node.shared.balance();

    if ((target == null) || (target == UnlimitedTarget.INSTANCE)) {
      // Without a target that uses shared quotas or children, this balance has no live writers.
      return (shared == null) || shared.calculator().canRetire(shared.state().get(), clock.getAsLong());
    }
    QuotaTarget quota = (QuotaTarget)target;

    quota.lock();
    try {
      Balance balance = quota.balance();

      while (true) {
        BalanceState previous = (balance == null) ? null : balance.state().get();
        long now = clock.getAsLong();

        if (((balance != null) && !balance.calculator().canRetire(previous, now))
              || ((shared != null) && !shared.calculator().canRetire(shared.state().get(), now))) {
          return false;
        }
        if ((balance == null) || balance.state().compareAndSet(previous, null)) {
          node.target = null;
          return true;
        }
      }
    } finally {
      quota.unlock();
    }
  }

  private static class Node<S> {

    private final SharedBalance shared;
    private final boolean rejected;
    private volatile ConcurrentHashMap<S, Node<S>> children;
    private volatile Target target;

    private Node (SharedBalance shared, boolean rejected) {

      this.shared = shared;
      this.rejected = rejected;
    }

    private Node<S> getChild (S segment) {

      ConcurrentHashMap<S, Node<S>> map = children;

      return (map == null) ? null : map.get(segment);
    }

    // Called under the tree lock.
    private void addChild (S segment, Node<S> child) {

      if (children == null) {
        ConcurrentHashMap<S, Node<S>> map = new ConcurrentHashMap<>();

        map.put(segment, child);
        children = map;
      } else {
        children.put(segment, child);
      }
    }

    private void removeChild (S segment) {

      children.remove(segment);
      if (children.isEmpty()) {
        children = null;
      }
    }
  }

  private record Resolution(Target target, BucketDecision denial) {

  }

  private record SharedBalance(Balance balance, ReentrantLock lock) {

  }

  private record Balance(Calculator calculator, AtomicReference<BalanceState> state) {

    private Balance (BucketPolicy policy, long now) {

      this(new Calculator(policy), now);
    }

    private Balance (Calculator calculator, long now) {

      this(calculator, new AtomicReference<>(calculator.initialState(now)));
    }
  }

  private record QuotaTarget(Balance balance, List<SharedBalance> shared, Node<?> node) implements Target {

    private void lock () {

      // Every operation acquires shared quota locks in root-to-leaf order.
      for (SharedBalance entry : shared) {
        entry.lock().lock();
      }
    }

    private void unlock () {

      for (int index = shared.size() - 1; index >= 0; index--) {
        shared.get(index).lock().unlock();
      }
    }
  }
}
