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
package org.smallmind.nutsnbolts.scope;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * Static utility that binds {@link Scope} instances, keyed by concrete class, for the dynamic extent of an operation using a {@link ScopedValue}.
 * <p>
 * Unlike a thread-local stack, a scope can not be pushed in one place and popped in another; it is bound only while the operation passed to {@link #callInScope} or {@link #runInScope} is executing, and is automatically unbound when that operation returns or throws. Nested entries for the same scope class shadow the outer binding until the inner operation completes. Bindings are not inherited by threads the operation starts; use {@link #snapshot()} to carry them across a thread boundary explicitly.
 */
public class ScopeFactory {

  private static final ScopedValue<Map<Class<? extends Scope>, Scope>> SCOPE_MAP = ScopedValue.newInstance();

  /**
   * Calls the operation with the scope bound, under its concrete class, for the dynamic extent of the operation.
   *
   * @param scope scope to bind; must not be {@code null}
   * @param op    operation to call while the scope is bound
   * @param <T>   result type
   * @param <X>   exception type thrown by the operation
   * @return the result of the operation
   * @throws X              if the operation throws
   * @throws ScopeException if the scope is {@code null}
   */
  public static <T, X extends Throwable> T callInScope (Scope scope, ScopedValue.CallableOp<? extends T, X> op)
    throws X {

    if (scope == null) {
      throw new ScopeException("A null scope can not be entered");
    }

    return callInScopes(new Scope[] {scope}, op);
  }

  /**
   * Calls the operation with every supplied scope bound, each under its concrete class, for the dynamic extent of the operation. Scopes are bound in array order, so when two scopes share a class the later one is visible. A {@code null} or empty array calls the operation without adding any binding.
   * <p>
   * For each scope implementing {@link LifecycleAware}, {@code beforeEnter} is called in array order before anything is bound, and {@code afterExit} is called in reverse order after the operation completes, whether normally or by throwing. If a {@code beforeEnter} call throws, {@code afterExit} is still called for the scopes already entered.
   *
   * @param scopes scopes to bind; elements must not be {@code null}
   * @param op     operation to call while the scopes are bound
   * @param <T>    result type
   * @param <X>    exception type thrown by the operation
   * @return the result of the operation
   * @throws X              if the operation throws
   * @throws ScopeException if any element of the array is {@code null}
   */
  public static <T, X extends Throwable> T callInScopes (Scope[] scopes, ScopedValue.CallableOp<? extends T, X> op)
    throws X {

    int enteredCount = 0;

    if ((scopes == null) || (scopes.length == 0)) {

      return op.call();
    }

    try {
      for (Scope scope : scopes) {
        if (scope == null) {
          throw new ScopeException("A null scope can not be entered");
        }
        if (scope instanceof LifecycleAware) {
          ((LifecycleAware)scope).beforeEnter();
        }
        enteredCount++;
      }

      return ScopedValue.where(SCOPE_MAP, bind(scopes)).call(op);
    } finally {
      for (int index = enteredCount - 1; index >= 0; index--) {
        if (scopes[index] instanceof LifecycleAware) {
          ((LifecycleAware)scopes[index]).afterExit();
        }
      }
    }
  }

  /**
   * Runs the operation with the scope bound, under its concrete class, for the dynamic extent of the operation.
   *
   * @param scope    scope to bind; must not be {@code null}
   * @param runnable operation to run while the scope is bound
   * @throws ScopeException if the scope is {@code null}
   */
  public static void runInScope (Scope scope, Runnable runnable) {

    ScopeFactory.<Void, RuntimeException>callInScope(scope, () -> {
      runnable.run();

      return null;
    });
  }

  /**
   * Runs the operation with every supplied scope bound for the dynamic extent of the operation, with the same ordering, lifecycle, and {@code null} handling rules as {@link #callInScopes}.
   *
   * @param scopes   scopes to bind; elements must not be {@code null}
   * @param runnable operation to run while the scopes are bound
   * @throws ScopeException if any element of the array is {@code null}
   */
  public static void runInScopes (Scope[] scopes, Runnable runnable) {

    ScopeFactory.<Void, RuntimeException>callInScopes(scopes, () -> {
      runnable.run();

      return null;
    });
  }

  /**
   * Returns the innermost bound scope of the given class on the current thread.
   *
   * @param scopeClass concrete scope class used as the binding key
   * @param <S>        scope type
   * @return the bound scope, or {@code null} if no scope of that class is bound
   */
  public static <S extends Scope> S getScope (Class<S> scopeClass) {

    return SCOPE_MAP.isBound() ? scopeClass.cast(SCOPE_MAP.get().get(scopeClass)) : null;
  }

  /**
   * Captures the scopes currently bound on this thread so they can be rebound, for the extent of an operation, on another thread. The capture is taken immediately; later entries or exits on this thread do not affect it.
   *
   * @return an immutable snapshot of the current bindings, possibly empty; never {@code null}
   */
  public static ScopeSnapshot snapshot () {

    return new ScopeSnapshot(ScopedValue.where(SCOPE_MAP, SCOPE_MAP.isBound() ? SCOPE_MAP.get() : Collections.emptyMap()));
  }

  /**
   * Collects the scopes bound on the current thread that are assignment-compatible with at least one of the supplied filter types, validating any {@link ExpectedScopes} declared on the method.
   *
   * @param method        method whose {@link ExpectedScopes} annotation is consulted for required scope types
   * @param filterClasses one or more scope supertypes; only scopes whose class is assignable to a filter class are included
   * @return array of matching bound scopes; never {@code null}
   * @throws ScopeException if a scope type declared in {@link ExpectedScopes} is not currently bound
   */
  public static Scope[] filterScopesOn (Method method, Class<?>... filterClasses)
    throws ScopeException {

    Scope[] scopes;
    ExpectedScopes expectedScopes;
    HashSet<Class<? extends Scope>> expectedClasses = new HashSet<>();
    LinkedList<Scope> scopeList = new LinkedList<>();

    if ((expectedScopes = method.getAnnotation(ExpectedScopes.class)) != null) {
      expectedClasses.addAll(Arrays.asList(expectedScopes.value()));
    }

    if (SCOPE_MAP.isBound()) {
      for (Map.Entry<Class<? extends Scope>, Scope> scopeEntry : SCOPE_MAP.get().entrySet()) {
        for (Class<?> filterClass : filterClasses) {
          if (filterClass.isAssignableFrom(scopeEntry.getKey())) {
            expectedClasses.remove(scopeEntry.getKey());
            scopeList.add(scopeEntry.getValue());
            break;
          }
        }
      }
    }

    if (!expectedClasses.isEmpty()) {
      throw new ScopeException("The scope expectations(%s) have not been satisfied", Arrays.toString(expectedClasses.toArray()));
    }

    scopes = new Scope[scopeList.size()];
    scopeList.toArray(scopes);

    return scopes;
  }

  private static Map<Class<? extends Scope>, Scope> bind (Scope[] scopes) {

    HashMap<Class<? extends Scope>, Scope> boundMap = SCOPE_MAP.isBound() ? new HashMap<>(SCOPE_MAP.get()) : new HashMap<>();

    for (Scope scope : scopes) {
      boundMap.put(scope.getClass(), scope);
    }

    return Collections.unmodifiableMap(boundMap);
  }
}
