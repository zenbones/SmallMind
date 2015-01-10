/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
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
public class DurableKey<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Serializable {

  private Class<D> durableClass;
  private String key;

  public DurableKey (Class<D> durableClass, I id) {

    this.durableClass = durableClass;

    StringBuilder keyBuilder = new StringBuilder(durableClass.getSimpleName());

    keyBuilder.append('=');
    keyBuilder.append(id);

    key = keyBuilder.toString();
  }

  public Class<D> getDurableClass () {

    return durableClass;
  }

  public String getKey () {

    return key;
  }

  public String getIdAsString () {

    return key.substring(key.indexOf('=') + 1);
  }

  public String toString () {

    return key;
  }

  public int hashCode () {

    return key.hashCode();
  }

  public boolean equals (Object obj) {

    return (obj instanceof DurableKey) && key.equals(((DurableKey)obj).getKey());
  }
}
