package org.smallmind.sso.oauth.spi.server.repository;

public class CodeRegister {

  private final String clientId;
  private final String redirectUri;
  private final String scope;
  private final String acrValues;
  private final String state;

  public CodeRegister (String clientId, String redirectUri, String scope, String acrValues, String state) {

    this.clientId = clientId;
    this.redirectUri = redirectUri;
    this.scope = scope;
    this.acrValues = acrValues;
    this.state = state;
  }
}
