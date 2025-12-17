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
 * Iterates over all declared methods in a class hierarchy starting from the highest ancestor.
 */
public class MethodCensus implements Iterable<Method> {

  private final Class<?> clazz;

  /**
   * @param clazz root class whose methods will be enumerated
   */
  public MethodCensus (Class<?> clazz) {

    this.clazz = clazz;
  }

  /**
   * @return iterator over the class hierarchy's declared methods
   */
  @Override
  public Iterator<Method> iterator () {

    return new MethodCensusIterator(clazz);
  }

  /**
   * Iterator that walks the class hierarchy breadth-first from base class to leaf.
   */
  private static class MethodCensusIterator implements Iterator<Method> {

    private final LinkedList<Class<?>> classList = new LinkedList<>();
    private Method[] methods = new Method[0];
    private int methodIndex = 0;

    /**
     * @param clazz starting class
     */
    private MethodCensusIterator (Class<?> clazz) {

      do {
        classList.addFirst(clazz);
      } while ((clazz = clazz.getSuperclass()) != null);
    }

    /**
     * @return {@code true} if a further method exists
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
     * @return next method in the hierarchy
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
     * Unsupported to avoid mutating reflection results.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }
}
