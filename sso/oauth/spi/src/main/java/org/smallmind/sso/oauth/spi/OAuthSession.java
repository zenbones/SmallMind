package org.smallmind.sso.oauth.spi;

public class OAuthSession {

  private final String accessToken;
  private final String tokenType;
  private final String refreshToken;
  private final String scope;
  private final Integer expiresIn;

  public OAuthSession (String accessToken, String tokenType, String refreshToken, String scope, Integer expiresIn) {

    this.accessToken = accessToken;
    this.tokenType = tokenType;
    this.refreshToken = refreshToken;
    this.scope = scope;
    this.expiresIn = expiresIn;
  }

  public String getAccessToken () {

    return accessToken;
  }

  public String getTokenType () {

    return tokenType;
  }

  public String getRefreshToken () {

    return refreshToken;
  }

  public String getScope () {

    return scope;
  }

  public Integer getExpiresIn () {

    return expiresIn;
  }
}
