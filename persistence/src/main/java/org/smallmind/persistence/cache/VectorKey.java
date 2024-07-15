/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class VectorKey<D extends Durable<?>> implements Serializable {

  private final Class<D> elementClass;
  private final String key;

  public VectorKey (Vector vector, D durable, Class<D> elementClass) {

    this(VectorCalculator.getVectorArtifact(vector, durable), elementClass, Classifications.get(CachedWith.class, null, vector));
  }

  public VectorKey (VectorArtifact vectorArtifact, Class<D> elementClass) {

    this(vectorArtifact, elementClass, null);
  }

  public VectorKey (VectorArtifact vectorArtifact, Class<D> elementClass, String classification) {

    this.elementClass = elementClass;

    key = buildKey(vectorArtifact, classification);
  }

  public String getKey () {

    return key;
  }

  public Class<D> getElementClass () {

    return elementClass;
  }

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

  public String toString () {

    return key;
  }

  public int hashCode () {

    return key.hashCode();
  }

  public boolean equals (Object obj) {

    return (obj instanceof VectorKey) && key.equals(((VectorKey<?>)obj).getKey());
  }
}