package org.smallmind.web.jersey.data;

public enum Direction {

  IN("In"), OUT("Out");

  private String code;

  Direction (String code) {

    this.code = code;
  }

  public String getCode () {

    return code;
  }
}
