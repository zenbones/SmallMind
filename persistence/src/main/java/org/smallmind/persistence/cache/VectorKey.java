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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.aop.CachedWith;
import org.smallmind.persistence.cache.aop.Classifications;
import org.smallmind.persistence.cache.aop.Vector;
import org.smallmind.persistence.cache.aop.VectorCalculator;

/**
 * Serializable cache key for a durable vector, formed from the element class name, vector
 * namespace, index values, and an optional classification suffix.
 *
 * @param <D> vector element durable type
 */
public class VectorKey<D extends Durable<?>> implements Serializable {

  private final Class<D> elementClass;
  private final String key;

  /**
   * Constructs a key from a {@link Vector} annotation and a durable instance, using classification
   * derived from the associated {@link CachedWith} annotation.
   *
   * @param vector       vector annotation providing namespace and index field definitions
   * @param durable      durable instance whose field values populate the index
   * @param elementClass class of the vector elements
   */
  public VectorKey (Vector vector, D durable, Class<D> elementClass) {

    this(VectorCalculator.getVectorArtifact(vector, durable), elementClass, Classifications.get(CachedWith.class, null, vector));
  }

  /**
   * Constructs a key from a precomputed {@link VectorArtifact} with no classification suffix.
   *
   * @param vectorArtifact pre-built artifact containing namespace and index values
   * @param elementClass   class of the vector elements
   */
  public VectorKey (VectorArtifact vectorArtifact, Class<D> elementClass) {

    this(vectorArtifact, elementClass, null);
  }

  /**
   * Constructs a key from a precomputed {@link VectorArtifact} and an optional classification
   * suffix.
   *
   * @param vectorArtifact pre-built artifact containing namespace and index values
   * @param elementClass   class of the vector elements
   * @param classification optional string appended to the key to distinguish variants; may be {@code null}
   */
  public VectorKey (VectorArtifact vectorArtifact, Class<D> elementClass, String classification) {

    this.elementClass = elementClass;

    key = buildKey(vectorArtifact, classification);
  }

  /**
   * Returns the formatted cache key string for this vector.
   *
   * @return vector key string
   */
  public String getKey () {

    return key;
  }

  /**
   * Returns the class of the durable elements stored in this vector.
   *
   * @return vector element class
   */
  public Class<D> getElementClass () {

    return elementClass;
  }

  /**
   * Assembles the key string from the element class name, namespace, index entries, and optional
   * classification.
   *
   * @param vectorArtifact artifact supplying the namespace and indices
   * @param classification optional classification suffix; {@code null} means no suffix
   * @return composed key string
   */
  private String buildKey (VectorArtifact vectorArtifact, String classification) {

    StringBuilder keyBuilder;
    boolean indexed = false;

    keyBuilder = new StringBuilder(elementClass.getSimpleName());

    keyBuilder.append(':').append(vectorArtifact.getVectorNamespace());
    keyBuilder.append('[');
    for (VectorIndex index : vectorArtifact.getVectorIndices()) {
      if (indexed) {
        keyBuilder.append(',');
      }

      keyBuilder.append((index.getIndexAlias().length() > 0) ? index.getIndexAlias() : index.getIndexField()).append('=').append((index.getIndexValue() == null) ? "null" : index.getIndexValue());

      indexed = true;
    }
    keyBuilder.append(']');

    if (classification != null) {
      keyBuilder.append(classification);
    }

    return keyBuilder.toString();
  }

  /**
   * Returns the cache key string.
   *
   * @return string representation of this vector key
   */
  public String toString () {

    return key;
  }

  /**
   * Returns a hash code based on the generated key string.
   *
   * @return hash code of the key string
   */
  public int hashCode () {

    return key.hashCode();
  }

  /**
   * Returns {@code true} when {@code obj} is a {@link VectorKey} with an identical key string.
   *
   * @param obj object to compare against this key
   * @return {@code true} if the keys are equal
   */
  public boolean equals (Object obj) {

    return (obj instanceof VectorKey) && key.equals(((VectorKey<?>)obj).getKey());
  }
}
