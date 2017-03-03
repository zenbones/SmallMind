/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.nutsnbolts.overlay;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.nutsnbolts.reflection.type.UnexpectedGenericDeclaration;

public abstract class Overlay<O extends Overlay<O>> implements Differentiable<O> {

  private static final ConcurrentHashMap<Class<? extends Overlay>, HashSet<Field>> CLASS_FIELD_MAP = new ConcurrentHashMap<>();

  private final Class<? extends Overlay> overlayClass;

  public Overlay () {

    List<Class<?>> typeArguments = GenericUtility.getTypeArguments(Overlay.class, this.getClass());

    if (typeArguments.size() != 1) {
      throw new UnexpectedGenericDeclaration("Expecting a single generic type");
    } else {

      Class<?> genericClass = typeArguments.get(0);

      if (!Overlay.class.isAssignableFrom(genericClass)) {
        throw new UnexpectedGenericDeclaration("Expecting a single generic type extending %s", Overlay.class.getSimpleName());
      } else {
        overlayClass = (Class<? extends Overlay>)genericClass;

        if (!CLASS_FIELD_MAP.containsKey(overlayClass)) {

          synchronized (CLASS_FIELD_MAP) {
            if (!CLASS_FIELD_MAP.containsKey(overlayClass)) {

              HashSet<Field> fieldSet = new HashSet<>();

              for (Field field : genericClass.getDeclaredFields()) {
                if (!(field.isSynthetic() || Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))) {
                  field.setAccessible(true);
                  fieldSet.add(field);
                }
              }

              CLASS_FIELD_MAP.put(overlayClass, fieldSet);
            }
          }
        }
      }
    }
  }

  public Class<? extends Overlay> getOverlayClass () {

    return overlayClass;
  }

  public O overlay (O... overlays)
    throws IllegalAccessException {

    if ((overlays != null) && (overlays.length > 0)) {
      for (O overlay : overlays) {
        if (!overlayClass.isAssignableFrom(overlay.getClass())) {
          throw new TypeMismatchException("Overlays must be of matching type(%s)", overlayClass);
        }
        for (Field field : CLASS_FIELD_MAP.get(overlayClass)) {

          Object value;

          if ((value = field.get(overlay)) != null) {
            field.set(this, value);
          }
        }
      }
    }

    return (O)this;
  }
}
