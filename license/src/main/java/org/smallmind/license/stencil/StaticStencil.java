package org.smallmind.license.stencil;

/**
 * Base class for stencils whose formatting configuration is fixed at compile time.
 *
 * <p>Subclasses override the getter methods inherited from {@link Stencil} to return hard-coded
 * values. Every setter is overridden here to throw {@link UnsupportedOperationException},
 * preventing runtime mutation. The stencil id is derived automatically from
 * {@link Class#getName()} and is likewise immutable.
 */
public class StaticStencil extends Stencil {

  /**
   * Returns the fully-qualified class name of this stencil as its immutable identifier.
   *
   * @return the stencil id derived from {@link Class#getName()}; never {@code null}
   */
  @Override
  public final String getId () {

    return this.getClass().getName();
  }

  /**
   * Always throws {@link UnsupportedOperationException} because static stencil ids are immutable.
   *
   * @param id ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setId (String id) {

    throw new UnsupportedOperationException();
  }

  /**
   * Always throws {@link UnsupportedOperationException} because static stencil formatting is
   * immutable.
   *
   * @param skipLinePattern ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setSkipLinePattern (String skipLinePattern) {

    throw new UnsupportedOperationException();
  }

  /**
   * Always throws {@link UnsupportedOperationException} because static stencil formatting is
   * immutable.
   *
   * @param firstLine ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setFirstLine (String firstLine) {

    throw new UnsupportedOperationException();
  }

  /**
   * Always throws {@link UnsupportedOperationException} because static stencil formatting is
   * immutable.
   *
   * @param lastLine ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setLastLine (String lastLine) {

    throw new UnsupportedOperationException();
  }

  /**
   * Always throws {@link UnsupportedOperationException} because static stencil formatting is
   * immutable.
   *
   * @param linePrefix ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setLinePrefix (String linePrefix) {

    throw new UnsupportedOperationException();
  }

  /**
   * Always throws {@link UnsupportedOperationException} because static stencil formatting is
   * immutable.
   *
   * @param blankLinePrefix ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setBlankLinePrefix (String blankLinePrefix) {

    throw new UnsupportedOperationException();
  }

  /**
   * Always throws {@link UnsupportedOperationException} because static stencil formatting is
   * immutable.
   *
   * @param blankLinesBefore ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setBlankLinesBefore (int blankLinesBefore) {

    throw new UnsupportedOperationException();
  }

  /**
   * Always throws {@link UnsupportedOperationException} because static stencil formatting is
   * immutable.
   *
   * @param blankLinesAfter ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setBlankLinesAfter (int blankLinesAfter) {

    throw new UnsupportedOperationException();
  }
}
