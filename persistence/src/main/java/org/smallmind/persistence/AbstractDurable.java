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

/**
 * Base implementation of a persistent {@link Durable} that supplies common comparison,
 * identity and debugging helpers. Equality and ordering are based on the durable id
 * when it is present, and fall back to object identity when no id has been assigned.
 * Reflection is used to provide mirror comparisons and a descriptive {@code toString()}.
 *
 * @param <I> the identifier type, which must be comparable and serializable
 * @param <D> the concrete durable type
 */
public abstract class AbstractDurable<I extends Serializable & Comparable<I>, D extends AbstractDurable<I, D>> implements Overlay<D>, Durable<I> {

  private static final ThreadLocal<Set<Durable>> IN_USE_SET_LOCAL = ThreadLocal.withInitial(HashSet::new);

  /**
   * Compares this durable to another, ordering by identifier when both ids are populated.
   * A durable with a {@code null} id is considered less than one with an id.
   *
   * @param durable the other durable instance
   * @return a negative value when {@code this} should sort before {@code durable}, a positive value when after, and zero when equal
   * @throws TypeMismatchException if the provided durable is not of a compatible type
   */
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

  /**
   * Computes a hash code based on the durable id when available, or falls back to the
   * default object hash code when the id has not yet been set.
   *
   * @return the hash code for this durable
   */
  @Override
  public int hashCode () {

    if (getId() == null) {

      return super.hashCode();
    }

    int h = getId().hashCode();

    h ^= (h >>> 20) ^ (h >>> 12);

    return h ^ (h >>> 7) ^ (h >>> 4);
  }

  /**
   * Tests equality based on the durable id when both sides have an id; otherwise defaults
   * to the inherited {@link Object#equals(Object)} behavior.
   *
   * @param obj the object to compare
   * @return {@code true} when the two objects represent the same durable record
   */
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

  /**
   * Determines whether all non-id fields mirror those of the supplied durable.
   *
   * @param durable the durable to compare against
   * @return {@code true} when all properties except the id match, otherwise {@code false}
   */
  public boolean mirrors (Durable durable) {

    return mirrors(durable, FieldUtility.getFieldAccessor(this.getClass(), "id").getField());
  }

  /**
   * Determines whether all fields other than the supplied exclusions mirror those of the provided durable.
   *
   * @param durable    the durable to compare against
   * @param exclusions optional fields to omit from the comparison
   * @return {@code true} when the two objects match on all non-excluded fields
   * @throws PersistenceException if an exclusion does not belong to this durable type
   */
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

  /**
   * Builds a reflective string representation of the durable, listing each field name and value.
   * Recursion is guarded by a per-thread set so that cyclical graphs degrade gracefully.
   *
   * @return the string form of this durable
   */
  @Override
  public String toString () {

    StringBuilder displayBuilder = new StringBuilder();

    // Prevent recursive invocations when traversing graphs of durables
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
