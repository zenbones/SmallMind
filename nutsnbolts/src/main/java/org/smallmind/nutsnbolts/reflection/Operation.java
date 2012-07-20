/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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

public class Operation {

  private String operationName;
  private String[] signatureNames;

  public Operation (Method method) {

    Class[] signature;

    operationName = method.getName();

    signature = method.getParameterTypes();
    signatureNames = new String[signature.length];
    for (int count = 0; count < signatureNames.length; count++) {
      signatureNames[count] = signature[count].getName();
    }
  }

  public Operation (String operationName, String[] signatureNames) {

    this.operationName = operationName;
    this.signatureNames = signatureNames;
  }

  public String getOperationName () {

    return operationName;
  }

  public String[] getSignatureNames () {

    return signatureNames;
  }

  public int hashCode () {

    int hashCode;

    hashCode = operationName.hashCode();
    for (String signatureName : signatureNames) {
      hashCode = hashCode ^ signatureName.hashCode();
    }

    return hashCode;
  }

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
