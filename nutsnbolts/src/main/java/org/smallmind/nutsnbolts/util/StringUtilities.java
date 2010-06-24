package org.smallmind.nutsnbolts.util;

public class StringUtilities {

   public static String toDisplayCase (String anyCase) {

      return toDisplayCase(anyCase, '\0');
   }

   public static String toDisplayCase (String anyCase, char wordMarker) {

      StringBuilder displayBuilder;
      boolean newWord = true;

      displayBuilder = new StringBuilder();

      for (int count = 0; count < anyCase.length(); count++) {
         if (anyCase.charAt(count) == wordMarker) {
            newWord = true;
            displayBuilder.append(" ");
         }
         else if (newWord) {
            displayBuilder.append(Character.toUpperCase(anyCase.charAt(count)));
            newWord = false;
         }
         else {
            displayBuilder.append(Character.toLowerCase(anyCase.charAt(count)));
         }
      }

      return displayBuilder.toString();
   }

   public static String toCamelCase (String anyCase, char wordMarker) {

      StringBuilder camelBuilder;
      boolean upper = true;

      camelBuilder = new StringBuilder();

      for (int count = 0; count < anyCase.length(); count++) {
         if (anyCase.charAt(count) == wordMarker) {
            upper = true;
         }
         else if (upper) {
            camelBuilder.append(Character.toUpperCase(anyCase.charAt(count)));
            upper = false;
         }
         else {
            camelBuilder.append(Character.toLowerCase(anyCase.charAt(count)));
         }
      }

      return camelBuilder.toString();
   }

   public static String toStaticFieldName (String anyCase, char wordMarker) {

      StringBuilder fieldBuilder;

      fieldBuilder = new StringBuilder();

      for (int count = 0; count < anyCase.length(); count++) {
         if (anyCase.charAt(count) == wordMarker) {
            fieldBuilder.append('_');
         }
         else {
            fieldBuilder.append(Character.toUpperCase(anyCase.charAt(count)));
         }
      }

      return fieldBuilder.toString();
   }

   public static boolean isJavaIdentifier (String anyName) {

      for (int count = 0; count < anyName.length(); count++) {
         if (count == 0) {
            if (!Character.isJavaIdentifierStart(anyName.charAt(count))) {
               return false;
            }
         }
         else if (!Character.isJavaIdentifierPart(anyName.charAt(count))) {
            return false;
         }
      }

      return true;
   }

}
