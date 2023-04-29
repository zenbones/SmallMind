package org.smallmind.sso.oauth.spi.server;

import java.util.concurrent.ThreadLocalRandom;

public class AuthorizationCode {

  private final String clientId;
  private final String requestUri;
  private final String scope;
  private final byte[] nonce;
  private final long timestamp;

  public AuthorizationCode (String clientId, String requestUri, String scope) {

    this.clientId = clientId;
    this.requestUri = requestUri;
    this.scope = scope;

    nonce = new byte[4];
    ThreadLocalRandom.current().nextBytes(nonce);

    timestamp = System.currentTimeMillis();
  }
}
