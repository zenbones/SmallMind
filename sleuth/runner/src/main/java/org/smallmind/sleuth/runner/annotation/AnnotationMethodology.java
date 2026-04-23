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
package org.smallmind.sleuth.runner.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.Pair;
import org.smallmind.sleuth.runner.Culprit;
import org.smallmind.sleuth.runner.SleuthRunner;
import org.smallmind.sleuth.runner.event.ErrorSleuthEvent;
import org.smallmind.sleuth.runner.event.SkippedSleuthEvent;
import org.smallmind.sleuth.runner.event.StartSleuthEvent;
import org.smallmind.sleuth.runner.event.SuccessSleuthEvent;

/**
 * Maintains an ordered, deduplicated collection of methods sharing a lifecycle annotation and
 * provides reflective invocation with integrated Sleuth event emission.
 * <p>
 * Methods are registered in hierarchy order (base class first) via {@link #add}. Duplicate
 * registrations — identified by method name and parameter signature — are silently ignored, so
 * overridden lifecycle methods appear only once. At invocation time each method is called in
 * registration order; if a prior culprit exists the method is skipped and a
 * {@link SkippedSleuthEvent} is emitted instead.
 *
 * @param <A> annotation type shared by all methods in this collection
 */
public class AnnotationMethodology<A extends Annotation> implements Iterable<Pair<Method, A>> {

  private final HashSet<MethodKey> methodSet = new HashSet<>();
  private final LinkedList<Pair<Method, A>> pairList = new LinkedList<>();

  /**
   * Adds a method/annotation pair to the collection if the method's signature has not already
   * been registered.
   * <p>
   * Deduplication is based on method name and declared parameter types, so an overriding method
   * in a subclass does not result in a second entry when the superclass version was added first.
   *
   * @param method     reflected method to register; must not be {@code null}
   * @param annotation annotation instance associated with the method; must not be {@code null}
   */
  public void add (Method method, A annotation) {

    if (methodSet.add(new MethodKey(method.getName(), method.getParameterTypes()))) {
      pairList.add(new Pair<>(method, annotation));
    }
  }

  /**
   * Invokes each registered method in order, emitting Sleuth events and updating the culprit.
   * <p>
   * For each method, a {@link StartSleuthEvent} is always emitted first. If {@code culprit} is
   * non-null the method is not called and a {@link SkippedSleuthEvent} is emitted. Otherwise
   * the method is invoked reflectively; a {@link SuccessSleuthEvent} is emitted on success or
   * an {@link ErrorSleuthEvent} on any exception, and the culprit is set accordingly. The updated
   * culprit — possibly set by an earlier method in this call — is returned.
   *
   * @param sleuthRunner runner used to dispatch events to registered listeners; must not be {@code null}
   * @param culprit      existing failure context that suppresses further invocations; {@code null} if none
   * @param clazz        declaring class used for event class-name fields; must not be {@code null}
   * @param instance     object instance on which to invoke the methods; must not be {@code null}
   * @return updated culprit after all methods have been processed; may be the original value or a
   * newly created one if a method threw
   */
  public Culprit invoke (SleuthRunner sleuthRunner, Culprit culprit, Class<?> clazz, Object instance) {

    for (Pair<Method, A> pair : pairList) {
      sleuthRunner.fire(new StartSleuthEvent(clazz.getName(), pair.first().getName()));

      if (culprit != null) {
        sleuthRunner.fire(new SkippedSleuthEvent(clazz.getName(), pair.first().getName(), 0, "Skipped due to prior error[" + culprit + "]"));
      } else {

        long startMilliseconds = System.currentTimeMillis();

        try {
          pair.first().invoke(instance);
          sleuthRunner.fire(new SuccessSleuthEvent(clazz.getName(), pair.first().getName(), System.currentTimeMillis() - startMilliseconds));
        } catch (InvocationTargetException invocationTargetException) {
          culprit = new Culprit(clazz.getName(), pair.first().getName(), invocationTargetException.getCause());
          sleuthRunner.fire(new ErrorSleuthEvent(clazz.getName(), pair.first().getName(), System.currentTimeMillis() - startMilliseconds, invocationTargetException.getCause()));
        } catch (Exception exception) {
          culprit = new Culprit(clazz.getName(), pair.first().getName(), exception);
          sleuthRunner.fire(new ErrorSleuthEvent(clazz.getName(), pair.first().getName(), System.currentTimeMillis() - startMilliseconds, exception));
        }
      }
    }

    return culprit;
  }

  /**
   * Returns an iterator over the registered method/annotation pairs in registration order.
   *
   * @return iterator; never {@code null}
   */
  @Override
  public Iterator<Pair<Method, A>> iterator () {

    return pairList.iterator();
  }

  /**
   * Composite key that uniquely identifies a method by name and declared parameter types.
   * <p>
   * Used internally to deduplicate lifecycle methods when scanning class hierarchies.
   */
  private static class MethodKey {

    private final String name;
    private final Class[] parameters;

    /**
     * Constructs a key for the given method signature.
     *
     * @param name       method name; must not be {@code null}
     * @param parameters declared parameter types; must not be {@code null}
     */
    public MethodKey (String name, Class[] parameters) {

      this.name = name;
      this.parameters = parameters;
    }

    /**
     * @return method name component of this key
     */
    public String getName () {

      return name;
    }

    /**
     * @return declared parameter types component of this key
     */
    public Class[] getParameters () {

      return parameters;
    }

    /**
     * @return hash combining method name and parameter types for use in hash-based collections
     */
    @Override
    public int hashCode () {

      return (31 * name.hashCode()) + Arrays.hashCode(parameters);
    }

    /**
     * Two keys are equal when both their name and parameter types match.
     *
     * @param obj object to compare; may be {@code null}
     * @return {@code true} if the names and parameter types are identical
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof MethodKey) && name.equals(((MethodKey)obj).getName()) && Arrays.equals(parameters, ((MethodKey)obj).getParameters());
    }
  }
}
