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
package org.smallmind.mongodb.throng.mapping;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import org.smallmind.mongodb.throng.ThrongRuntimeException;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;

/**
 * Helper methods for resolving concrete types needed for codec generation.
 */
public class CodecUtility {

  /**
   * Attempts to resolve a generic field's concrete type by inspecting the parent class's type arguments.
   *
   * @param parentClass   entity class declaring the field
   * @param fieldAccessor reflection accessor for the field
   * @return the concrete class for the field
   * @throws ThrongRuntimeException if the type parameter cannot be resolved
   */
  public static Class<?> getReifiedType (Class<?> parentClass, FieldAccessor fieldAccessor) {

    Type genericType;

    if ((genericType = fieldAccessor.getGenericType()) instanceof TypeVariable) {

      Class<?> reifiedFieldType;

      if ((reifiedFieldType = GenericUtility.findTypeArgument(parentClass, (TypeVariable<?>)genericType)) == null) {
        throw new ThrongRuntimeException("Could not reify the type of field(%s) in entity(%s)", fieldAccessor.getName(), parentClass.getName());
      } else {

        return reifiedFieldType;
      }
    } else {

      return fieldAccessor.getType();
    }
  }
}
