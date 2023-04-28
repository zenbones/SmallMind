package org.smallmind.sso.oauth.spi.server;

import org.smallmind.sso.oauth.spi.ResponseType;

public class AuthorizationRequest {

  private final ResponseType responseType;
  private final String clientId;
  private final String redirectUri;
  private final String scope;

  public AuthorizationRequest (ResponseType responseType, String clientId, String redirectUri, String scope) {

    this.responseType = responseType;
    this.clientId = clientId;
    this.redirectUri = redirectUri;
    this.scope = scope;
  }

  public ResponseType getResponseType () {

    return responseType;
  }

  public String getClientId () {

    return clientId;
  }

  public String getRedirectUri () {

    return redirectUri;
  }

  public String getScope () {

    return scope;
  }
}
