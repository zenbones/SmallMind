package org.smallmind.sso.oauth;

public enum ClientType {

  CONFIDENTIAL("confidential"), PUBLIC("public");

  private final String code;

  ClientType (String code) {

    this.code = code;
  }

  public String getCode () {

    return code;
  }
}
