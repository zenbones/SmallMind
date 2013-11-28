/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.persistence;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractDurable<I extends Comparable<I>> implements Durable<I> {

  private static final long serialVersionUID = 1L;
  private static final ThreadLocal<Set<Durable>> IN_USE_SET_LOCAL = new ThreadLocal<Set<Durable>>() {

    @Override
    protected Set<Durable> initialValue () {

      return new HashSet<Durable>();
    }
  };

  public int compareTo (Durable<I> durable) {

    if (getId() == null) {
      if (durable.getId() == null) {

        return 0;
      }
      else {

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
      }
      else {
        return ((Durable)obj).getId().equals(getId());
      }
    }

    return false;
  }

  public boolean mirrors (Durable durable) {

    return mirrors(durable, "id");
  }

  private boolean mirrors (Durable durable, String... exclusions) {

    if (this.getClass().isAssignableFrom(durable.getClass())) {

      boolean excluded;

      try {
        for (Field field : DurableFields.getFields(this.getClass())) {

          excluded = false;

          if ((exclusions != null) && (exclusions.length > 0)) {
            for (String exclusion : exclusions) {
              if (exclusion.equals(field.getName())) {
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
            }
            else if (!myValue.equals(theirValue)) {

              return false;
            }
          }
        }
      }
      catch (IllegalAccessException illegalAccessException) {
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
    }
    else {
      try {
        IN_USE_SET_LOCAL.get().add(this);

        boolean first = false;

        displayBuilder.append(this.getClass().getSimpleName()).append('[');

        try {
          for (Field field : DurableFields.getFields(this.getClass())) {
            if (first) {
              displayBuilder.append(',');
            }

            displayBuilder.append(field.getName()).append('=').append(field.get(this));
            first = true;
          }
        }
        catch (IllegalAccessException illegalAccessException) {
          throw new RuntimeException(illegalAccessException);
        }

        displayBuilder.append(']');
      }
      finally {
        IN_USE_SET_LOCAL.get().remove(this);
      }
    }

    return displayBuilder.toString();
  }
}
