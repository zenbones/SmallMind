package org.smallmind.license.stencil;

public class JavaDocStencil extends StaticStencil {

  @Override
  public String getSkipLinePattern () {

    return null;
  }

  @Override
  public String getFirstLine () {

    return "/*";
  }

  @Override
  public String getLastLine () {

    return " */";
  }

  @Override
  public String getLinePrefix () {

    return " * ";
  }

  @Override
  public String getBlankLinePrefix () {

    return " *";
  }

  @Override
  public int getBlankLinesBefore () {

    return 0;
  }

  @Override
  public int getBlankLinesAfter () {

    return 0;
  }
}
