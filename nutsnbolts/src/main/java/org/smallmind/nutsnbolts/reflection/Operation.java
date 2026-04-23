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

import java.lang.reflect.Method;

/**
 * Captures a method's identity as a name plus an ordered array of fully-qualified parameter type names,
 * suitable for use as a map key or equality check across class loaders.
 */
public class Operation {

  private final String operationName;
  private final String[] signatureNames;

  /**
   * Constructs an operation from a reflected method, extracting its name and parameter type names.
   *
   * @param method the reflected method whose identity should be captured
   */
  public Operation (Method method) {

    Class[] signature;

    operationName = method.getName();

    signature = method.getParameterTypes();
    signatureNames = new String[signature.length];
    for (int count = 0; count < signatureNames.length; count++) {
      signatureNames[count] = signature[count].getName();
    }
  }

  /**
   * Constructs an operation from an explicit name and parameter type name array.
   *
   * @param operationName  the name of the method
   * @param signatureNames the fully qualified names of the method's parameters, in declaration order
   */
  public Operation (String operationName, String[] signatureNames) {

    this.operationName = operationName;
    this.signatureNames = signatureNames;
  }

  /**
   * Returns the method name captured by this operation.
   *
   * @return the method name
   */
  public String getOperationName () {

    return operationName;
  }

  /**
   * Returns the fully-qualified parameter type names for this operation in declaration order.
   *
   * @return array of parameter type names; empty when the method takes no parameters
   */
  public String[] getSignatureNames () {

    return signatureNames;
  }

  /**
   * Computes a hash code combining the operation name and all parameter type names.
   *
   * @return an integer hash code suitable for use in hash-based collections
   */
  public int hashCode () {

    int hashCode;

    hashCode = operationName.hashCode();
    for (String signatureName : signatureNames) {
      hashCode = hashCode ^ signatureName.hashCode();
    }

    return hashCode;
  }

  /**
   * Compares this operation to another object by method name and parameter type names in order.
   *
   * @param o the object to compare with this operation
   * @return {@code true} if {@code o} is an {@code Operation} with the same name and signature
   */
  public boolean equals (Object o) {

    if (o instanceof Operation) {
      if (operationName.equals(((Operation)o).getOperationName())) {
        if (signatureNames.length == ((Operation)o).getSignatureNames().length) {
          for (int count = 0; count < signatureNames.length; count++) {
            if (!signatureNames[count].equals(((Operation)o).getSignatureNames()[count])) {

              return false;
            }
          }

          return true;
        }
      }
    }

    return false;
  }
}
