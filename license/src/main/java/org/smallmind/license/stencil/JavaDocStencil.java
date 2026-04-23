package org.smallmind.license.stencil;

/**
 * Pre-configured {@link StaticStencil} that wraps notice text in a standard JavaDoc comment block.
 *
 * <p>The stencil produces the following layout:
 * <pre>
 * /*
 *  * &lt;notice line 1&gt;
 *  * &lt;notice line 2&gt;
 *  *&#47;
 * </pre>
 * Blank lines inside the notice are rendered as {@code " *"} to preserve the decorated style.
 * No additional blank lines are inserted before or after the block.
 */
public class JavaDocStencil extends StaticStencil {

  /**
   * Returns {@code null}; JavaDoc stencils do not skip any leading file lines.
   *
   * @return always {@code null}
   */
  @Override
  public String getSkipLinePattern () {

    return null;
  }

  /**
   * Returns the JavaDoc opening delimiter.
   *
   * @return {@code "/*"}
   */
  @Override
  public String getFirstLine () {

    return "/*";
  }

  /**
   * Returns the JavaDoc closing delimiter.
   *
   * @return {@code " *\/"}
   */
  @Override
  public String getLastLine () {

    return " */";
  }

  /**
   * Returns the prefix prepended to each non-blank notice line.
   *
   * @return {@code " * "}
   */
  @Override
  public String getLinePrefix () {

    return " * ";
  }

  /**
   * Returns the string written in place of blank notice lines to preserve the decorated style.
   *
   * @return {@code " *"}
   */
  @Override
  public String getBlankLinePrefix () {

    return " *";
  }

  /**
   * Returns zero; no blank lines are written before the JavaDoc block.
   *
   * @return {@code 0}
   */
  @Override
  public int getBlankLinesBefore () {

    return 0;
  }

  /**
   * Returns zero; no blank lines are written after the JavaDoc block.
   *
   * @return {@code 0}
   */
  @Override
  public int getBlankLinesAfter () {

    return 0;
  }
}
