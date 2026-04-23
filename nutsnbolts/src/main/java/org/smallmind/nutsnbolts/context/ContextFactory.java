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
package org.smallmind.nutsnbolts.context;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * Static utility that maintains per-type, per-thread stacks of {@link Context} instances using an inheritable thread-local map, allowing child threads to inherit a snapshot of the parent's context.
 */
public class ContextFactory {

  private static final InheritableThreadLocal<Map<Class<? extends Context>, LinkedList<? extends Context>>> CONTEXT_MAP_LOCAL = new InheritableThreadLocal<>() {

    @Override
    protected Map<Class<? extends Context>, LinkedList<? extends Context>> initialValue () {

      return new HashMap<>();
    }

    @Override
    protected Map<Class<? extends Context>, LinkedList<? extends Context>> childValue (Map<Class<? extends Context>, LinkedList<? extends Context>> parentValue) {

      HashMap<Class<? extends Context>, LinkedList<? extends Context>> inheritedMap = new HashMap<>();

      for (Map.Entry<Class<? extends Context>, LinkedList<? extends Context>> parentEntry : parentValue.entrySet()) {
        inheritedMap.put(parentEntry.getKey(), new LinkedList<>(parentEntry.getValue()));
      }

      return inheritedMap;
    }
  };

  /**
   * Pushes each element of the supplied array onto the current thread's context stack for the given type, preserving the provided order.
   *
   * @param contextClass context class key identifying the stack to populate
   * @param contexts     contexts to push; ignored when {@code null} or empty
   * @param <C>          context type
   */
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

  /**
   * Removes all contexts for the given type from the current thread's stack and returns them as an array ordered from bottom to top (oldest first).
   *
   * @param contextClass context class key identifying the stack to drain
   * @param <C>          context type
   * @return array of contexts in bottom-to-top order; never {@code null}
   */
  public static <C extends Context> C[] exportContextTrace (Class<C> contextClass) {

    C[] contexts;
    C context;
    LinkedList<C> exportedList;
    LinkedList<C> contextStack;

    exportedList = new LinkedList<>();
    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      while (!contextStack.isEmpty()) {
        exportedList.addFirst(contextStack.pop());
      }
    }

    contexts = (C[])Array.newInstance(contextClass, exportedList.size());
    exportedList.toArray(contexts);

    return contexts;
  }

  /**
   * Clears the context stack for the given type on the current thread without returning the removed elements.
   *
   * @param contextClass context class key identifying the stack to clear
   * @param <C>          context type
   */
  public static <C extends Context> void clearContextTrace (Class<C> contextClass) {

    LinkedList<C> contextStack;

    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      contextStack.clear();
    }
  }

  /**
   * Returns {@code true} if at least one context of the given type is present on the current thread's stack.
   *
   * @param contextClass context class key to check
   * @param <C>          context type
   * @return {@code true} if a context of the specified type is available
   */
  public static <C extends Context> boolean exists (Class<C> contextClass) {

    LinkedList<C> contextStack;

    return ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) && (!contextStack.isEmpty());
  }

  /**
   * Returns the top context for the given type on the current thread's stack without removing it.
   *
   * @param contextClass context class key identifying the stack to peek
   * @param <C>          context type
   * @return the topmost context, or {@code null} if the stack is empty or not yet created
   * @throws ContextException if an unexpected error occurs during lookup
   */
  public static <C extends Context> C getContext (Class<C> contextClass)
    throws ContextException {

    LinkedList<C> contextStack;
    C context;

    if (((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) == null) || ((context = contextStack.peek()) == null)) {

      return null;
    }

    return context;
  }

  /**
   * Collects all currently active contexts on the current thread, validating any {@link ExpectedContexts} declared on the method.
   *
   * @param method method whose {@link ExpectedContexts} annotation is consulted
   * @return all active contexts matching the base {@link Context} type
   * @throws ContextException if any context type declared in {@link ExpectedContexts} is not present
   */
  public static Context[] filterContextsOn (Method method) {

    return filterContextsOn(method, Context.class);
  }

  /**
   * Collects all currently active contexts on the current thread that are assignment-compatible with at least one of the supplied filter types, validating any {@link ExpectedContexts} declared on the method.
   *
   * @param method        method whose {@link ExpectedContexts} annotation is consulted for required context types
   * @param filterClasses one or more context supertypes; only contexts whose class is assignable to a filter class are included
   * @return array of matching active contexts; never {@code null}
   * @throws ContextException if a context type declared in {@link ExpectedContexts} is not currently active
   */
  public static Context[] filterContextsOn (Method method, Class... filterClasses)
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

  /**
   * Pushes a context onto the stack for its concrete class on the current thread.
   *
   * @param context context instance to push; the stack key is {@code context.getClass()}
   * @param <C>     context type
   */
  public static <C extends Context> void pushContext (C context) {

    LinkedList<C> contextStack;

    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(context.getClass())) == null) {
      CONTEXT_MAP_LOCAL.get().put(context.getClass(), contextStack = new LinkedList<>());
    }

    contextStack.push(context);
  }

  /**
   * Removes and returns the topmost context for the given type from the current thread's stack.
   *
   * @param contextClass context class key identifying the stack to pop
   * @param <C>          context type
   * @return the removed context, or {@code null} if the stack is empty or absent
   */
  public static <C extends Context> C popContext (Class<C> contextClass) {

    LinkedList<C> contextStack;

    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      if (!contextStack.isEmpty()) {
        return contextStack.pop();
      }
    }

    return null;
  }

  /**
   * Removes the first occurrence of the given context instance from the stack for its concrete class on the current thread.
   *
   * @param context the specific context instance to remove
   * @return the removed context if it was found, or {@code null} if it was not present
   */
  public static Context removeContext (Context context) {

    LinkedList<? extends Context> contextStack;

    if ((contextStack = CONTEXT_MAP_LOCAL.get().get(context.getClass())) != null) {
      if (contextStack.remove(context)) {

        return context;
      }
    }

    return null;
  }

  /**
   * Returns the number of contexts currently on the stack for the given type on the current thread.
   *
   * @param contextClass context class key identifying the stack to measure
   * @param <C>          context type
   * @return stack depth, or {@code 0} if no stack exists for the type
   */
  public static <C extends Context> int sizeFor (Class<C> contextClass) {

    LinkedList<C> contextStack;

    if ((contextStack = (LinkedList<C>)CONTEXT_MAP_LOCAL.get().get(contextClass)) != null) {
      return contextStack.size();
    }

    return 0;
  }
}
