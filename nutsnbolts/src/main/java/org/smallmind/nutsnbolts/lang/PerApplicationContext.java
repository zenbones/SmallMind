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
 * Holds application-scoped data in an {@link InheritableThreadLocal} map keyed by a
 * {@link PerApplicationDataManager} type. Allows threads to share application-level context
 * safely across child threads.
 */
public class PerApplicationContext {

  private static final InheritableThreadLocal<ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object>> PER_APPLICATION_MAP_LOCAL = new InheritableThreadLocal<>();

  private ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;

  /**
   * Initializes the context, creating the per-application map if one has not already been attached
   * to the current thread.
   */
  public PerApplicationContext () {

    if ((perApplicationMap = PER_APPLICATION_MAP_LOCAL.get()) == null) {
      PER_APPLICATION_MAP_LOCAL.set(perApplicationMap = new ConcurrentHashMap<>());
    }
  }

  /**
   * Stores per-application data under the manager type for the current thread.
   *
   * @param clazz the manager key
   * @param data  the data to associate with the manager
   * @throws MissingPerApplicationContextException if the context has not been initialized on this thread
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
   * Retrieves per-application data for the given manager type.
   *
   * @param clazz the manager key
   * @param type  the expected data type
   * @param <K>   the generic type of the returned object
   * @return the data associated with the manager, or {@code null} if none
   * @throws MissingPerApplicationContextException if the context has not been initialized on this thread
   * @throws ClassCastException                    if the stored value cannot be cast to the expected type
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
   * Captures the currently attached per-application context map for later reuse in another thread.
   *
   * @return a carrier containing the current thread's per-application map, or {@code null} map if none is attached
   */
  public static ContextCarrier generateCarrier () {

    return new ContextCarrier(PER_APPLICATION_MAP_LOCAL.get());
  }

  /**
   * Wraps a {@link ThreadFactory} so each created thread receives the current per-application context.
   *
   * @param threadFactory the delegate factory used to create threads
   * @return a factory that propagates the current per-application context map to produced threads
   */
  public static ThreadFactory wrapThreadFactory (ThreadFactory threadFactory) {

    return new WrappingThreadFactory(PER_APPLICATION_MAP_LOCAL.get(), threadFactory);
  }

  /**
   * Attaches the current per-application map to the thread, making it available to child threads.
   */
  public void prepareThread () {

    PER_APPLICATION_MAP_LOCAL.set(perApplicationMap);
  }

  /**
   * Captures and re-attaches a per-application context map to another thread.
   */
  public static class ContextCarrier {

    private final ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;

    private ContextCarrier (ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap) {

      this.perApplicationMap = perApplicationMap;
    }

    /**
     * Attaches the captured per-application map to the current thread.
     */
    public void prepareThread () {

      PER_APPLICATION_MAP_LOCAL.set(perApplicationMap);
    }
  }

  /**
   * {@link ThreadFactory} wrapper that propagates a captured per-application context map to each new thread.
   */
  public static class WrappingThreadFactory implements ThreadFactory {

    private final ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;
    private final ThreadFactory threadFactory;

    /**
     * Creates a thread factory wrapper bound to the provided per-application context map.
     *
     * @param perApplicationMap the context map to attach to threads created by the delegate
     * @param threadFactory     the delegate factory used to create threads
     */
    public WrappingThreadFactory (ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap, ThreadFactory threadFactory) {

      this.perApplicationMap = perApplicationMap;
      this.threadFactory = threadFactory;
    }

    /**
     * Creates a thread that attaches the captured per-application context before running the delegate task.
     *
     * @param runnable the task to execute
     * @return a new thread created by the delegate factory with context propagation applied
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
