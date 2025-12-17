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
 * Maintains an ordered collection of methods annotated with a specific lifecycle annotation and provides invocation support.
 *
 * @param <A> annotation type represented by this methodology
 */
public class AnnotationMethodology<A extends Annotation> implements Iterable<Pair<Method, A>> {

  private final HashSet<MethodKey> methodSet = new HashSet<>();
  private final LinkedList<Pair<Method, A>> pairList = new LinkedList<>();

  /**
   * Adds a method/annotation pair if it has not already been registered.
   *
   * @param method     reflected method
   * @param annotation associated annotation
   */
  public void add (Method method, A annotation) {

    if (methodSet.add(new MethodKey(method.getName(), method.getParameterTypes()))) {
      pairList.add(new Pair<>(method, annotation));
    }
  }

  /**
   * Executes each annotated method in order, emitting Sleuth events and capturing any resulting culprit.
   *
   * @param sleuthRunner runner used for event dispatch
   * @param culprit      prior culprit that should cause remaining methods to be skipped; may be {@code null}
   * @param clazz        declaring class
   * @param instance     instance to invoke methods on
   * @return updated culprit after invocation
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
   * @return iterator over the registered method/annotation pairs
   */
  @Override
  public Iterator<Pair<Method, A>> iterator () {

    return pairList.iterator();
  }

  /**
   * Uniquely identifies a method by name and parameter signature.
   */
  private static class MethodKey {

    private final String name;
    private final Class[] parameters;

    /**
     * @param name       method name
     * @param parameters parameter types
     */
    public MethodKey (String name, Class[] parameters) {

      this.name = name;
      this.parameters = parameters;
    }

    public String getName () {

      return name;
    }

    /**
     * @return parameter types associated with the method
     */
    public Class[] getParameters () {

      return parameters;
    }

    /**
     * @return hash combining method name and parameter types
     */
    @Override
    public int hashCode () {

      return (31 * name.hashCode()) + Arrays.hashCode(parameters);
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof MethodKey) && name.equals(((MethodKey)obj).getName()) && Arrays.equals(parameters, ((MethodKey)obj).getParameters());
    }
  }
}
