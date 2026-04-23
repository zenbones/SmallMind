package org.smallmind.license;

/**
 * Translates glob-style file name patterns into Java regular expressions suitable for use with
 * {@link java.util.regex.Pattern}.
 *
 * <p>The translation handles a small, fixed set of special characters: {@code $} and {@code .}
 * are escaped to their literal equivalents, {@code *} becomes {@code .*}, and {@code ?} becomes
 * {@code .?}. Every other character is copied verbatim, so the resulting string can be passed
 * directly to {@link java.util.regex.Pattern#compile(String)}.
 */
public class FileTypeRegExTranslator {

  /**
   * Converts a glob-style file name pattern into an equivalent regular expression string.
   *
   * @param pattern a file name pattern optionally containing {@code *} and {@code ?} wildcards;
   *                must not be {@code null}
   * @return a regular expression string that can be compiled via
   * {@link java.util.regex.Pattern#compile(String)}; never {@code null}
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
