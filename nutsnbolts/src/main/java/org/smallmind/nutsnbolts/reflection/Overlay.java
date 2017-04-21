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
package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.Field;
import java.util.List;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.nutsnbolts.reflection.type.UnexpectedGenericDeclaration;

public abstract class Overlay<O extends Overlay<O>> implements Differentiable<O> {

  private final Class<O> overlayClass;

  public Overlay () {

    List<Class<?>> typeArguments = GenericUtility.getTypeArguments(Overlay.class, this.getClass());

    if (typeArguments.size() != 1) {
      throw new UnexpectedGenericDeclaration("Expecting a single generic type");
    } else if (!Overlay.class.isAssignableFrom(overlayClass = (Class<O>)typeArguments.get(0))) {
      throw new UnexpectedGenericDeclaration("Expecting a single generic type extending %s", Overlay.class.getSimpleName());
    }
  }

  public Class<? extends Overlay> getOverlayClass () {

    return overlayClass;
  }

  public O overlay (Object... overlays)
    throws IllegalAccessException {

    return overlay(overlays, null);
  }

  public O overlay (Object[] overlays, Field[] exclusions)
    throws IllegalAccessException {

    if ((overlays != null) && (overlays.length > 0)) {
      for (Object overlay : overlays) {
        if (overlay != null) {
          if (!this.getClass().isAssignableFrom(overlay.getClass())) {
            throw new TypeMismatchException("Overlays must be assignable from type(%s)", this.getClass());
          } else {

            boolean excluded;

            for (Field field : FieldUtility.getFields(this.getClass())) {

              excluded = false;

              if ((exclusions != null) && (exclusions.length > 0)) {
                for (Field exclusion : exclusions) {
                  if (!this.getClass().isAssignableFrom(exclusion.getDeclaringClass())) {
                    throw new TypeMismatchException("The type(%s) does not contain the excluded field(%s)", this.getClass().getName(), exclusion.getName());
                  } else if (exclusion.equals(field)) {
                    excluded = true;
                    break;
                  }
                }
              }

              if (!excluded) {

                Object value;

                if ((value = field.get(overlay)) != null) {
                  field.set(this, value);
                }
              }
            }
          }
        }
      }
    }

    return overlayClass.cast(this);
  }
}
