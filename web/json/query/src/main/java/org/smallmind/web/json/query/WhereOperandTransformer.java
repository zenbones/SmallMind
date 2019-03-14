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
package org.smallmind.web.json.query;

import java.lang.reflect.Array;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import org.smallmind.persistence.orm.ORMOperationException;

public class WhereOperandTransformer {

  private static final WhereOperandTransformer DEFAULT_TRANSFORMER = new WhereOperandTransformer();
  private HashMap<TransformKey<?, ?>, WhereOperandTransform<?, ?>> transformMap = new HashMap<>();

  public WhereOperandTransformer () {

    transformMap.put(new TransformKey<>(ZonedDateTime.class, Date.class), (String typeHint, ZonedDateTime zonedDateTime) -> Date.from(zonedDateTime.toInstant()));
    transformMap.put(new TransformKey<>(String.class, Enum.class), (String typeHint, String name) -> {
      throw new ORMOperationException("Missing transform for enum(%s)", typeHint);
    });
  }

  public static WhereOperandTransformer instance () {

    return DEFAULT_TRANSFORMER;
  }

  public synchronized <I, O> WhereOperandTransformer add (Class<I> inputClass, Class<O> outputClass, WhereOperandTransform<I, O> transform) {

    transformMap.put(new TransformKey<>(inputClass, outputClass), transform);

    return this;
  }

  public synchronized <I, O> O transform (WhereOperand<I, O> whereOperand) {

    I input;

    if ((input = whereOperand.getValue()) == null) {
      return null;
    } else {

      Class<? extends O> clazz = whereOperand.getTargetClass();

      if (clazz.isAssignableFrom(input.getClass())) {

        return clazz.cast(input);
      } else if (clazz.isArray()) {
        if (!input.getClass().isArray()) {
          throw new ORMOperationException("Attempt to apply non-array type(%s) into array type(%s)", input.getClass().getName(), clazz.getName());
        } else {

          WhereOperandTransform<Object, ?> transform;

          if ((transform = (WhereOperandTransform<Object, ?>)transformMap.get(new TransformKey<>(input.getClass().getComponentType(), clazz.getComponentType()))) == null) {
            throw new ORMOperationException("Missing transform from input type(%s) to output(%s)", input.getClass().getComponentType(), clazz.getComponentType());
          } else {

            Object[] output;
            int arrayLength = Array.getLength(input);

            output = (Object[])Array.newInstance(clazz.getComponentType(), arrayLength);

            for (int index = 0; index < arrayLength; index++) {
              output[index] = transform.apply(whereOperand.getTypeHint(), Array.get(input, index));
            }

            return clazz.cast(output);
          }
        }
      } else {

        WhereOperandTransform<I, O> transform;

        if ((transform = (WhereOperandTransform<I, O>)transformMap.get(new TransformKey<>(input.getClass(), clazz))) == null) {
          throw new ORMOperationException("Missing transform from input type(%s) to output(%s)", input.getClass(), clazz);
        } else {

          return transform.apply(whereOperand.getTypeHint(), input);
        }
      }
    }
  }

  private class TransformKey<T, U> {

    private Class<T> inputClass;
    private Class<U> outputClass;

    private TransformKey (Class<T> inputClass, Class<U> outputClass) {

      this.inputClass = inputClass;
      this.outputClass = outputClass;
    }

    private Class<T> getInputClass () {

      return inputClass;
    }

    private Class<U> getOutputClass () {

      return outputClass;
    }

    @Override
    public int hashCode () {

      return inputClass.hashCode() ^ outputClass.hashCode();
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof TransformKey) && inputClass.equals(((TransformKey)obj).getInputClass()) && outputClass.equals(((TransformKey)obj).getOutputClass());
    }
  }
}
