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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

public class ContextFactory {

  private static final InheritableThreadLocal<Map<Class<? extends Context>, ContextStack>> CONTEXT_MAP_LOCAL = new InheritableThreadLocal<Map<Class<? extends Context>, ContextStack>>() {

    @Override
    protected Map<Class<? extends Context>, ContextStack> initialValue () {

      return new HashMap<>();
    }

    @Override
    protected Map<Class<? extends Context>, ContextStack> childValue (Map<Class<? extends Context>, ContextStack> parentValue) {

      return new HashMap<>(parentValue);
    }
  };

  public static <C extends Context> void importContextTrace (Class<C> contextClass, Context... contexts) {

    ContextStack<C> contextStack;

    if ((contexts != null) && (contexts.length > 0)) {

      if ((contextStack = CONTEXT_MAP_LOCAL.get().get(contextClass)) == null) {
        CONTEXT_MAP_LOCAL.get().put(contextClass, contextStack = new ContextStack<C>());
      }

      for (Context context : contexts) {
        contextStack.push(contextClass.cast(context));
      }
    }
  }

  public static <C extends Context> C[] exportContextTrace (Class<C> contextClass) {

    C[] contexts;
    C context;
    LinkedList<C> contextList;
    ContextStack<C> contextStack;

    contextList = new LinkedList<>();
    if ((contextStack = CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      while ((context = contextStack.pop()) != null) {
        contextList.addFirst(context);
      }
    }

    contexts = (C[])Array.newInstance(contextClass, contextList.size());
    contextList.toArray(contexts);

    return contexts;
  }

  public static <C extends Context> void clearContextTrace (Class<C> contextClass) {

    C context;
    ContextStack<C> contextStack;

    if ((contextStack = CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      do {
        context = contextStack.pop();
      } while (context != null);
    }
  }

  public static <C extends Context> boolean exists (Class<C> contextClass) {

    ContextStack<C> contextStack;

    return ((contextStack = CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) && (!contextStack.isEmpty());
  }

  public static <C extends Context> C getContext (Class<C> contextClass)
    throws ContextException {

    ContextStack<C> contextStack;
    C context;

    if (((contextStack = CONTEXT_MAP_LOCAL.get().get(contextClass)) == null) || ((context = contextStack.peek()) == null)) {

      return null;
    }

    return context;
  }

  public static Context[] getExpectedContexts (Method method)
    throws ContextException {

    Context[] contexts;
    ExpectedContexts expectedContexts;
    HashSet<Class<? extends Context>> requiredClasses = new HashSet<>();
    LinkedList<Context> contextList = new LinkedList<>();

    if ((expectedContexts = method.getAnnotation(ExpectedContexts.class)) != null) {
      requiredClasses.addAll(Arrays.asList(expectedContexts.value()));
    }

    for (Map.Entry<Class<? extends Context>, ContextStack> contextEntry : CONTEXT_MAP_LOCAL.get().entrySet()) {

      Context context;

      if ((context = contextEntry.getValue().peek()) != null) {
        requiredClasses.remove(contextEntry.getKey());
        contextList.add(context);
      }
    }

    if (!requiredClasses.isEmpty()) {
      throw new ContextException("Context(%s) has not been instantiated", requiredClasses.iterator().next());
    }

    contexts = new Context[contextList.size()];
    contextList.toArray(contexts);

    return contexts;
  }

  public static <C extends Context> void pushContext (C context) {

    ContextStack<C> contextStack;

    if ((contextStack = CONTEXT_MAP_LOCAL.get().get(context.getClass())) == null) {
      CONTEXT_MAP_LOCAL.get().put(context.getClass(), contextStack = new ContextStack<>());
    }

    contextStack.push(context);
  }

  public static Context popContext (Context context) {

    return popContext(context.getClass());
  }

  public static <C extends Context> C popContext (Class<C> contextClass) {

    ContextStack<C> contextStack;

    if ((contextStack = CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      return contextStack.pop();
    }

    return null;
  }
}