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
package org.smallmind.nutsnbolts.scope;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;

/**
 * Owns a per-application map of data objects, keyed by {@link PerApplicationDataManager} implementation class, and binds that map to the current thread with a {@link ScopedValue} for the dynamic extent of an operation.
 * <p>
 * The map itself is mutable and shared: data stored during one {@link #run} or {@link #call} is visible in every later or concurrent entry on the same instance and to every {@link ApplicationCarrier} taken from it. Only the binding is scoped. Constructing an instance binds nothing; unlike {@code org.smallmind.nutsnbolts.context.PerApplicationContext}, there is no way to install the map on a thread and leave it there, and threads started from inside a bound operation do not inherit the binding. Use {@link #generateCarrier()}, {@link #wrapThreadFactory(ThreadFactory)}, or {@link #wrapExecutor(Executor)} to carry the binding onto other threads explicitly, or, when the scope instance itself is in hand, {@link #boundThreadFactory(ThreadFactory)} and {@link #boundExecutor(Executor)} to bind it directly.
 * <p>
 * The bound instance itself is visible through the static {@link #getBound()}, and {@link #resolve(PerApplicationScope)} applies the one rule every consumer that accepts a scope follows: a scope handed in is used, a {@code null} adopts the scope bound on the current thread, and a handed scope that differs from the bound one is rejected.
 */
public class PerApplicationScope {

  private static final ScopedValue<PerApplicationScope> PER_APPLICATION_SCOPE = ScopedValue.newInstance();

  private final ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap = new ConcurrentHashMap<>();

  /**
   * Constructs a scope with an empty per-application map. Nothing is bound to the current thread.
   */
  public PerApplicationScope () {

  }

  /**
   * Reports whether a per-application scope is bound on the current thread.
   *
   * @return {@code true} if a scope is bound, {@code false} otherwise
   */
  public static boolean isBound () {

    return PER_APPLICATION_SCOPE.isBound();
  }

  /**
   * Returns the scope bound on the current thread, which is the instance whose {@link #run} / {@link #call}, or a
   * carrier or factory taken from it, the thread is currently inside.
   *
   * @return the bound scope, or {@code null} if no scope is bound
   */
  public static PerApplicationScope getBound () {

    return PER_APPLICATION_SCOPE.isBound() ? PER_APPLICATION_SCOPE.get() : null;
  }

  /**
   * Resolves the scope a consumer should run inside, given the scope it was handed. A handed scope is returned as-is
   * when no scope is bound on the current thread, or when the bound scope is that same instance. A handed scope that
   * differs from the bound one is rejected, because the registrations made while the bound scope was in force could
   * never be seen through it. A {@code null} adopts the bound scope, or resolves to {@code null} when nothing is
   * bound, in which case the consumer runs with no scope. Call this once, from the thread that constructs or starts
   * the consumer, and keep the result; a later thread may carry a different binding or none.
   *
   * @param perApplicationScope the scope handed to the consumer, or {@code null}
   * @return the handed scope, else the bound scope, else {@code null}
   * @throws ScopeException if a scope was handed in and a different scope is bound on the current thread
   */
  public static PerApplicationScope resolve (PerApplicationScope perApplicationScope) {

    PerApplicationScope boundPerApplicationScope = getBound();

    if (perApplicationScope == null) {

      return boundPerApplicationScope;
    } else if ((boundPerApplicationScope != null) && (boundPerApplicationScope != perApplicationScope)) {
      throw new ScopeException("The supplied PerApplicationScope is not the scope bound on the current thread; registrations made under the bound scope could not be resolved through it");
    }

    return perApplicationScope;
  }

  /**
   * Stores a data object under the given manager type in the per-application map bound to the current thread.
   *
   * @param clazz the manager class used as the map key
   * @param data  the data object to associate with that manager; must not be {@code null}
   * @throws MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   */
  public static void setPerApplicationData (Class<? extends PerApplicationDataManager> clazz, Object data) {

    requireBoundMap().put(clazz, data);
  }

  /**
   * Stores a data object under the given manager type unless one is already stored there, and returns whichever
   * object the map holds afterwards. This is the atomic form of a get-then-set, for registries that create their
   * backing map on first use and may be written from more than one thread.
   *
   * @param clazz the manager class used as the map key
   * @param data  the data object to store if the key is absent; must not be {@code null}
   * @param <K>   the type of the data object, which every writer of the key is expected to share
   * @return the object already stored under the key, or {@code data} if there was none; the cast to {@code K} is
   * unchecked, so a value of another type stored under the key surfaces as a {@code ClassCastException} at the
   * caller's assignment rather than here
   * @throws MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   */
  public static <K> K setPerApplicationDataIfAbsent (Class<? extends PerApplicationDataManager> clazz, K data) {

    Object existing = requireBoundMap().putIfAbsent(clazz, data);

    return (existing == null) ? data : (K)existing;
  }

  /**
   * Retrieves the data object stored under the given manager type from the per-application map bound to the current thread, cast to the requested type.
   *
   * @param clazz the manager class used as the map key
   * @param type  the class to which the stored value will be cast
   * @param <K>   the expected return type
   * @return the data associated with the manager, or {@code null} if no value has been stored
   * @throws MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   * @throws ClassCastException                  if the stored value cannot be cast to {@code type}
   */
  public static <K> K getPerApplicationData (Class<? extends PerApplicationDataManager> clazz, Class<K> type) {

    return type.cast(requireBoundMap().get(clazz));
  }

  /**
   * Captures the per-application map bound to the current thread so it can be rebound, for the extent of an operation, on another thread.
   *
   * @return a carrier for the currently bound map; never {@code null}
   * @throws MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   */
  public static ApplicationCarrier generateCarrier () {

    return new ApplicationCarrier(ScopedValue.where(PER_APPLICATION_SCOPE, requireBound()));
  }

  /**
   * Wraps the given {@link ThreadFactory} so that every thread it creates runs its entire body with the per-application map bound to the current thread.
   *
   * @param threadFactory the delegate factory whose threads should receive the current binding
   * @return a wrapping factory that binds the per-application map on each new thread
   * @throws MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   */
  public static ThreadFactory wrapThreadFactory (ThreadFactory threadFactory) {

    return new WrappingThreadFactory(generateCarrier(), threadFactory);
  }

  /**
   * Wraps the given {@link Executor} so that every task handed to it runs with the per-application map that is bound on the submitting thread at the moment of submission. The carrier is taken inside {@code execute}, per task, so a pool built outside any binding still carries the scope of whichever bound thread submits to it. Nothing is captured when the executor is wrapped.
   *
   * @param executor the delegate executor that actually runs the tasks
   * @return a wrapping executor that rebinds the submitting thread's per-application map around each task
   * @throws MissingPerApplicationScopeException from {@code execute}, if the submitting thread has no per-application scope bound
   */
  public static Executor wrapExecutor (Executor executor) {

    return new CarrierExecutor(executor);
  }

  private static PerApplicationScope requireBound () {

    if (!PER_APPLICATION_SCOPE.isBound()) {
      throw new MissingPerApplicationScopeException("No per-application scope is bound in this thread environment");
    }

    return PER_APPLICATION_SCOPE.get();
  }

  private static ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> requireBoundMap () {

    return requireBound().perApplicationMap;
  }

  /**
   * Reports whether this instance's map is the one bound on the current thread, as opposed to no scope or a different
   * instance's scope.
   *
   * @return {@code true} if the current thread is inside this scope's {@code run} / {@code call} (or a carrier or
   * factory taken from it), {@code false} otherwise
   */
  public boolean isBoundOnCurrentThread () {

    return PER_APPLICATION_SCOPE.isBound() && (PER_APPLICATION_SCOPE.get() == this);
  }

  /**
   * Runs the operation with this scope's per-application map bound to the current thread for the dynamic extent of the operation.
   *
   * @param runnable operation to run while the map is bound
   */
  public void run (Runnable runnable) {

    ScopedValue.where(PER_APPLICATION_SCOPE, this).run(runnable);
  }

  /**
   * Calls the operation with this scope's per-application map bound to the current thread for the dynamic extent of the operation and returns its result.
   *
   * @param op  operation to call while the map is bound
   * @param <T> result type
   * @param <X> exception type thrown by the operation
   * @return the result of the operation
   * @throws X if the operation throws
   */
  public <T, X extends Throwable> T call (ScopedValue.CallableOp<? extends T, X> op)
    throws X {

    return ScopedValue.where(PER_APPLICATION_SCOPE, this).call(op);
  }

  /**
   * Wraps the given {@link ThreadFactory} so that every thread it creates runs its entire body with this scope's per-application map bound. Nothing is captured from the calling thread and no binding needs to exist when this method is called, which is what distinguishes it from the static {@link #wrapThreadFactory(ThreadFactory)}.
   *
   * @param threadFactory the delegate factory whose threads should run inside this scope
   * @return a wrapping factory that binds this scope on each new thread
   */
  public ThreadFactory boundThreadFactory (ThreadFactory threadFactory) {

    return new BoundThreadFactory(this, threadFactory);
  }

  /**
   * Wraps the given {@link Executor} so that every task handed to it runs with this scope's per-application map bound. Nothing is captured from the submitting thread and no binding needs to exist at submission, which is what distinguishes it from the static {@link #wrapExecutor(Executor)}.
   *
   * @param executor the delegate executor that actually runs the tasks
   * @return a wrapping executor that binds this scope around each task
   */
  public Executor boundExecutor (Executor executor) {

    return new BoundExecutor(this, executor);
  }

  /**
   * A captured per-application binding that can be applied, for the extent of an operation, on any thread. The carrier refers to the same shared map as the scope it was taken from, so data stored after the capture is still visible through it. A carrier may be used any number of times.
   */
  public static final class ApplicationCarrier {

    private final ScopedValue.Carrier carrier;

    private ApplicationCarrier (ScopedValue.Carrier carrier) {

      this.carrier = carrier;
    }

    /**
     * Runs the operation with the captured per-application map bound for its dynamic extent.
     *
     * @param runnable operation to run
     */
    public void run (Runnable runnable) {

      carrier.run(runnable);
    }

    /**
     * Calls the operation with the captured per-application map bound for its dynamic extent and returns its result.
     *
     * @param op  operation to call
     * @param <T> result type
     * @param <X> exception type thrown by the operation
     * @return the result of the operation
     * @throws X if the operation throws
     */
    public <T, X extends Throwable> T call (ScopedValue.CallableOp<? extends T, X> op)
      throws X {

      return carrier.call(op);
    }
  }

  private record WrappingThreadFactory(ApplicationCarrier applicationCarrier, ThreadFactory threadFactory) implements ThreadFactory {

    @Override
    public Thread newThread (Runnable runnable) {

      return threadFactory.newThread(() -> applicationCarrier.run(runnable));
    }
  }

  private record CarrierExecutor(Executor executor) implements Executor {

    @Override
    public void execute (Runnable runnable) {

      ApplicationCarrier applicationCarrier = generateCarrier();

      executor.execute(() -> applicationCarrier.run(runnable));
    }
  }

  private record BoundThreadFactory(PerApplicationScope perApplicationScope, ThreadFactory threadFactory) implements ThreadFactory {

    @Override
    public Thread newThread (Runnable runnable) {

      return threadFactory.newThread(() -> perApplicationScope.run(runnable));
    }
  }

  private record BoundExecutor(PerApplicationScope perApplicationScope, Executor executor) implements Executor {

    @Override
    public void execute (Runnable runnable) {

      executor.execute(() -> perApplicationScope.run(runnable));
    }
  }
}
