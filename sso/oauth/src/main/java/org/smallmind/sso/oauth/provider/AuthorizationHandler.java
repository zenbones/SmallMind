package org.smallmind.sso.oauth.provider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.smallmind.nutsnbolts.http.Base64Codec;

public class AuthorizationHandler {

  public static UserAndPassword basic (String basicAuthorizationHeaderValue)
    throws IOException {

    if (!basicAuthorizationHeaderValue.startsWith("Basic ")) {
      throw new UnsupportedEncodingException("Expecting basic authorization header value to start with 'Basic '...");
    } else {

      String base64Decoded = new String(Base64Codec.decode(basicAuthorizationHeaderValue.substring(6)));
      int colonPos;

      if ((colonPos = base64Decoded.indexOf(':')) < 0) {
        throw new UnsupportedEncodingException("Expecting basic authorization header value to contain a ':' separator");
      } else {

        return new UserAndPassword(base64Decoded.substring(0, colonPos), base64Decoded.substring(colonPos + 1));
      }
    }
  }

  public static UserAndPassword formUrlEncoded (String parameterBlock)
    throws UnsupportedEncodingException {

    ParameterMap parameterMap = FormDecoder.decode(parameterBlock);
    String clientId;
    String clientSecret;

    if ((clientId = parameterMap.get("client_id")) == null) {
      throw new UnsupportedEncodingException("The form encoded credentials are missing 'client_id");
    } else if ((clientSecret = parameterMap.get("client_secret")) == null) {
      throw new UnsupportedEncodingException("The form encoded credentials are missing 'client_secret");
    } else {

      return new UserAndPassword(clientId, clientSecret);
    }
  }
}
