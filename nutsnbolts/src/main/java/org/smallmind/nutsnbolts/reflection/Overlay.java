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
package org.smallmind.nutsnbolts.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public interface Overlay<O extends Overlay<O>> {

  default void overlaid () {

  }

  default O overlay (O[] overlays) {

    return overlay(overlays, null);
  }

  default O overlay (O[] overlays, Field[] exclusions) {

    if ((overlays != null) && (overlays.length > 0)) {
      for (O overlay : overlays) {
        overlay(overlay, exclusions);
      }
    }

    return (O)this;
  }

  default O overlay (O overlay) {

    return overlay(overlay, null);
  }

  default O overlay (O overlay, Field[] exclusions) {

    if (overlay != null) {
      if (!overlay.getClass().isAssignableFrom(this.getClass())) {
        throw new TypeMismatchException("Overlays must be assignable from type(%s)", this.getClass());
      } else {

        boolean excluded;

        for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(this.getClass())) {

          excluded = false;

          if (exclusions != null) {
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

                  Overlay original;

                  if ((original = (Overlay)fieldAccessor.get(this)) != null) {
                    fieldAccessor.set(this, fieldAccessor.getType().cast(original.overlay((Overlay)value)));
                  } else {
                    if (equivalentToNull(fieldAccessor, value)) {
                      fieldAccessor.set(this, null);
                    } else {
                      fieldAccessor.set(this, value);
                    }
                  }
                } else {
                  if (equivalentToNull(fieldAccessor, value)) {
                    fieldAccessor.set(this, null);
                  } else {
                    fieldAccessor.set(this, value);
                  }
                }
              }
            } catch (IllegalAccessException | InvocationTargetException exception) {
              throw new OverlayException(exception);
            }
          }
        }
      }
    }

    this.overlaid();

    return (O)this;
  }

  private boolean equivalentToNull (FieldAccessor fieldAccessor, Object value)
    throws OverlayException {

    for (Annotation fieldAnnotation : fieldAccessor.getField().getAnnotations()) {

      OverlayNullifier overlayNullifier;

      if ((overlayNullifier = fieldAnnotation.annotationType().getAnnotation(OverlayNullifier.class)) != null) {

        return internalEquivalentToNull(overlayNullifier, fieldAnnotation, value);
      }
    }

    return false;
  }

  private <A extends Annotation, T> boolean internalEquivalentToNull (OverlayNullifier overlayNullifier, A annotation, T object)
    throws OverlayException {

    try {

      OverlayNullifierValidator<A, T> overlayNullifierValidator = (OverlayNullifierValidator<A, T>)overlayNullifier.validatedBy().getConstructor().newInstance();

      overlayNullifierValidator.initialize(annotation);

      return overlayNullifierValidator.equivalentToNull(object);
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
      throw new OverlayException(exception);
    }
  }
}
