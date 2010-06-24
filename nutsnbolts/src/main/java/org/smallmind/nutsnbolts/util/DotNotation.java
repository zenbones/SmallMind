package org.smallmind.nutsnbolts.util;

import java.util.regex.Pattern;

public class DotNotation {

   private static enum TranslationState {

      START, POST_DOT, WILD, NORMAL
   }

   private static enum WildState {

      STAR, QUESTION, TAME
   }

   private Pattern pattern;
   private String notation;

   public DotNotation (String notation)
      throws DotNotationException {

      this.notation = notation;

      pattern = Pattern.compile(validateAsRegEx(notation));
   }

   public String getNotation () {

      return notation;
   }

   public Pattern getPattern () {

      return pattern;
   }

   public static String validateAsRegEx (String notation)
      throws DotNotationException {

      StringBuilder patternBuilder;
      TranslationState translationState;
      WildState wildState;
      char curChar;
      int count;

      patternBuilder = new StringBuilder();
      patternBuilder.append('^');
      translationState = TranslationState.START;
      wildState = WildState.TAME;

      for (count = 0; count < notation.length(); count++) {
         curChar = notation.charAt(count);
         switch (curChar) {
            case '.':
               if (translationState.equals(TranslationState.POST_DOT)) {
                  throw new DotNotationException("Empty component in the pattern");
               }
               else if (translationState.equals(TranslationState.START)) {
                  throw new DotNotationException("The pattern can not begin with '.'");
               }

               if (translationState.equals(TranslationState.NORMAL)) {
                  patternBuilder.append(')');
               }

               patternBuilder.append("\\.");
               translationState = TranslationState.POST_DOT;

               break;
            case '*':
               if (translationState.equals(TranslationState.WILD)) {
                  throw new DotNotationException("Wildcards must either be followed by '.' or terminate the pattern");
               }
               else if (translationState.equals(TranslationState.NORMAL)) {
                  throw new DotNotationException("Wildcards must either start the pattern or be preceded by '.'");
               }
               else if (!wildState.equals(WildState.TAME)) {
                  throw new DotNotationException("Any wildcard followed by '*' is redundant");
               }

               patternBuilder.append(".+");
               translationState = TranslationState.WILD;
               wildState = WildState.STAR;

               break;
            case '?':
               if (translationState.equals(TranslationState.WILD)) {
                  throw new DotNotationException("Wildcards must either be followed by '.' or terminate the pattern");
               }
               else if (translationState.equals(TranslationState.NORMAL)) {
                  throw new DotNotationException("Wildcards must either start the pattern or be preceded by '.'");
               }
               else if (wildState.equals(WildState.STAR)) {
                  throw new DotNotationException("Following '*' with '?' is redundant");
               }

               patternBuilder.append("[^.]+");
               translationState = TranslationState.WILD;
               wildState = WildState.QUESTION;

               break;
            default:
               if (!Character.isJavaIdentifierPart(curChar)) {
                  throw new DotNotationException("Components must be composed of valid Java identifiers");
               }
               else if (translationState.equals(TranslationState.WILD)) {
                  throw new DotNotationException("Wildcards must either be followed by '.' or terminate the pattern");
               }

               if ((translationState.equals(TranslationState.POST_DOT)) || (translationState.equals(TranslationState.START))) {
                  patternBuilder.append('(');
               }

               patternBuilder.append(curChar);
               translationState = TranslationState.NORMAL;
               wildState = WildState.TAME;
         }
      }

      if (translationState.equals(TranslationState.POST_DOT)) {
         throw new DotNotationException("The pattern can not end with '.'");
      }
      else if (translationState.equals(TranslationState.NORMAL)) {
         patternBuilder.append(')');
      }

      patternBuilder.append('$');

      return patternBuilder.toString();
   }
}