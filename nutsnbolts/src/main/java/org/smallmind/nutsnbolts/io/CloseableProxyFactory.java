/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

public class CloseableProxyFactory {

  public static <C extends Closeable> C createProxy (Class<C> clazz, C instance, CloseListener listener)
    throws NoSuchMethodException {

    return clazz.cast(Proxy.newProxyInstance(instance.getClass().getClassLoader(), new Class[] {clazz}, new CloseableInvocationHandler(instance, listener)));
  }

  private static class CloseableInvocationHandler implements InvocationHandler {

    private final Method closeMethod;
    private final Closeable closeable;
    private final CloseListener listener;

    public CloseableInvocationHandler (Closeable closeable, CloseListener listener)
      throws NoSuchMethodException {

      this.closeable = closeable;
      this.listener = listener;

      closeMethod = closeable.getClass().getMethod("close");
    }

    @Override
    public Object invoke (Object proxy, Method method, Object[] args)
      throws Throwable {

      Object value;

      value = method.invoke(closeable, args);

      if (closeMethod.equals(method)) {
        listener.postClose(new CloseEvent(closeable));
      }

      return value;
    }
  }
}
