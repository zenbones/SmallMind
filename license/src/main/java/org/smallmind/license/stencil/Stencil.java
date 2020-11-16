package org.smallmind.license.stencil;

public class Stencil {

  private String id;
  private String skipLinePattern;
  private String firstLine;
  private String lastLine;
  private String linePrefix;
  private String blankLinePrefix;
  private int blankLinesBefore;
  private int blankLinesAfter;

  public String getId () {

    return id;
  }

  public void setId (String id) {

    this.id = id;
  }

  public String getSkipLinePattern () {

    return skipLinePattern;
  }

  public void setSkipLinePattern (String skipLinePattern) {

    this.skipLinePattern = skipLinePattern;
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

  public String getLinePrefix () {

    return linePrefix;
  }

  public void setLinePrefix (String linePrefix) {

    this.linePrefix = linePrefix;
  }

  public String getBlankLinePrefix () {

    return blankLinePrefix;
  }

  public void setBlankLinePrefix (String blankLinePrefix) {

    this.blankLinePrefix = blankLinePrefix;
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