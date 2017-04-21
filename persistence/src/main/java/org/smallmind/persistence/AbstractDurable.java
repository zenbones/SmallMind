/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.persistence;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;
import org.smallmind.nutsnbolts.reflection.FieldUtility;
import org.smallmind.nutsnbolts.reflection.Overlay;

public abstract class AbstractDurable<I extends Serializable & Comparable<I>, D extends AbstractDurable<I, D>> extends Overlay<D> implements Durable<I> {

  private static final ThreadLocal<Set<Durable>> IN_USE_SET_LOCAL = new ThreadLocal<Set<Durable>>() {

    @Override
    protected Set<Durable> initialValue () {

      return new HashSet<>();
    }
  };

  public int compareTo (Durable<I> durable) {

    if (!this.getClass().isAssignableFrom(durable.getClass())) {
      throw new TypeMismatchException("Comparison must be made with a type which extends %s", this.getClass().getSimpleName());
    }

    if (getId() == null) {
      if (durable.getId() == null) {

        return 0;
      } else {

        return -1;
      }
    }

    if (durable.getId() == null) {

      return 1;
    }

    return durable.getId().compareTo(getId());
  }

  public synchronized int hashCode () {

    if (getId() == null) {

      return super.hashCode();
    }

    int h = getId().hashCode();

    h ^= (h >>> 20) ^ (h >>> 12);

    return h ^ (h >>> 7) ^ (h >>> 4);
  }

  @Override
  public synchronized boolean equals (Object obj) {

    if (obj instanceof Durable) {
      if ((((Durable)obj).getId() == null) || (getId() == null)) {
        return super.equals(obj);
      } else {
        return ((Durable)obj).getId().equals(getId());
      }
    }

    return false;
  }

  public boolean mirrors (Durable durable) {

    return mirrors(durable, FieldUtility.getField(this.getClass(), "id"));
  }

  private boolean mirrors (Durable durable, Field... exclusions) {

    if (this.getClass().isAssignableFrom(durable.getClass())) {

      boolean excluded;

      try {
        for (Field field : FieldUtility.getFields(this.getClass())) {

          excluded = false;

          if ((exclusions != null) && (exclusions.length > 0)) {
            for (Field exclusion : exclusions) {
              if (!this.getClass().isAssignableFrom(exclusion.getDeclaringClass())) {
                throw new PersistenceException("The type(%s) does not contain the excluded field(%s)", this.getClass().getName(), exclusion.getName());
              } else if (exclusion.equals(field)) {
                excluded = true;
                break;
              }
            }
          }

          if (!excluded) {

            Object myValue = field.get(this);
            Object theirValue = field.get(durable);

            if ((myValue == null)) {
              if (theirValue != null) {

                return false;
              }
            } else if (!myValue.equals(theirValue)) {

              return false;
            }
          }
        }
      } catch (IllegalAccessException illegalAccessException) {
        throw new RuntimeException(illegalAccessException);
      }

      return true;
    }

    return false;
  }

  public String toString () {

    StringBuilder displayBuilder = new StringBuilder();

    if (IN_USE_SET_LOCAL.get().contains(this)) {
      displayBuilder.append(this.getClass().getSimpleName()).append("[id=").append(getId()).append(",...]");
    } else {
      try {
        IN_USE_SET_LOCAL.get().add(this);

        boolean first = false;

        displayBuilder.append(this.getClass().getSimpleName()).append('[');

        try {
          for (Field field : FieldUtility.getFields(this.getClass())) {
            if (first) {
              displayBuilder.append(',');
            }

            displayBuilder.append(field.getName()).append('=').append(field.get(this));
            first = true;
          }
        } catch (IllegalAccessException illegalAccessException) {
          throw new RuntimeException(illegalAccessException);
        }

        displayBuilder.append(']');
      } finally {
        IN_USE_SET_LOCAL.get().remove(this);
      }
    }

    return displayBuilder.toString();
  }
}
