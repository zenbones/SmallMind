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
