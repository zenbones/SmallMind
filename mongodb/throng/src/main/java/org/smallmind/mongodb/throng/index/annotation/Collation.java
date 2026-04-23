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
package org.smallmind.mongodb.throng.index.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;

/**
 * Describes collation settings to be applied to an index.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Collation {

  /**
   * Locale identifier for the collation; defaults to the simple binary collation ({@code "simple"}).
   *
   * @return the locale string
   */
  String locale () default "simple";

  /**
   * Whether case comparisons are performed at a dedicated extra level of comparison.
   *
   * @return {@code true} to enable case-level comparison
   */
  boolean caseLevel () default false;

  /**
   * Ordering preference for upper- or lower-case letters when the strength is set to allow it.
   *
   * @return the case-first setting
   */
  CollationCaseFirst caseFirst () default CollationCaseFirst.OFF;

  /**
   * Comparison strength indicating the sensitivity to case, diacritics, and other differences.
   *
   * @return the collation strength
   */
  CollationStrength strength () default CollationStrength.TERTIARY;

  /**
   * Whether numeric strings are collated by their numeric value rather than lexicographically.
   *
   * @return {@code true} to enable numeric ordering
   */
  boolean numericOrdering () default false;

  /**
   * Whether spaces and punctuation are treated as ignorable characters.
   *
   * @return the alternate handling setting
   */
  CollationAlternate alternate () default CollationAlternate.NON_IGNORABLE;

  /**
   * Which characters are affected by the {@link #alternate()} setting.
   *
   * @return the max-variable setting
   */
  CollationMaxVariable maxVariable () default CollationMaxVariable.PUNCT;

  /**
   * Whether text is Unicode-normalized before comparison.
   *
   * @return {@code true} to enable normalization
   */
  boolean normalization () default false;

  /**
   * Whether secondary differences are sorted in reverse order, as required by certain languages.
   *
   * @return {@code true} to enable backwards secondary sorting
   */
  boolean backwards () default false;
}
