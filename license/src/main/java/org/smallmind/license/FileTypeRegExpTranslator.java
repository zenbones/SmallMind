package org.smallmind.license;

public class FileTypeRegExpTranslator {

   public static String translate (String pattern) {

      StringBuilder patternBuilder;
      char curChar;
      int index;

      patternBuilder = new StringBuilder();
      for (index = 0; index < pattern.length(); index++) {
         curChar = pattern.charAt(index);
         switch (curChar) {
            case '$':
               patternBuilder.append("\\$");
               break;
            case '.':
               patternBuilder.append("\\.");
               break;
            case '*':
               patternBuilder.append(".*");
               break;
            case '?':
               patternBuilder.append(".?");
               break;
            default:
               patternBuilder.append(curChar);
         }
      }

      return patternBuilder.toString();
   }

}
