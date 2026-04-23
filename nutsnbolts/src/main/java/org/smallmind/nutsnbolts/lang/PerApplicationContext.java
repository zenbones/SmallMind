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
package org.smallmind.nutsnbolts.lang;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

/**
 * Manages application-scoped data attached to threads via an {@link InheritableThreadLocal} map,
 * keyed by {@link PerApplicationDataManager} implementation class, so child threads automatically
 * inherit their parent's application context.
 */
public class PerApplicationContext {

  private static final InheritableThreadLocal<ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object>> PER_APPLICATION_MAP_LOCAL = new InheritableThreadLocal<>();

  private ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;

  /**
   * Constructs a context, reusing any map already attached to the current thread or creating and
   * attaching a new one if none exists.
   */
  public PerApplicationContext () {

    if ((perApplicationMap = PER_APPLICATION_MAP_LOCAL.get()) == null) {
      PER_APPLICATION_MAP_LOCAL.set(perApplicationMap = new ConcurrentHashMap<>());
    }
  }

  /**
   * Stores a data object under the given manager type in the current thread's per-application map.
   *
   * @param clazz the manager class used as the map key
   * @param data  the data object to associate with that manager
   * @throws MissingPerApplicationContextException if no per-application context has been initialized on the current thread
   */
  public static void setPerApplicationData (Class<? extends PerApplicationDataManager> clazz, Object data) {

    ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;

    if ((perApplicationMap = PER_APPLICATION_MAP_LOCAL.get()) == null) {
      throw new MissingPerApplicationContextException("No per-application context has been set in this thread environment");
    } else {

      perApplicationMap.put(clazz, data);
    }
  }

  /**
   * Retrieves the data object stored under the given manager type from the current thread's
   * per-application map, cast to the requested type.
   *
   * @param clazz the manager class used as the map key
   * @param type  the class to which the stored value will be cast
   * @param <K>   the expected return type
   * @return the data associated with the manager, or {@code null} if no value has been stored
   * @throws MissingPerApplicationContextException if no per-application context has been initialized on the current thread
   * @throws ClassCastException                    if the stored value cannot be cast to {@code type}
   */
  public static <K> K getPerApplicationData (Class<? extends PerApplicationDataManager> clazz, Class<K> type) {

    ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;

    if ((perApplicationMap = PER_APPLICATION_MAP_LOCAL.get()) == null) {
      throw new MissingPerApplicationContextException("No per-application context has been set in this thread environment");
    } else {

      return type.cast(perApplicationMap.get(clazz));
    }
  }

  /**
   * Captures the current thread's per-application map into a {@link ContextCarrier} that can be
   * used to propagate the same context to another thread.
   *
   * @return a carrier holding the current per-application map, which may be {@code null} if no context is attached
   */
  public static ContextCarrier generateCarrier () {

    return new ContextCarrier(PER_APPLICATION_MAP_LOCAL.get());
  }

  /**
   * Wraps the given {@link ThreadFactory} so that every thread it creates inherits the current
   * thread's per-application context map.
   *
   * @param threadFactory the delegate factory whose threads should receive the current context
   * @return a wrapping factory that propagates the per-application context to new threads
   */
  public static ThreadFactory wrapThreadFactory (ThreadFactory threadFactory) {

    return new WrappingThreadFactory(PER_APPLICATION_MAP_LOCAL.get(), threadFactory);
  }

  /**
   * Installs this instance's per-application map onto the current thread so it is available for
   * lookup and inherited by any child threads.
   */
  public void prepareThread () {

    PER_APPLICATION_MAP_LOCAL.set(perApplicationMap);
  }

  /**
   * Immutable snapshot of a per-application context map that can be applied to another thread.
   */
  public static class ContextCarrier {

    private final ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;

    private ContextCarrier (ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap) {

      this.perApplicationMap = perApplicationMap;
    }

    /**
     * Installs the captured per-application map onto the current thread.
     */
    public void prepareThread () {

      PER_APPLICATION_MAP_LOCAL.set(perApplicationMap);
    }
  }

  /**
   * {@link ThreadFactory} decorator that installs a captured per-application context map onto
   * each thread before its task begins executing.
   */
  public static class WrappingThreadFactory implements ThreadFactory {

    private final ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;
    private final ThreadFactory threadFactory;

    /**
     * Constructs a wrapping factory bound to the given context map and delegate factory.
     *
     * @param perApplicationMap the context map to install on each new thread
     * @param threadFactory     the delegate factory used to construct the threads
     */
    public WrappingThreadFactory (ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap, ThreadFactory threadFactory) {

      this.perApplicationMap = perApplicationMap;
      this.threadFactory = threadFactory;
    }

    /**
     * Creates a new thread via the delegate factory that installs the captured per-application
     * context map before executing the given task.
     *
     * @param runnable the task the new thread will run
     * @return a thread that applies the per-application context before running {@code runnable}
     */
    @Override
    public Thread newThread (Runnable runnable) {

      return threadFactory.newThread(() -> {
        PER_APPLICATION_MAP_LOCAL.set(perApplicationMap);
        runnable.run();
      });
    }
  }
}
