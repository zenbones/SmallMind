/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class VectorKey<D extends Durable> implements Serializable {

  private Class<D> elementClass;
  private String key;

  public VectorKey (VectorIndex[] vectorIndices, Class<D> elementClass) {

    this(vectorIndices, elementClass, null);
  }

  public VectorKey (VectorIndex[] vectorIndices, Class<D> elementClass, String classification) {

    this.elementClass = elementClass;

    key = buildKey(vectorIndices, classification);
  }

  public String getKey () {

    return key;
  }

  public Class<D> getElementClass () {

    return elementClass;
  }

  private String buildKey (VectorIndex[] vectorIndices, String classification) {

    StringBuilder keyBuilder;
    boolean indexed = false;

    keyBuilder = new StringBuilder(elementClass.getSimpleName());

    keyBuilder.append('[');
    for (VectorIndex index : vectorIndices) {
      if (indexed) {
        keyBuilder.append(',');
      }

      keyBuilder.append(index.getIndexClass().getSimpleName()).append('.').append((index.getIndexAlias().length() > 0) ? index.getIndexAlias() : index.getIndexField()).append('=').append((index.getIndexValue() == null) ? "null" : index.getIndexValue());

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

    return (obj instanceof VectorKey) && key.equals(((VectorKey)obj).getKey());
  }
}