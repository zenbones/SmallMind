package org.smallmind.license;

/**
 * Utility for translating file type patterns containing wildcards into equivalent regular expressions that can be
 * applied to file names.
 */
public class FileTypeRegExTranslator {

  /**
   * Converts a glob-like file pattern into a Java regular expression, escaping regex meta characters and translating
   * wildcard symbols to their regex equivalents.
   *
   * @param pattern the file type pattern containing characters such as {@code *} and {@code ?}
   * @return the translated pattern that can be compiled as a {@link java.util.regex.Pattern}
   */
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
