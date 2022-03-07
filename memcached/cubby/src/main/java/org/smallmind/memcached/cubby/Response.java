package org.smallmind.memcached.cubby;

public class Response {

  private ResponseCode code;
  private String token;
  private long cas;

  public Response (ResponseCode code) {

    this.code = code;
  }

  public Response setToken (String token) {

    this.token = token;

    return this;
  }

  public Response setCas (long cas) {

    this.cas = cas;

    return this;
  }
}
