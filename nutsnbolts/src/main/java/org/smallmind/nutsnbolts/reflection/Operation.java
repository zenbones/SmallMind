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
 * Represents a method signature by name and parameter type names.
 */
public class Operation {

  private final String operationName;
  private final String[] signatureNames;

  /**
   * Creates an operation representation from a reflected method.
   *
   * @param method the method to describe
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
   * Creates an operation representation from explicit values.
   *
   * @param operationName  the method name
   * @param signatureNames fully qualified parameter type names in declaration order
   */
  public Operation (String operationName, String[] signatureNames) {

    this.operationName = operationName;
    this.signatureNames = signatureNames;
  }

  /**
   * @return the name of the operation
   */
  public String getOperationName () {

    return operationName;
  }

  /**
   * @return the parameter type names for this operation
   */
  public String[] getSignatureNames () {

    return signatureNames;
  }

  /**
   * Computes a hash over the operation name and signature for use in hash collections.
   *
   * @return a hash code for this operation
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
   * Compares operations by name and ordered parameter type names.
   *
   * @param o the object to compare
   * @return {@code true} if the operations represent the same signature
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
