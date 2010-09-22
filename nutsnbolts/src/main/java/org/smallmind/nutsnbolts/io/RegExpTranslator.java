package org.smallmind.nutsnbolts.io;

public class RegExpTranslator {

   public static String translate (String pattern) {

      StringBuilder patternBuilder = new StringBuilder();
      boolean isStar = false;

      for (int index = 0; index < pattern.length(); index++) {
         if (isStar && (pattern.charAt(index) != '*')) {
            patternBuilder.append("[^/]*");
            isStar = false;
         }

         switch (pattern.charAt(index)) {
            case '$':
               patternBuilder.append("\\$");
            case '.':
               patternBuilder.append("\\.");
               break;
            case '*':
               if (isStar) {
                  patternBuilder.append(".*");
                  isStar = false;
               }
               else {
                  isStar = true;
               }
               break;
            case '?':
               patternBuilder.append("[^/]?");
               break;
            default:
               patternBuilder.append(pattern.charAt(index));
         }
      }

      if (isStar) {
         patternBuilder.append("[^/]*");
      }

      return patternBuilder.toString();
   }
}
