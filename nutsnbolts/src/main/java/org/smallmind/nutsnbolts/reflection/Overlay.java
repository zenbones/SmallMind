/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public interface Overlay<O extends Overlay<O>> {

  default O overlay (O[] overlays) {

    return overlay(overlays, null);
  }

  default void overliad () {

  }

  default O overlay (O[] overlays, Field[] exclusions) {

    if ((overlays != null) && (overlays.length > 0)) {
      for (Object overlay : overlays) {
        if (overlay != null) {
          if (!overlay.getClass().isAssignableFrom(this.getClass())) {
            throw new TypeMismatchException("Overlays must be assignable from type(%s)", this.getClass());
          } else {

            boolean excluded;

            for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(this.getClass())) {

              excluded = false;

              if ((exclusions != null) && (exclusions.length > 0)) {
                for (Field exclusion : exclusions) {
                  if (!exclusion.getDeclaringClass().isAssignableFrom(this.getClass())) {
                    throw new TypeMismatchException("The type(%s) does not contain the excluded field(%s)", this.getClass().getName(), exclusion.getName());
                  } else if (exclusion.equals(fieldAccessor.getField())) {
                    excluded = true;
                    break;
                  }
                }
              }

              if (!excluded) {

                Object value;

                try {
                  if ((value = fieldAccessor.get(overlay)) != null) {
                    if (Overlay.class.isAssignableFrom(fieldAccessor.getType())) {

                      Object original;

                      if ((original = fieldAccessor.get(this)) != null) {
                        fieldAccessor.set(this, ((Overlay)original).overlay(new Overlay[] {(Overlay)value}));
                      } else {
                        fieldAccessor.set(this, value);
                      }
                    } else {
                      fieldAccessor.set(this, value);
                    }
                  }
                } catch (IllegalAccessException | InvocationTargetException exception) {
                  throw new OverlayException(exception);
                }
              }
            }
          }
        }
      }
    }

    this.overliad();

    return (O)this;
  }
}
