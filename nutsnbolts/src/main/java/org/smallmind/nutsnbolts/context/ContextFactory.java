/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.context;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ContextFactory {

  private static final Map<Class<? extends Context>, ContextStackThreadLocal> CONTEXT_MAP = new HashMap<Class<? extends Context>, ContextStackThreadLocal>();

  public static <C extends Context> void importContextTrace (Class<C> contextClass, C... contexts) {

    ContextStackThreadLocal threadLocal;
    ContextStack contextStack;

    if ((contexts != null) && (contexts.length > 0)) {
      synchronized (CONTEXT_MAP) {
        if ((threadLocal = CONTEXT_MAP.get(contextClass)) == null) {
          CONTEXT_MAP.put(contextClass, threadLocal = new ContextStackThreadLocal());
        }
      }

      if ((contextStack = threadLocal.get()) == null) {
        threadLocal.set(contextStack = new ContextStack());
      }

      for (C context : contexts) {
        contextStack.push(context);
      }
    }
  }

  public static <C extends Context> C[] exportContextTrace (Class<C> contextClass) {

    C[] contexts;
    Context context;
    LinkedList<C> contextList;
    ContextStackThreadLocal threadLocal;
    ContextStack contextStack;

    synchronized (CONTEXT_MAP) {
      threadLocal = CONTEXT_MAP.get(contextClass);
    }

    contextList = new LinkedList<C>();

    if (threadLocal != null) {
      if ((contextStack = threadLocal.get()) != null) {
        while ((context = contextStack.pop()) != null) {
          contextList.addFirst(contextClass.cast(context));
        }
      }
    }

    contexts = (C[])Array.newInstance(contextClass, contextList.size());
    contextList.toArray(contexts);

    return contexts;
  }

  public static void clearContextTrace (Class<? extends Context> contextClass) {

    Context context;
    ContextStackThreadLocal threadLocal;
    ContextStack contextStack;

    synchronized (CONTEXT_MAP) {
      threadLocal = CONTEXT_MAP.get(contextClass);
    }

    if (threadLocal != null) {
      if ((contextStack = threadLocal.get()) != null) {
        do {
          context = contextStack.pop();
        } while (context != null);
      }
    }
  }

  public static boolean exists (Class<? extends Context> contextClass) {

    ContextStackThreadLocal threadLocal;
    ContextStack contextStack;

    synchronized (CONTEXT_MAP) {

      return ((threadLocal = CONTEXT_MAP.get(contextClass)) != null) && ((contextStack = threadLocal.get()) != null) && (!contextStack.isEmpty());
    }
  }

  public static <C extends Context> C getContext (Class<C> contextClass)
    throws ContextException {

    ContextStackThreadLocal threadLocal;
    ContextStack contextStack;
    C context;

    synchronized (CONTEXT_MAP) {
      if ((threadLocal = CONTEXT_MAP.get(contextClass)) == null) {

        return null;
      }
    }

    if (((contextStack = threadLocal.get()) == null) || ((context = contextClass.cast(contextStack.peek())) == null)) {

      return null;
    }

    return context;
  }

  public static Context[] getExpectedContexts (Method method)
    throws ContextException {

    ExpectedContexts expectedContexts;
    LinkedList<Context> contextList = new LinkedList<>();
    Context[] contexts;

    if ((expectedContexts = method.getAnnotation(ExpectedContexts.class)) != null) {
      for (ExpectedContext expectedContext : expectedContexts.value()) {

        Context context;

        if ((context = getContext(expectedContext.value())) == null) {
          if (expectedContext.required()) {
            throw new ContextException("Context(%s) has not been instantiated", expectedContext.value());
          }
        }
        else {
          contextList.add(context);
        }
      }
    }

    contexts = new Context[contextList.size()];
    contextList.toArray(contexts);

    return contexts;
  }

  public static void pushContext (Context context) {

    ContextStackThreadLocal threadLocal;
    ContextStack contextStack;

    synchronized (CONTEXT_MAP) {
      if ((threadLocal = CONTEXT_MAP.get(context.getClass())) == null) {
        CONTEXT_MAP.put(context.getClass(), threadLocal = new ContextStackThreadLocal());
      }
    }

    if ((contextStack = threadLocal.get()) == null) {
      threadLocal.set(contextStack = new ContextStack());
    }

    contextStack.push(context);
  }

  public static Context popContext (Context context) {

    return popContext(context.getClass());
  }

  public static <C extends Context> C popContext (Class<C> contextClass) {

    ContextStackThreadLocal threadLocal;
    ContextStack contextStack;

    synchronized (CONTEXT_MAP) {
      threadLocal = CONTEXT_MAP.get(contextClass);
    }

    if ((threadLocal != null) && ((contextStack = threadLocal.get()) != null)) {
      return contextClass.cast(contextStack.pop());
    }

    return null;
  }

  private static class ContextStackThreadLocal extends InheritableThreadLocal<ContextStack> {

    @Override
    protected ContextStack initialValue () {

      return new ContextStack();
    }

    @Override
    protected ContextStack childValue (ContextStack parentValue) {

      return new ContextStack(parentValue);
    }
  }
}