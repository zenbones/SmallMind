package org.smallmind.nutsnbolts.http;

import org.smallmind.nutsnbolts.lang.SmallMindSystem;

public final class HTMLUtilities {

   public static String convertLineBreaks (String javaString) {

      StringBuilder htmlBuilder;

      htmlBuilder = new StringBuilder();
      for (int count = 0; count < javaString.length(); count++) {
         if (javaString.charAt(count) == '\n') {
            htmlBuilder.append("<br>");
         }
         else if (javaString.charAt(count) != '\r') {
            htmlBuilder.append(javaString.charAt(count));
         }
      }

      return htmlBuilder.toString();
   }

   public static String convertThrowable (Throwable throwable) {

      return convertLineBreaks(SmallMindSystem.getStackTrace(throwable));
   }

}
