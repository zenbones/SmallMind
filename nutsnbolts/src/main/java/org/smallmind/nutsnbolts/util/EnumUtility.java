package org.smallmind.nutsnbolts.util;

public class EnumUtility {

  public static String toEnumName (String anyCase) {

    StringBuilder fieldBuilder = new StringBuilder();
    LetterState prevState = LetterState.NONE;
    int stateCount = 0;

    for (int count = 0; count < anyCase.length(); count++) {

      LetterState state;

      if (Character.isWhitespace(anyCase.charAt(count))) {
        state = LetterState.WHITESPACE;
      } else if (Character.isDigit(anyCase.charAt(count))) {
        state = LetterState.DIGIT;
      } else if (Character.isLetter(anyCase.charAt(count))) {
        state = Character.isUpperCase(anyCase.charAt(count)) ? LetterState.UPPER_LETTER : LetterState.LOWER_LETTER;
      } else {
        state = LetterState.OTHER;
      }

      if (!(state.equals(LetterState.WHITESPACE) || ((count > 0) && (anyCase.charAt(count) == '_') && (fieldBuilder.charAt(fieldBuilder.length() - 1) == '_')))) {
        if ((count > 0) && (!state.equals(prevState)) && (anyCase.charAt(count) != '_') && (fieldBuilder.charAt(fieldBuilder.length() - 1) != '_') && (!(prevState.equals(LetterState.UPPER_LETTER) && state.equals(LetterState.LOWER_LETTER)))) {
          fieldBuilder.append('_');
        }
        if (!state.equals(LetterState.OTHER)) {
          if (prevState.equals(LetterState.UPPER_LETTER) && state.equals(LetterState.LOWER_LETTER) && stateCount > 0) {
            fieldBuilder.insert(fieldBuilder.length() - 1, '_');
          }

          fieldBuilder.append(state.equals(LetterState.LOWER_LETTER) ? Character.toUpperCase(anyCase.charAt(count)) : anyCase.charAt(count));
        }
      }

      stateCount = prevState.equals(state) ? stateCount + 1 : 0;
      prevState = state;
    }

    return fieldBuilder.toString();
  }

  private static enum LetterState {NONE, DIGIT, UPPER_LETTER, LOWER_LETTER, WHITESPACE, OTHER}
}