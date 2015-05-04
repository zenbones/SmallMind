/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.oauth;

import org.smallmind.nutsnbolts.http.HTTPCodec;
import org.smallmind.nutsnbolts.util.Tuple;
import org.smallmind.web.oauth.v1.MungedCodec;

public class ClientAccessTokenFromCodeRequest {

  private String code;
  private String grantType;
  private String clientId;
  private String redirectUri;
  private String clientSecret;

  private ClientAccessTokenFromCodeRequest () {

  }

  public static ClientAccessTokenFromCodeRequest instance () {

    return new ClientAccessTokenFromCodeRequest();
  }

  public ClientAccessTokenFromCodeRequest setCode (String code) {

    this.code = code;

    return this;
  }

  public ClientAccessTokenFromCodeRequest setGrantType (String grantType) {

    this.grantType = grantType;

    return this;
  }

  public ClientAccessTokenFromCodeRequest setClientId (String clientId) {

    this.clientId = clientId;

    return this;
  }

  public ClientAccessTokenFromCodeRequest setRedirectUri (String redirectUri) {

    this.redirectUri = redirectUri;

    return this;
  }

  public ClientAccessTokenFromCodeRequest setClientSecret (String clientSecret)
    throws Exception {

    this.clientSecret = MungedCodec.encrypt(clientSecret);

    return this;
  }

  public String build ()
    throws OAuthProtocolException {

    if ((code == null) || code.isEmpty()) {
      throw new MissingParameterException("missing code");
    }
    if ((grantType == null) || grantType.isEmpty()) {
      throw new MissingParameterException("missing grant type");
    }
    if ((clientId == null) || clientId.isEmpty()) {
      throw new MissingParameterException("missing client id");
    }
    if ((redirectUri == null) || redirectUri.isEmpty()) {
      throw new MissingParameterException("missing redirect uri");
    }

    Tuple<String, String> paramTuple = new Tuple<>();

    paramTuple.addPair("code", code);
    paramTuple.addPair("grant_type", grantType);
    paramTuple.addPair("client_id", clientId);
    paramTuple.addPair("redirect_uri", redirectUri);

    if ((clientSecret != null) && (!clientSecret.isEmpty())) {
      paramTuple.addPair("client_secret", clientSecret);
    }

    return HTTPCodec.urlEncode(paramTuple);
  }
}