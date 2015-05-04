/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.oauth;

public class ServerAccessTokenResponse {

  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private String expiresIn;
  private String scope;

  private ServerAccessTokenResponse () {

  }

  public static ServerAccessTokenResponse instance () {

    return new ServerAccessTokenResponse();
  }

  public ServerAccessTokenResponse setAccessToken (String accessToken) {

    this.accessToken = accessToken;

    return this;
  }

  public ServerAccessTokenResponse setRefreshToken (String refreshToken) {

    this.refreshToken = refreshToken;

    return this;
  }

  public ServerAccessTokenResponse setTokenType (String tokenType) {

    this.tokenType = tokenType;

    return this;
  }

  public ServerAccessTokenResponse setExpiresIn (String expiresIn) {

    this.expiresIn = expiresIn;

    return this;
  }

  public ServerAccessTokenResponse setScope (String scope) {

    this.scope = scope;

    return this;
  }

  public String build ()
    throws OAuthProtocolException {

    if ((accessToken == null) || accessToken.isEmpty()) {
      throw new MissingParameterException("missing access token");
    }
    if ((tokenType == null) || tokenType.isEmpty()) {
      throw new MissingParameterException("missing token type");
    }

    StringBuilder jsonBuilder = new StringBuilder("{\"access_token\": \"").append(accessToken).append("\", \"token_type\": \"").append(tokenType).append('"');

    if ((refreshToken != null) && (!refreshToken.isEmpty())) {
      jsonBuilder.append(", \"refresh_token\": \"").append(refreshToken).append('"');
    }
    if ((expiresIn != null) && (!expiresIn.isEmpty())) {
      jsonBuilder.append(", \"expires_in\": \"").append(expiresIn).append('"');
    }
    if ((scope != null) && (!scope.isEmpty())) {
      jsonBuilder.append(", \"scope\": \"").append(scope).append('"');
    }

    return jsonBuilder.append('}').toString();
  }
}