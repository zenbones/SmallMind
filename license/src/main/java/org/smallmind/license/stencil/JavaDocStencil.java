package org.smallmind.license.stencil;

/**
 * Static stencil that formats notices using JavaDoc-style comment delimiters and prefixes.
 */
public class JavaDocStencil extends StaticStencil {

  /**
   * JavaDoc stencils do not skip any lines when searching for an existing notice.
   *
   * @return always {@code null}
   */
  @Override
  public String getSkipLinePattern () {

    return null;
  }

  /**
   * Returns the starting delimiter for a JavaDoc comment.
   *
   * @return {@code "/*"}
   */
  @Override
  public String getFirstLine () {

    return "/*";
  }

  /**
   * Returns the closing delimiter for a JavaDoc comment.
   *
   * @return {@code " *\/"}
   */
  @Override
  public String getLastLine () {

    return " */";
  }

  /**
   * Returns the prefix applied to each JavaDoc line.
   *
   * @return {@code " * "}
   */
  @Override
  public String getLinePrefix () {

    return " * ";
  }

  /**
   * Returns the prefix written for blank JavaDoc lines.
   *
   * @return {@code " *"}
   */
  @Override
  public String getBlankLinePrefix () {

    return " *";
  }

  /**
   * JavaDoc stencils do not add blank lines before the notice content.
   *
   * @return always {@code 0}
   */
  @Override
  public int getBlankLinesBefore () {

    return 0;
  }

  /**
   * JavaDoc stencils do not add blank lines after the notice content.
   *
   * @return always {@code 0}
   */
  @Override
  public int getBlankLinesAfter () {

    return 0;
  }
}
