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
package org.smallmind.persistence.cache.aop;

import org.smallmind.nutsnbolts.lang.AnnotationLiteral;

/**
 * Programmatic implementation of {@link Vector} for constructing vector metadata at runtime.
 */
public class VectorLiteral extends AnnotationLiteral<Vector> implements Vector {

  private final String namespace;
  private final Key[] keys;
  private final Classifier classifier;

  /**
   * Creates a vector literal without a classifier.
   *
   * @param namespace namespace used in the cache key
   * @param keys      key definitions that compose the vector
   */
  public VectorLiteral (String namespace, Key[] keys) {

    this(namespace, keys, new ClassifierLiteral(""));
  }

  /**
   * Creates a vector literal with explicit classifier.
   *
   * @param namespace  namespace used in the cache key
   * @param keys       key definitions that compose the vector
   * @param classifier classifier appended to the key
   */
  public VectorLiteral (String namespace, Key[] keys, Classifier classifier) {

    this.namespace = namespace;
    this.keys = keys;
    this.classifier = classifier;
  }

  /**
   * @return namespace used in the cache key
   */
  @Override
  public String namespace () {

    return namespace;
  }

  /**
   * @return key definitions that compose the vector
   */
  @Override
  public Key[] value () {

    return keys;
  }

  /**
   * @return classifier appended to the key
   */
  @Override
  public Classifier classifier () {

    return classifier;
  }
}
