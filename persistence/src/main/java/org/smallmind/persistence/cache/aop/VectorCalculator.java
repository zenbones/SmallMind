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
package org.smallmind.persistence.cache.aop;

import org.aspectj.lang.JoinPoint;
import org.smallmind.nutsnbolts.reflection.aop.AOPUtility;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.VectorArtifact;
import org.smallmind.persistence.cache.VectorIndex;

/**
 * Utility that constructs {@link VectorArtifact} instances from {@link Vector} annotation metadata
 * combined with runtime values sourced from either a durable entity or an AOP join point.
 */
public class VectorCalculator {

  /**
   * Builds a {@link VectorArtifact} by reading key values from bean properties of the given durable.
   * Used by {@link CachedWithAspect} during persist and delete operations.
   *
   * @param vector  vector annotation that describes the key structure
   * @param durable entity from which property values are read
   * @return fully populated {@link VectorArtifact} ready for cache operations
   */
  public static VectorArtifact getVectorArtifact (Vector vector, Durable durable) {

    VectorIndex[] indices;

    indices = new VectorIndex[vector.value().length];
    for (int count = 0; count < vector.value().length; count++) {

      Object indexValue;

      indexValue = vector.value()[count].constant() ? vector.value()[count].value() : getValue(durable, vector.value()[count].value(), vector.value()[count].nullable());
      indices[count] = new VectorIndex(vector.value()[count].value(), indexValue, vector.value()[count].alias());
    }

    return new VectorArtifact(vector.namespace(), indices);
  }

  /**
   * Builds a {@link VectorArtifact} by reading key values from method arguments exposed by the join point.
   * Used by {@link CacheAsAspect} during annotated method interception.
   *
   * @param vector    vector annotation that describes the key structure
   * @param joinPoint the intercepted invocation that provides argument values
   * @return fully populated {@link VectorArtifact} ready for cache operations
   */
  public static VectorArtifact getVectorArtifact (Vector vector, JoinPoint joinPoint) {

    VectorIndex[] indices;

    indices = new VectorIndex[vector.value().length];
    for (int count = 0; count < vector.value().length; count++) {

      Object indexValue;

      indexValue = vector.value()[count].constant() ? vector.value()[count].value() : getValue(joinPoint, vector.value()[count].value(), vector.value()[count].nullable());
      indices[count] = new VectorIndex(vector.value()[count].value(), indexValue, vector.value()[count].alias());
    }

    return new VectorArtifact((vector.namespace().length() > 0) ? vector.namespace() : joinPoint.getSignature().getName(), indices);
  }

  /**
   * Extracts the named parameter value from a join point, throwing a {@link CacheAutomationError} if extraction fails.
   *
   * @param joinPoint     the intercepted invocation that contains the arguments
   * @param parameterName the name of the parameter to extract
   * @param nullable      {@code true} to permit a {@code null} value for this parameter
   * @return the runtime argument value for the named parameter
   */
  public static Object getValue (JoinPoint joinPoint, String parameterName, boolean nullable) {

    try {
      return AOPUtility.getParameterValue(joinPoint, parameterName, nullable);
    } catch (Exception exception) {
      throw new CacheAutomationError(exception);
    }
  }

  /**
   * Reads the named bean property from a durable entity, throwing a {@link CacheAutomationError} if access fails.
   *
   * @param durable   the entity from which the property is read
   * @param fieldName the bean-property name to access
   * @param nullable  {@code true} to permit a {@code null} property value
   * @return the property value from the durable
   */
  public static Object getValue (Durable durable, String fieldName, boolean nullable) {

    try {

      return BeanUtility.executeGet(durable, fieldName, nullable);
    } catch (Exception exception) {
      throw new CacheAutomationError(exception);
    }
  }
}
