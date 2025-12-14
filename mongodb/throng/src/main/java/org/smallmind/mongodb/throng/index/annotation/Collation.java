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

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
/**
 * Describes collation settings to be applied to an index.
 */
public @interface Collation {

  /**
   * @return locale identifier for the collation; defaults to the simple binary collation
   */
  String locale () default "simple";

  /**
   * @return whether case comparisons are performed at an extra level
   */
  boolean caseLevel () default false;

  /**
   * @return ordering preference for upper or lower case when strength is set accordingly
   */
  CollationCaseFirst caseFirst () default CollationCaseFirst.OFF;

  /**
   * @return comparison strength indicating sensitivity to case, diacritics, etc.
   */
  CollationStrength strength () default CollationStrength.TERTIARY;

  /**
   * @return whether numeric strings are ordered by numeric value
   */
  boolean numericOrdering () default false;

  /**
   * @return whether spaces and punctuation are considered ignorable
   */
  CollationAlternate alternate () default CollationAlternate.NON_IGNORABLE;

  /**
   * @return which characters are affected by {@link #alternate()}
   */
  CollationMaxVariable maxVariable () default CollationMaxVariable.PUNCT;

  /**
   * @return whether text is normalized before comparison
   */
  boolean normalization () default false;

  /**
   * @return whether secondary comparisons are reversed (for certain languages)
   */
  boolean backwards () default false;
}
