package org.smallmind.sso.oauth;

public enum AuthorizationGrantType {

  AUTHORIZATION_CODE("authorization code"),
  IMPLICIT("implicit"),
  RESOURCE_OWNER_CREDENTIALS("resource_owner_password_credentials"),
  CLIENT_CREDENTIALS("client_credentials");

  private final String code;

  AuthorizationGrantType (String code) {

    this.code = code;
  }

  public String getCode () {

    return code;
  }
}
