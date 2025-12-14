package org.smallmind.license.stencil;

/**
 * Stencil implementation whose formatting settings are immutable and derived from the concrete class.
 */
public class StaticStencil extends Stencil {

  /**
   * Returns the fully qualified class name as the immutable stencil identifier.
   *
   * @return the class name of the stencil implementation
   */
  @Override
  public final String getId () {

    return this.getClass().getName();
  }

  /**
   * Unsupported for static stencils because identifiers are fixed.
   *
   * @param id ignored
   * @throws UnsupportedOperationException always thrown to indicate immutability
   */
  @Override
  public final void setId (String id) {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported for static stencils because formatting is fixed.
   *
   * @param skipLinePattern ignored
   * @throws UnsupportedOperationException always thrown to indicate immutability
   */
  @Override
  public final void setSkipLinePattern (String skipLinePattern) {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported for static stencils because formatting is fixed.
   *
   * @param firstLine ignored
   * @throws UnsupportedOperationException always thrown to indicate immutability
   */
  @Override
  public final void setFirstLine (String firstLine) {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported for static stencils because formatting is fixed.
   *
   * @param lastLine ignored
   * @throws UnsupportedOperationException always thrown to indicate immutability
   */
  @Override
  public final void setLastLine (String lastLine) {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported for static stencils because formatting is fixed.
   *
   * @param linePrefix ignored
   * @throws UnsupportedOperationException always thrown to indicate immutability
   */
  @Override
  public final void setLinePrefix (String linePrefix) {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported for static stencils because formatting is fixed.
   *
   * @param blankLinePrefix ignored
   * @throws UnsupportedOperationException always thrown to indicate immutability
   */
  @Override
  public void setBlankLinePrefix (String blankLinePrefix) {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported for static stencils because formatting is fixed.
   *
   * @param blankLinesBefore ignored
   * @throws UnsupportedOperationException always thrown to indicate immutability
   */
  @Override
  public final void setBlankLinesBefore (int blankLinesBefore) {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported for static stencils because formatting is fixed.
   *
   * @param blankLinesAfter ignored
   * @throws UnsupportedOperationException always thrown to indicate immutability
   */
  @Override
  public final void setBlankLinesAfter (int blankLinesAfter) {

    throw new UnsupportedOperationException();
  }
}
