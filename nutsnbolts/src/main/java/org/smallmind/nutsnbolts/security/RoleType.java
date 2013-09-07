package org.smallmind.nutsnbolts.security;

public enum RoleType {

  ADMIN("ADMIN");

  private String code;

  private RoleType (String code) {

    this.code = code;
  }

  public String getCode () {

    return code;
  }
}
