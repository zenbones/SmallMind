package org.smallmind.license.stencil;

public class Stencil {

  private String id;
  private String skipLines;
  private String firstLine;
  private String lastLine;
  private String beforeEachLine;
  private boolean prefixBlankLines = false;
  private int blankLinesBefore;
  private int blankLinesAfter;

  public String getId () {

    return id;
  }

  public void setId (String id) {

    this.id = id;
  }

  public String getSkipLines () {

    return skipLines;
  }

  public void setSkipLines (String skipLines) {

    this.skipLines = skipLines;
  }

  public String getFirstLine () {

    return firstLine;
  }

  public void setFirstLine (String firstLine) {

    this.firstLine = firstLine;
  }

  public String getLastLine () {

    return lastLine;
  }

  public void setLastLine (String lastLine) {

    this.lastLine = lastLine;
  }

  public String getBeforeEachLine () {

    return beforeEachLine;
  }

  public void setBeforeEachLine (String beforeEachLine) {

    this.beforeEachLine = beforeEachLine;
  }

  public boolean willPrefixBlankLines () {

    return prefixBlankLines;
  }

  public void setPrefixBlankLines (boolean prefixBlankLines) {

    this.prefixBlankLines = prefixBlankLines;
  }

  public int getBlankLinesBefore () {

    return blankLinesBefore;
  }

  public void setBlankLinesBefore (int blankLinesBefore) {

    this.blankLinesBefore = blankLinesBefore;
  }

  public int getBlankLinesAfter () {

    return blankLinesAfter;
  }

  public void setBlankLinesAfter (int blankLinesAfter) {

    this.blankLinesAfter = blankLinesAfter;
  }
}