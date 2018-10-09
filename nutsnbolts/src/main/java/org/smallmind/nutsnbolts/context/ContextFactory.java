/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.nutsnbolts.context;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

public class ContextFactory {

  private static final InheritableThreadLocal<Map<Class<? extends Context>, LinkedList<? extends Context>>> CONTEXT_MAP_LOCAL = new InheritableThreadLocal<Map<Class<? extends Context>, LinkedList<? extends Context>>>() {

    @Override
    protected Map<Class<? extends Context>, LinkedList<? extends Context>> initialValue () {

      return new HashMap<>();
    }

    @Override
    protected Map<Class<? extends Context>, LinkedList<? extends Context>> childValue (Map<Class<? extends Context>, LinkedList<? extends Context>> parentValue) {

      return new HashMap<>(parentValue);
    }
  };

  public static <C extends Context> void importContextTrace (Class<C> contextClass, C... contexts) {

    LinkedList<C> contextStack;

    if ((contexts != null) && (contexts.length > 0)) {

      if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) == null) {
        CONTEXT_MAP_LOCAL.get().put(contextClass, contextStack = new LinkedList<C>());
      }

      for (C context : contexts) {
        contextStack.push(contextClass.cast(context));
      }
    }
  }

  public static <C extends Context> C[] exportContextTrace (Class<C> contextClass) {

    C[] contexts;
    C context;
    LinkedList<C> exportedList;
    LinkedList<C> contextStack;

    exportedList = new LinkedList<>();
    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      while ((context = contextStack.pop()) != null) {
        exportedList.addFirst(context);
      }
    }

    contexts = (C[])Array.newInstance(contextClass, exportedList.size());
    exportedList.toArray(contexts);

    return contexts;
  }

  public static <C extends Context> void clearContextTrace (Class<C> contextClass) {

    C context;
    LinkedList<C> contextStack;

    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      do {
        context = contextStack.pop();
      } while (context != null);
    }
  }

  public static <C extends Context> boolean exists (Class<C> contextClass) {

    LinkedList<C> contextStack;

    return ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) && (!contextStack.isEmpty());
  }

  public static <C extends Context> C getContext (Class<C> contextClass)
    throws ContextException {

    LinkedList<C> contextStack;
    C context;

    if (((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) == null) || ((context = contextStack.peek()) == null)) {

      return null;
    }

    return context;
  }

  public static Context[] filterContextsOn (Method method) {

    return filterContextsOn(method, Context.class);
  }

  public static Context[] filterContextsOn (Method method, Class<? extends Context>... filterClasses)
    throws ContextException {

    Context[] contexts;
    ExpectedContexts expectedContexts;
    HashSet<Class<? extends Context>> expectedClasses = new HashSet<>();
    LinkedList<Context> contextList = new LinkedList<>();

    if ((expectedContexts = method.getAnnotation(ExpectedContexts.class)) != null) {
      expectedClasses.addAll(Arrays.asList(expectedContexts.value()));
    }

    for (Map.Entry<Class<? extends Context>, LinkedList<? extends Context>> contextEntry : CONTEXT_MAP_LOCAL.get().entrySet()) {

      Context context;

      for (Class<? extends Context> filterClass : filterClasses) {
        if (filterClass.isAssignableFrom(contextEntry.getKey())) {
          if ((context = contextEntry.getValue().peek()) != null) {
            expectedClasses.remove(contextEntry.getKey());
            contextList.add(context);
          }
          break;
        }
      }
    }

    if (!expectedClasses.isEmpty()) {
      throw new ContextException("The context expectations(%s) have not been satisfied", Arrays.toString(expectedClasses.toArray()));
    }

    contexts = new Context[contextList.size()];
    contextList.toArray(contexts);

    return contexts;
  }

  public static <C extends Context> void pushContext (C context) {

    LinkedList<C> contextStack;

    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(context.getClass())) == null) {
      CONTEXT_MAP_LOCAL.get().put(context.getClass(), contextStack = new LinkedList<>());
    }

    contextStack.push(context);
  }

  public static <C extends Context> C popContext (Class<C> contextClass) {

    LinkedList<C> contextStack;

    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      return contextStack.pop();
    }

    return null;
  }

  public static Context removeContext (Context context) {

    LinkedList<? extends Context> contextStack;

    if ((contextStack = CONTEXT_MAP_LOCAL.get().get(context.getClass())) != null) {
      if (contextStack.remove(context)) {

        return context;
      }
    }

    return null;
  }

  public static <C extends Context> int sizeFor (Class<C> contextClass) {

    LinkedList<C> contextStack;

    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      return contextStack.size();
    }

    return 0;
  }
}