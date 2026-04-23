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

/**
 * Describes index creation options that mirror the MongoDB driver's {@code IndexOptions}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface IndexOptions {

  /**
   * Whether the index build runs in the background without blocking other operations.
   *
   * @return {@code true} for a background build
   */
  boolean background () default false;

  /**
   * Whether the index enforces a uniqueness constraint on the indexed field(s).
   *
   * @return {@code true} for a unique index
   */
  boolean unique () default false;

  /**
   * An explicit name for the index; MongoDB generates a name when this is empty.
   *
   * @return the index name, or an empty string to use the generated name
   */
  String name () default "";

  /**
   * Whether documents that do not contain the indexed field are excluded from the index.
   *
   * @return {@code true} for a sparse index
   */
  boolean sparse () default false;

  /**
   * Number of seconds after which documents in the collection expire; {@code 0} disables TTL.
   *
   * @return the TTL in seconds
   */
  long expireAfterSeconds () default 0;

  /**
   * The index version number; {@code 0} lets the server choose the version.
   *
   * @return the index version
   */
  int version () default 0;

  /**
   * Weighting document for text indexes in JSON format; empty string uses the default weights.
   *
   * @return the weights JSON string
   */
  String weights () default "";

  /**
   * Default language used for text index stemming and stop words; empty string uses the server default.
   *
   * @return the default language
   */
  String defaultLanguage () default "";

  /**
   * Field name whose value overrides the default language for text indexing; empty string disables the override.
   *
   * @return the language override field name
   */
  String languageOverride () default "";

  /**
   * Version of the text index; {@code 0} lets the server choose.
   *
   * @return the text index version
   */
  int textVersion () default 0;

  /**
   * Version of the 2dsphere index; {@code 0} lets the server choose.
   *
   * @return the 2dsphere index version
   */
  int sphereVersion () default 0;

  /**
   * Number of precision bits for legacy 2d geo indexes; {@code 0} uses the server default.
   *
   * @return the precision bits
   */
  int bits () default 0;

  /**
   * Minimum longitude/latitude boundary for legacy 2d geo indexes.
   *
   * @return the lower geo boundary
   */
  double min () default -360;

  /**
   * Maximum longitude/latitude boundary for legacy 2d geo indexes.
   *
   * @return the upper geo boundary
   */
  double max () default 360;

  /**
   * Storage engine options as a JSON document; empty string uses the default options.
   *
   * @return the storage engine options JSON
   */
  String storageEngine () default "";

  /**
   * Partial filter expression as a JSON document that restricts which documents are included in the index.
   *
   * @return the partial filter expression JSON
   */
  String partialFilterExpression () default "";

  /**
   * Collation settings applied to this index.
   *
   * @return the collation configuration
   */
  Collation collation () default @Collation();

  /**
   * Wildcard projection document as JSON, controlling which fields a wildcard index covers.
   *
   * @return the wildcard projection JSON
   */
  String wildcardProjection () default "";

  /**
   * Whether the index is hidden from the query planner, allowing it to be evaluated without affecting queries.
   *
   * @return {@code true} for a hidden index
   */
  boolean hidden () default false;
}
