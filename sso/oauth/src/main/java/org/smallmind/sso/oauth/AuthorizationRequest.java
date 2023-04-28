package org.smallmind.sso.oauth;

//   .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
public class AuthorizationRequest {

  private ResponseType responseType; // required
  private String redirect_uri; // optional, must be compared to registered
}
