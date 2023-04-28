package org.smallmind.sso.oauth.spi;

public enum ResponseType {

  CODE("code"),
  TOKEN("token");

  private final String code;

  ResponseType (String code) {

    this.code = code;
  }

  public String getCode () {

    return code;
  }
}
