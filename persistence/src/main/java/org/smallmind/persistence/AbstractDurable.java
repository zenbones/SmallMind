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
package org.smallmind.persistence;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.FieldUtility;
import org.smallmind.nutsnbolts.reflection.Overlay;

public abstract class AbstractDurable<I extends Serializable & Comparable<I>, D extends AbstractDurable<I, D>> implements Overlay<D>, Durable<I> {

  private static final ThreadLocal<Set<Durable>> IN_USE_SET_LOCAL = ThreadLocal.withInitial(HashSet::new);

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

  @Override
  public int hashCode () {

    if (getId() == null) {

      return super.hashCode();
    }

    int h = getId().hashCode();

    h ^= (h >>> 20) ^ (h >>> 12);

    return h ^ (h >>> 7) ^ (h >>> 4);
  }

  @Override
  public boolean equals (Object obj) {

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

    return mirrors(durable, FieldUtility.getFieldAccessor(this.getClass(), "id").getField());
  }

  public boolean mirrors (Durable durable, Field... exclusions) {

    if (this.getClass().isAssignableFrom(durable.getClass())) {

      boolean excluded;

      try {
        for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(this.getClass())) {

          excluded = false;

          if ((exclusions != null) && (exclusions.length > 0)) {
            for (Field exclusion : exclusions) {
              if (exclusion != null) {
                if (!exclusion.getDeclaringClass().isAssignableFrom(this.getClass())) {
                  throw new PersistenceException("The type(%s) does not contain the excluded field(%s)", this.getClass().getName(), exclusion.getName());
                } else if (exclusion.equals(fieldAccessor.getField())) {
                  excluded = true;
                  break;
                }
              }
            }
          }

          if (!excluded) {

            Object myValue = fieldAccessor.get(this);
            Object theirValue = fieldAccessor.get(durable);

            if ((myValue == null)) {
              if (theirValue != null) {

                return false;
              }
            } else if (!myValue.equals(theirValue)) {

              return false;
            }
          }
        }
      } catch (IllegalAccessException | InvocationTargetException exception) {
        throw new RuntimeException(exception);
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
          for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(this.getClass())) {
            if (first) {
              displayBuilder.append(',');
            }

            displayBuilder.append(fieldAccessor.getName()).append('=').append(fieldAccessor.get(this));
            first = true;
          }
        } catch (IllegalAccessException | InvocationTargetException exception) {
          throw new RuntimeException(exception);
        }

        displayBuilder.append(']');
      } finally {
        IN_USE_SET_LOCAL.get().remove(this);
      }
    }

    return displayBuilder.toString();
  }
}
