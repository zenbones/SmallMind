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

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Provides an {@link Iterable} view over all declared methods in a class hierarchy, visiting
 * superclasses before subclasses.
 * <p>
 * This ordering ensures that when {@link AnnotationMethodology#add} deduplicates by name and
 * parameter types, overriding methods in subclasses take precedence because the superclass
 * version is registered first and subsequent identical signatures are dropped.
 *
 * @see AnnotationMethodology
 */
public class MethodCensus implements Iterable<Method> {

  private final Class<?> clazz;

  /**
   * Constructs a census rooted at the given class.
   *
   * @param clazz leaf class in the hierarchy to enumerate; must not be {@code null}
   */
  public MethodCensus (Class<?> clazz) {

    this.clazz = clazz;
  }

  /**
   * Returns a new iterator over all declared methods starting from the topmost superclass.
   *
   * @return iterator traversing the hierarchy from base to leaf; never {@code null}
   */
  @Override
  public Iterator<Method> iterator () {

    return new MethodCensusIterator(clazz);
  }

  /**
   * Iterator that walks the class hierarchy from the highest ancestor down to the leaf class,
   * yielding each class's declared methods in turn.
   */
  private static class MethodCensusIterator implements Iterator<Method> {

    private final LinkedList<Class<?>> classList = new LinkedList<>();
    private Method[] methods = new Method[0];
    private int methodIndex = 0;

    /**
     * Builds the ancestor stack for the given class, placing the topmost superclass first.
     *
     * @param clazz leaf class to start from; must not be {@code null}
     */
    private MethodCensusIterator (Class<?> clazz) {

      do {
        classList.addFirst(clazz);
      } while ((clazz = clazz.getSuperclass()) != null);
    }

    /**
     * Returns {@code true} if at least one more method is available across the hierarchy.
     *
     * @return {@code true} when a further method can be returned by {@link #next()}
     */
    @Override
    public boolean hasNext () {

      while (methodIndex == methods.length) {
        if (classList.isEmpty()) {

          return false;
        } else {
          methods = classList.removeFirst().getDeclaredMethods();
          methodIndex = 0;
        }
      }

      return true;
    }

    /**
     * Returns the next method in hierarchy order.
     *
     * @return the next declared method; never {@code null}
     * @throws NoSuchElementException if no more methods are available
     */
    @Override
    public Method next () {

      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return methods[methodIndex++];
    }

    /**
     * Not supported; removal from a reflection-backed view is not meaningful.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }
}
