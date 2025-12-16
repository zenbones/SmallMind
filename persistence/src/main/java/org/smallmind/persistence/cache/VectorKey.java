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
 * Key used to cache vectors, derived from {@link Vector} metadata, the durable instance, and
 * optional classification.
 */
public class VectorKey<D extends Durable<?>> implements Serializable {

  private final Class<D> elementClass;
  private final String key;

  /**
   * Builds a key from the vector annotation, durable instance, and classification derived from
   * {@link CachedWith}.
   *
   * @param vector       vector annotation
   * @param durable      durable instance providing index values
   * @param elementClass vector element class
   */
  public VectorKey (Vector vector, D durable, Class<D> elementClass) {

    this(VectorCalculator.getVectorArtifact(vector, durable), elementClass, Classifications.get(CachedWith.class, null, vector));
  }

  /**
   * Builds a key from a precomputed {@link VectorArtifact}.
   *
   * @param vectorArtifact vector details
   * @param elementClass   vector element class
   */
  public VectorKey (VectorArtifact vectorArtifact, Class<D> elementClass) {

    this(vectorArtifact, elementClass, null);
  }

  /**
   * Builds a key from a vector artifact and optional classification suffix.
   *
   * @param vectorArtifact vector details
   * @param elementClass   vector element class
   * @param classification optional classification string
   */
  public VectorKey (VectorArtifact vectorArtifact, Class<D> elementClass, String classification) {

    this.elementClass = elementClass;

    key = buildKey(vectorArtifact, classification);
  }

  /**
   * @return cacheable vector key string
   */
  public String getKey () {

    return key;
  }

  /**
   * @return vector element class
   */
  public Class<D> getElementClass () {

    return elementClass;
  }

  /**
   * Builds the string form of the vector key using namespace, indices, and optional classification.
   *
   * @param vectorArtifact artifact describing vector indices and namespace
   * @param classification optional classification suffix
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
   * Hashes based on the generated key string.
   *
   * @return hash code for this key
   */
  public int hashCode () {

    return key.hashCode();
  }

  /**
   * Keys are equal when their underlying key strings match.
   *
   * @param obj other object
   * @return equality result
   */
  public boolean equals (Object obj) {

    return (obj instanceof VectorKey) && key.equals(((VectorKey<?>)obj).getKey());
  }
}
