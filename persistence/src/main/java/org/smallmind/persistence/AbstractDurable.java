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
 * Base implementation of {@link Durable} that provides id-based equality, ordering, reflective
 * field comparison, and a cycle-safe {@code toString()}. Subclasses inherit all comparison
 * and display behaviour without additional code.
 *
 * @param <I> the identifier type, which must be {@link Comparable} and {@link java.io.Serializable}
 * @param <D> the concrete durable subtype
 */
public abstract class AbstractDurable<I extends Serializable & Comparable<I>, D extends AbstractDurable<I, D>> implements Overlay<D>, Durable<I> {

  private static final ThreadLocal<Set<Durable<?>>> IN_USE_SET_LOCAL = ThreadLocal.withInitial(HashSet::new);

  /**
   * Orders this durable relative to another by comparing identifiers. A {@code null} id
   * sorts before any non-{@code null} id; two {@code null} ids are considered equal.
   *
   * @param durable the other durable to compare against
   * @return a negative integer, zero, or a positive integer as this durable is less than,
   * equal to, or greater than {@code durable}
   * @throws TypeMismatchException if {@code durable} is not assignment-compatible with this instance's type
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
   * Returns an id-derived hash code when an id is present, or the default identity hash code
   * when the id is {@code null}.
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
   * Returns {@code true} when {@code obj} is a {@link Durable} with an id equal to this
   * instance's id. Falls back to identity equality when either side has a {@code null} id.
   *
   * @param obj the object to compare
   * @return {@code true} when both objects represent the same persisted record
   */
  @Override
  public boolean equals (Object obj) {

    if (obj instanceof Durable) {
      if ((((Durable<?>)obj).getId() == null) || (getId() == null)) {
        return super.equals(obj);
      } else {
        return ((Durable<?>)obj).getId().equals(getId());
      }
    }

    return false;
  }

  /**
   * Returns {@code true} when every field except {@code id} has an equal value in {@code durable}.
   * Locates the {@code id} field via reflection and delegates to {@link #mirrors(Durable, Field...)}.
   *
   * @param durable the durable to compare against
   * @return {@code true} when all non-id fields are equal
   * @throws DataIntegrityException if the {@code id} field cannot be found on this type
   */
  public boolean mirrors (Durable<?> durable) {

    FieldAccessor fieldAccessor;

    if ((fieldAccessor = FieldUtility.getFieldAccessor(this.getClass(), "id")) == null) {
      throw new DataIntegrityException("The durable(%s) does not contain an 'id' field", this.getClass().getName());
    } else {

      return mirrors(durable, fieldAccessor.getField());
    }
  }

  /**
   * Returns {@code true} when every field not listed in {@code exclusions} has an equal value
   * in {@code durable}. The comparison is only attempted when {@code durable} is assignment-compatible
   * with this instance's type.
   *
   * @param durable    the durable to compare against
   * @param exclusions fields to skip during comparison; {@code null} entries are ignored
   * @return {@code true} when all non-excluded fields are equal, {@code false} otherwise
   * @throws PersistenceException if an exclusion field does not belong to this durable's class hierarchy
   */
  public boolean mirrors (Durable<?> durable, Field... exclusions) {

    if (this.getClass().isAssignableFrom(durable.getClass())) {

      boolean excluded;

      try {
        for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(this.getClass())) {

          excluded = false;

          if (exclusions != null) {
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
   * Returns a reflective string listing every field name and value. A per-thread guard
   * prevents infinite recursion when durables form a cyclic graph; a cycle is rendered
   * as {@code ClassName[id=...,...]}.
   *
   * @return the string representation of this durable
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
