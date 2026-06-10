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
package org.smallmind.quorum.pool.complex;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.smallmind.quorum.pool.complex.event.ComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.event.ErrorReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.event.LeaseTimeReportingComponentPoolEvent;

/**
 * Shared test fakes for the complex-pool concurrency tests. Not a test class itself — it carries no
 * {@code @Test} methods, so TestNG ignores it.
 */
final class PoolComponentSupport {

  private PoolComponentSupport () {

  }

  /**
   * A {@link ComponentInstance} whose validity is fixed at construction and which records whether
   * the pool has closed it.
   */
  static class StringInstance implements ComponentInstance<String> {

    private final String value;
    private final boolean valid;
    private volatile boolean closed = false;

    StringInstance (String value, boolean valid) {

      this.value = value;
      this.valid = valid;
    }

    boolean isClosed () {

      return closed;
    }

    @Override
    public boolean validate () {

      return valid;
    }

    @Override
    public String serve () {

      return value;
    }

    @Override
    public void close () {

      closed = true;
    }

    @Override
    public StackTraceElement[] getExistentialStackTrace () {

      return null;
    }
  }

  /**
   * Factory that hands out {@link StringInstance}s named {@code c0, c1, ...}, optionally delaying or
   * failing creation, and optionally marking the first instances invalid. Every instance it produces
   * is retained so tests can inspect close state after the fact.
   */
  static class InstanceFactory extends AbstractComponentInstanceFactory<String> {

    private final CopyOnWriteArrayList<StringInstance> instances = new CopyOnWriteArrayList<>();
    private final boolean[] validities;
    private final long createDelayMillis;
    private final boolean throwOnCreate;

    InstanceFactory (boolean... validities) {

      this(0L, false, validities);
    }

    InstanceFactory (long createDelayMillis, boolean throwOnCreate, boolean... validities) {

      this.createDelayMillis = createDelayMillis;
      this.throwOnCreate = throwOnCreate;
      this.validities = validities;
    }

    int created () {

      return instances.size();
    }

    StringInstance instance (int index) {

      return instances.get(index);
    }

    @Override
    public ComponentInstance<String> createInstance (ComponentPool<String> componentPool)
      throws Exception {

      if (throwOnCreate) {
        throw new IllegalStateException("forced creation failure");
      }
      if (createDelayMillis > 0) {
        Thread.sleep(createDelayMillis);
      }

      int index = instances.size();
      StringInstance instance = new StringInstance("c" + index, (index >= validities.length) || validities[index]);

      instances.add(instance);

      return instance;
    }
  }

  /**
   * Listener that records every event it receives so tests can assert on fan-out.
   */
  static class RecordingListener implements ComponentPoolEventListener {

    private final List<ErrorReportingComponentPoolEvent<?>> errorEvents = Collections.synchronizedList(new LinkedList<>());
    private final List<LeaseTimeReportingComponentPoolEvent<?>> leaseEvents = Collections.synchronizedList(new LinkedList<>());

    List<ErrorReportingComponentPoolEvent<?>> getErrorEvents () {

      return errorEvents;
    }

    List<LeaseTimeReportingComponentPoolEvent<?>> getLeaseEvents () {

      return leaseEvents;
    }

    @Override
    public void reportErrorOccurred (ErrorReportingComponentPoolEvent<?> event) {

      errorEvents.add(event);
    }

    @Override
    public void reportLeaseTime (LeaseTimeReportingComponentPoolEvent<?> event) {

      leaseEvents.add(event);
    }
  }
}
