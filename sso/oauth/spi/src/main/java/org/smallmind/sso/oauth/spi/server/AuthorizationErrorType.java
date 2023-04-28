package org.smallmind.sso.oauth.spi.server;

public enum AuthorizationErrorType {

  INVALID_REQUEST("invalid_request"),
  UNAUTHORIZED_CLIENT("unauthorized_client"),
  ACCESS_DENIED("access_denied"),
  UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type"),
  INVALID_SCOPE("invalid_scope"),
  SERVER_ERROR("server_error"),
  TEMPORARILY_UNAVAILABLE("temporarily_unavailable");

  private final String code;

  AuthorizationErrorType (String code) {

    this.code = code;
  }

  public String getCode () {

    return code;
  }
}
