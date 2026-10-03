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
import java.util.concurrent.ThreadFactory;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;

/**
 * Owns a per-application map of data objects, keyed by {@link PerApplicationDataManager} implementation class, and binds that map to the current thread with a {@link ScopedValue} for the dynamic extent of an operation.
 * <p>
 * The map itself is mutable and shared: data stored during one {@link #run} or {@link #call} is visible in every later or concurrent entry on the same instance and to every {@link ApplicationCarrier} taken from it. Only the binding is scoped. Constructing an instance binds nothing; unlike {@code org.smallmind.nutsnbolts.lang.PerApplicationContext}, there is no way to install the map on a thread and leave it there, and threads started from inside a bound operation do not inherit the binding. Use {@link #generateCarrier()} or {@link #wrapThreadFactory(ThreadFactory)} to carry the binding onto other threads explicitly.
 */
public class PerApplicationScope {

  private static final ScopedValue<ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object>> PER_APPLICATION_MAP = ScopedValue.newInstance();

  private final ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap = new ConcurrentHashMap<>();

  /**
   * Constructs a scope with an empty per-application map. Nothing is bound to the current thread.
   */
  public PerApplicationScope () {

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

    return new ApplicationCarrier(ScopedValue.where(PER_APPLICATION_MAP, requireBoundMap()));
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

  private static ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> requireBoundMap () {

    if (!PER_APPLICATION_MAP.isBound()) {
      throw new MissingPerApplicationScopeException("No per-application scope is bound in this thread environment");
    }

    return PER_APPLICATION_MAP.get();
  }

  /**
   * Runs the operation with this scope's per-application map bound to the current thread for the dynamic extent of the operation.
   *
   * @param runnable operation to run while the map is bound
   */
  public void run (Runnable runnable) {

    ScopedValue.where(PER_APPLICATION_MAP, perApplicationMap).run(runnable);
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

    return ScopedValue.where(PER_APPLICATION_MAP, perApplicationMap).call(op);
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

  private static class WrappingThreadFactory implements ThreadFactory {

    private final ApplicationCarrier applicationCarrier;
    private final ThreadFactory threadFactory;

    private WrappingThreadFactory (ApplicationCarrier applicationCarrier, ThreadFactory threadFactory) {

      this.applicationCarrier = applicationCarrier;
      this.threadFactory = threadFactory;
    }

    @Override
    public Thread newThread (Runnable runnable) {

      return threadFactory.newThread(() -> applicationCarrier.run(runnable));
    }
  }
}
