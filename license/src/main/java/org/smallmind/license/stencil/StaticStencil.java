package org.smallmind.license.stencil;

public class StaticStencil extends Stencil {

  @Override
  public final String getId () {

    return this.getClass().getName();
  }

  @Override
  public final void setId (String id) {

    throw new UnsupportedOperationException();
  }

  @Override
  public final void setSkipLinePattern (String skipLinePattern) {

    throw new UnsupportedOperationException();
  }

  @Override
  public final void setFirstLine (String firstLine) {

    throw new UnsupportedOperationException();
  }

  @Override
  public final void setLastLine (String lastLine) {

    throw new UnsupportedOperationException();
  }

  @Override
  public final void setLinePrefix (String linePrefix) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void setBlankLinePrefix (String blankLinePrefix) {

    throw new UnsupportedOperationException();
  }

  @Override
  public final void setBlankLinesBefore (int blankLinesBefore) {

    throw new UnsupportedOperationException();
  }

  @Override
  public final void setBlankLinesAfter (int blankLinesAfter) {

    throw new UnsupportedOperationException();
  }
}
