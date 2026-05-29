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
package org.smallmind.nutsnbolts.io;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Produces JDK dynamic proxies for {@link Closeable} types that notify a {@link CloseListener} after each close call.
 */
public class CloseableProxyFactory {

  /**
   * Returns a proxy that forwards all method calls to {@code instance} and invokes {@code listener} after each {@code close()}.
   *
   * @param clazz    the interface type to proxy; must be implemented by {@code instance}
   * @param instance the actual closeable to delegate calls to
   * @param listener the listener to notify after the close method returns
   * @param <C>      the closeable interface type
   * @return a dynamic proxy implementing {@code clazz}
   * @throws NoSuchMethodException if the {@code close} method cannot be resolved on {@code instance}
   */
  public static <C extends Closeable> C createProxy (Class<C> clazz, C instance, CloseListener listener) {

    return clazz.cast(Proxy.newProxyInstance(instance.getClass().getClassLoader(), new Class[] {clazz}, new CloseableInvocationHandler(instance, listener)));
  }

  private record CloseableInvocationHandler(Closeable closeable, CloseListener listener) implements InvocationHandler {

    /**
     * Constructs the handler, resolving the {@code close} method for later comparison.
     *
     * @param closeable the wrapped closeable instance
     * @param listener  the listener to invoke after close returns
     * @throws NoSuchMethodException if the {@code close} method cannot be found on {@code closeable}
     */
    private CloseableInvocationHandler {

    }

      /**
       * Delegates the method call to the wrapped instance and, if the called method is {@code close},
       * fires the registered {@link CloseListener}.
       *
       * @param proxy  the proxy instance
       * @param method the method that was invoked
       * @param args   the arguments passed to the method
       * @return the value returned by the underlying method
       * @throws Throwable if the underlying method throws
       */
      @Override
      public Object invoke (Object proxy, Method method, Object[] args)
        throws Throwable {

        Object value;

        value = method.invoke(closeable, args);

        if ("close".equals(method.getName()) && (method.getParameterCount() == 0) && method.getReturnType().equals(Void.TYPE)) {
          listener.postClose(new CloseEvent(closeable));
        }

        return value;
      }
    }
}
