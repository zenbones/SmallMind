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
package org.smallmind.quorum.namespace.pool;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.NamingException;
import org.smallmind.quorum.namespace.PooledJavaContext;
import org.smallmind.quorum.namespace.event.JavaContextEvent;
import org.smallmind.quorum.namespace.event.JavaContextListener;
import org.smallmind.quorum.pool.complex.ComponentInstance;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * {@link ComponentInstance} adapter that links a {@link PooledJavaContext} to a
 * {@link ComponentPool} by listening for context lifecycle events.
 * <p>
 * When the caller logically closes the context (non-forced), the {@link PooledJavaContext} fires
 * a {@link org.smallmind.quorum.namespace.event.JavaContextEvent#contextClosed} event and this
 * instance returns itself to the pool via {@link ComponentPool#returnInstance}. When the context
 * detects a {@link javax.naming.CommunicationException} it fires an abort event and this instance
 * calls {@link ComponentPool#terminateInstance} followed by
 * {@link ComponentPool#reportErrorOccurred}.
 * <p>
 * {@link #close()} uses an {@link AtomicBoolean} guard to ensure the underlying context is
 * physically closed and the pool is notified at most once even under concurrent calls.
 */
public class JavaContextComponentInstance implements ComponentInstance<PooledJavaContext>, JavaContextListener {

  private final ComponentPool<PooledJavaContext> componentPool;
  private final PooledJavaContext pooledJavaContext;
  private final AtomicReference<StackTraceElement[]> stackTraceReference = new AtomicReference<StackTraceElement[]>();
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates the component instance and registers it as a listener on {@code pooledJavaContext}.
   *
   * @param componentPool     the pool that owns this instance and to which it reports lifecycle events
   * @param pooledJavaContext the {@link PooledJavaContext} to wrap and monitor
   * @throws NamingException if {@link PooledJavaContext#addJavaContextListener} throws
   */
  public JavaContextComponentInstance (ComponentPool<PooledJavaContext> componentPool, PooledJavaContext pooledJavaContext)
    throws NamingException {

    this.componentPool = componentPool;
    this.pooledJavaContext = pooledJavaContext;

    pooledJavaContext.addJavaContextListener(this);
  }

  /**
   * Returns the stack trace captured when this instance was last served, or {@code null} if
   * existential awareness is disabled or the instance has not yet been served.
   *
   * @return the stack trace from the most recent {@link #serve()} call, or {@code null}
   */
  public StackTraceElement[] getExistentialStackTrace () {

    return stackTraceReference.get();
  }

  /**
   * Validates this instance by performing a no-op lookup on the wrapped context.
   *
   * @return {@code true} if the backing context responds without throwing {@link NamingException};
   * {@code false} if the context is no longer usable
   */
  public boolean validate () {

    try {
      pooledJavaContext.lookup("");
    } catch (NamingException namingException) {

      return false;
    }

    return true;
  }

  /**
   * Called by the {@link PooledJavaContext} when the context is logically closed by the caller.
   * Returns this instance to the owning pool; any exception from the pool is logged and swallowed.
   *
   * @param javaContextEvent the event describing the normal close
   */
  public void contextClosed (JavaContextEvent javaContextEvent) {

    try {
      componentPool.returnInstance(this);
    } catch (Exception exception) {
      LoggerManager.getLogger(JavaContextComponentInstance.class).error(exception);
    }
  }

  /**
   * Called by the {@link PooledJavaContext} when a {@link javax.naming.CommunicationException} is
   * detected. Terminates this instance in the pool and reports the error; if the termination itself
   * throws, the communication exception is attached as the termination exception's cause before
   * both are reported.
   *
   * @param javaContextEvent the event carrying the originating {@link javax.naming.CommunicationException}
   */
  public void contextAborted (JavaContextEvent javaContextEvent) {

    Exception reportedException = javaContextEvent.getCommunicationException();

    try {
      componentPool.terminateInstance(this);
    } catch (Exception exception) {
      if ((reportedException != null) && (exception.getCause() == exception)) {
        exception.initCause(reportedException);
      }

      reportedException = exception;
    } finally {
      if (reportedException != null) {
        componentPool.reportErrorOccurred(reportedException);
        LoggerManager.getLogger(JavaContextComponentInstance.class).error(reportedException);
      }
    }
  }

  /**
   * Returns the wrapped {@link PooledJavaContext} to the caller.
   * <p>
   * If the pool has existential awareness enabled, the current thread's stack trace is captured
   * and stored so that long-running leases can be diagnosed.
   *
   * @return the {@link PooledJavaContext} managed by this instance
   * @throws Exception never thrown by this implementation
   */
  public PooledJavaContext serve ()
    throws Exception {

    if (componentPool.getComplexPoolConfig().isExistentiallyAware()) {
      stackTraceReference.set(Thread.currentThread().getStackTrace());
    }

    return pooledJavaContext;
  }

  /**
   * Forcibly terminates this instance in the pool and physically closes the wrapped context.
   * <p>
   * Guarded by an {@link AtomicBoolean} so that only the first call takes effect; subsequent
   * calls are silently ignored.
   *
   * @throws Exception if {@link ComponentPool#terminateInstance} or the forced context close throws
   */
  public void close ()
    throws Exception {

    if (closed.compareAndSet(false, true)) {
      componentPool.terminateInstance(this);

      if (pooledJavaContext != null) {
        pooledJavaContext.close(true);
      }
    }
  }
}
