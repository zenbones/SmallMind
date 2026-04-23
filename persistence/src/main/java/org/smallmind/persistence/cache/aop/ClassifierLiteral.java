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
 * Programmatic {@link Classifier} implementation that can be constructed in code rather than as an annotation literal.
 */
public class ClassifierLiteral extends AnnotationLiteral<Classifier> implements Classifier {

  private final String value;
  private final boolean asParameter;

  /**
   * Creates a classifier with a fixed literal value and {@code asParameter} defaulting to {@code false}.
   *
   * @param value literal classifier text
   */
  public ClassifierLiteral (String value) {

    this(value, false);
  }

  /**
   * Creates a classifier with explicit value and parameter-reference flag.
   *
   * @param value       literal classifier text or method parameter name
   * @param asParameter {@code true} when {@code value} names a method parameter
   */
  public ClassifierLiteral (String value, boolean asParameter) {

    this.value = value;
    this.asParameter = asParameter;
  }

  /**
   * Returns the classifier text or parameter name.
   *
   * @return classifier string
   */
  @Override
  public String value () {

    return value;
  }

  /**
   * Returns whether the classifier should be read from a method parameter at runtime.
   *
   * @return {@code true} when {@link #value()} is a parameter name
   */
  @Override
  public boolean asParameter () {

    return asParameter;
  }
}
