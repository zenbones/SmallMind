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

import java.util.HashMap;

/**
 * Applies a prioritised chain of {@link AnnotationTranslator}s to build an {@link AnnotationDictionary}
 * for each test class, caching results for repeated lookups.
 * <p>
 * The processor tries each registered translator in order. The first translator that returns an
 * {@link AnnotationDictionary#isImplemented() implemented} dictionary wins and its result is cached
 * for the class. Subsequent calls for the same class return the cached dictionary without re-scanning.
 * <p>
 * This class is thread-safe; {@link #process(Class)} is {@code synchronized}.
 *
 * @see AnnotationTranslator
 * @see NativeAnnotationTranslator
 * @see TestNGAnnotationTranslator
 */
public class AnnotationProcessor {

  private final AnnotationTranslator[] annotationTranslators;
  private final HashMap<Class<?>, AnnotationDictionary> dictionaryMap = new HashMap<>();

  /**
   * Constructs a processor backed by the supplied translators, tried in declaration order.
   *
   * @param annotationTranslators one or more translators that understand different annotation dialects;
   *                              must not be {@code null} or contain {@code null} elements
   */
  public AnnotationProcessor (AnnotationTranslator... annotationTranslators) {

    this.annotationTranslators = annotationTranslators;
  }

  /**
   * Returns the annotation dictionary for the given class, computing and caching it on the first call.
   * <p>
   * Each registered translator is tried in order; the first one to produce an implemented dictionary
   * is used and the result is cached. If no translator recognises the class, {@code null} is returned
   * and nothing is cached.
   *
   * @param clazz class to analyse; never {@code null}
   * @return implemented annotation dictionary for the class, or {@code null} if no supported
   * annotations are present
   */
  public synchronized AnnotationDictionary process (Class<?> clazz) {

    AnnotationDictionary annotationDictionary;

    if ((annotationDictionary = dictionaryMap.get(clazz)) != null) {

      return annotationDictionary;
    }
    for (AnnotationTranslator annotationTranslator : annotationTranslators) {
      if ((annotationDictionary = annotationTranslator.process(clazz)).isImplemented()) {
        dictionaryMap.put(clazz, annotationDictionary);

        return annotationDictionary;
      }
    }

    return null;
  }
}
