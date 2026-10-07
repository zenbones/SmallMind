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
package org.smallmind.nutsnbolts.reflection;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;

/**
 * Mixin interface that gives implementing classes the ability to copy non-null field values from one
 * instance to another, recursing into nested {@code Overlay} fields and honouring
 * {@link OverlayNullifier}-annotated sentinel values that should become {@code null}. Fields are reached
 * through {@link FieldUtility#getFieldAccessors(MethodHandles.Lookup, Class)} with the access of the
 * {@link MethodHandles.Lookup} each operation is given, which caches them for later operations from the same
 * module.
 *
 * @param <O> the self-referential concrete type that implements this interface
 */
public interface Overlay<O extends Overlay<O>> {

  /**
   * Callback invoked after each overlay operation completes, allowing subclasses to react to changes.
   * The default implementation is a no-op.
   */
  default void overlaid () {

  }

  /**
   * Applies each element of the supplied array to this object in order, skipping {@code null} entries.
   *
   * @param lookup   a full-privilege lookup, as returned by {@link MethodHandles#lookup()}, whose access is used
   *                 to read and write fields
   * @param overlays the overlay objects to apply; may be {@code null} or empty
   * @return this instance after all overlays have been applied
   */
  default O overlay (MethodHandles.Lookup lookup, O[] overlays) {

    return overlay(lookup, overlays, null);
  }

  /**
   * Applies each element of the supplied array to this object in order, skipping excluded fields.
   *
   * @param lookup     a full-privilege lookup, as returned by {@link MethodHandles#lookup()}, whose access is
   *                   used to read and write fields
   * @param overlays   the overlay objects to apply; may be {@code null} or empty
   * @param exclusions fields on this object that must not be overwritten; may be {@code null}
   * @return this instance after all overlays have been applied
   */
  default O overlay (MethodHandles.Lookup lookup, O[] overlays, Field[] exclusions) {

    if (overlays != null) {
      for (O overlay : overlays) {
        overlay(lookup, overlay, exclusions);
      }
    }

    return (O)this;
  }

  /**
   * Copies non-null field values from {@code overlay} to this object, honouring nullifier annotations.
   *
   * @param lookup  a full-privilege lookup, as returned by {@link MethodHandles#lookup()}, whose access is used
   *                to read and write fields
   * @param overlay the source object whose non-null values should be applied; may be {@code null}
   * @return this instance after the overlay has been applied
   */
  default O overlay (MethodHandles.Lookup lookup, O overlay) {

    return overlay(lookup, overlay, null);
  }

  /**
   * Copies non-null field values from {@code overlay} to this object, skipping excluded fields and
   * honouring nullifier annotations that convert sentinel values to {@code null}. Nested {@code Overlay}
   * fields are overlaid with the same {@code lookup}.
   *
   * @param lookup     a full-privilege lookup, as returned by {@link MethodHandles#lookup()}, whose access is
   *                   used to read and write fields and to construct {@link OverlayNullifierValidator}s
   * @param overlay    the source object whose non-null values should be applied; may be {@code null}
   * @param exclusions fields on this object that must not be overwritten; may be {@code null}
   * @return this instance after the overlay has been applied
   * @throws TypeMismatchException    if the overlay's type is not assignable to this object's type, or
   *                                  if an exclusion field belongs to an unrelated class
   * @throws IllegalArgumentException if {@code lookup} does not have full privilege access
   * @throws OverlayException         if {@code lookup} cannot access a field, its getter or setter, or a
   *                                  validator's constructor, if a {@code final} field without a setter would
   *                                  be written, or if a getter, setter, or validator constructor fails
   */
  default O overlay (MethodHandles.Lookup lookup, O overlay, Field[] exclusions) {

    if (overlay != null) {
      if (!overlay.getClass().isAssignableFrom(this.getClass())) {
        throw new TypeMismatchException("Overlays must be assignable from type(%s)", this.getClass());
      } else {

        FieldAccessor[] fieldAccessors;
        boolean excluded;

        try {
          fieldAccessors = FieldUtility.getFieldAccessors(lookup, this.getClass());
        } catch (IllegalAccessException illegalAccessException) {
          throw new OverlayException(illegalAccessException);
        }

        for (FieldAccessor fieldAccessor : fieldAccessors) {

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
                    fieldAccessor.set(this, fieldAccessor.getType().cast(original.overlay(lookup, (Overlay)value)));
                  } else {
                    if (equivalentToNull(lookup, fieldAccessor, value)) {
                      fieldAccessor.set(this, null);
                    } else {
                      fieldAccessor.set(this, value);
                    }
                  }
                } else {
                  if (equivalentToNull(lookup, fieldAccessor, value)) {
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

  /**
   * Inspects the field's annotations for any {@link OverlayNullifier} meta-annotation and, if found,
   * delegates to the corresponding validator to determine whether {@code value} should become {@code null}.
   *
   * @param lookup        the lookup used to construct the validator
   * @param fieldAccessor the accessor describing the field whose annotations should be checked
   * @param value         the non-null value read from the overlay source
   * @return {@code true} if a validator considers the value equivalent to {@code null}
   * @throws OverlayException if the validator cannot be constructed
   */
  private boolean equivalentToNull (MethodHandles.Lookup lookup, FieldAccessor fieldAccessor, Object value)
    throws OverlayException {

    for (Annotation fieldAnnotation : fieldAccessor.getField().getAnnotations()) {

      OverlayNullifier overlayNullifier;

      if ((overlayNullifier = fieldAnnotation.annotationType().getAnnotation(OverlayNullifier.class)) != null) {

        return internalEquivalentToNull(lookup, overlayNullifier, fieldAnnotation, value);
      }
    }

    return false;
  }

  /**
   * Instantiates the {@link OverlayNullifierValidator} specified by {@code overlayNullifier} through its
   * no-argument constructor, reached with the access of {@code lookup}, initialises it with the field
   * annotation, and asks it whether {@code object} is equivalent to {@code null}.
   *
   * @param lookup           the lookup used to construct the validator
   * @param overlayNullifier the meta-annotation identifying the validator class to use
   * @param annotation       the concrete annotation instance on the field, passed to the validator's initialiser
   * @param object           the value to be evaluated
   * @param <A>              the type of the field annotation
   * @param <T>              the type of the field value
   * @return {@code true} if the validator reports the value as equivalent to {@code null}
   * @throws OverlayException if the validator cannot be constructed
   */
  private <A extends Annotation, T> boolean internalEquivalentToNull (MethodHandles.Lookup lookup, OverlayNullifier overlayNullifier, A annotation, T object)
    throws OverlayException {

    OverlayNullifierValidator<A, T> overlayNullifierValidator;

    try {
      FieldUtility.ensureReadable(lookup, overlayNullifier.validatedBy().getModule());
      overlayNullifierValidator = (OverlayNullifierValidator<A, T>)lookup.findConstructor(overlayNullifier.validatedBy(), MethodType.methodType(void.class)).invoke();
    } catch (Error error) {
      throw error;
    } catch (Throwable throwable) {
      throw new OverlayException(throwable);
    }

    overlayNullifierValidator.initialize(annotation);

    return overlayNullifierValidator.equivalentToNull(object);
  }
}
