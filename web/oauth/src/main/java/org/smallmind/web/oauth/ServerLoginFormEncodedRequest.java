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

public class ServerLoginFormEncodedRequest {

  private String clientId;
  private String responseType;
  private String redirectUri;
  private String state;
  private String scope;
  private String authorizationUri;
  private String userName;

  public static ServerLoginFormEncodedRequest instance () {

    return new ServerLoginFormEncodedRequest();
  }

  public ServerLoginFormEncodedRequest setClientId (String clientId) {

    this.clientId = clientId;

    return this;
  }

  public ServerLoginFormEncodedRequest setResponseType (String responseType) {

    this.responseType = responseType;

    return this;
  }

  public ServerLoginFormEncodedRequest setRedirectUri (String redirectUri) {

    this.redirectUri = redirectUri;

    return this;
  }

  public ServerLoginFormEncodedRequest setState (String state) {

    this.state = state;

    return this;
  }

  public ServerLoginFormEncodedRequest setScope (String scope) {

    this.scope = scope;

    return this;
  }

  public ServerLoginFormEncodedRequest setAuthorizationUri (String authorizationUri) {

    this.authorizationUri = authorizationUri;

    return this;
  }

  public ServerLoginFormEncodedRequest setUserName (String userName) {

    this.userName = userName;

    return this;
  }

  public String build ()
    throws OAuthProtocolException {

    if ((clientId == null) || clientId.isEmpty()) {
      throw new MissingParameterException("missing client id");
    }
    if ((responseType == null) || responseType.isEmpty()) {
      throw new MissingParameterException("missing response type");
    }
    if ((authorizationUri == null) || authorizationUri.isEmpty()) {
      throw new MissingParameterException("missing authorization uri");
    }

    Tuple<String, String> paramTuple = new Tuple<>();

    paramTuple.addPair("client_id", clientId);
    paramTuple.addPair("response_type", responseType);
    paramTuple.addPair("authorization_uri", authorizationUri);

    if ((redirectUri != null) && (!redirectUri.isEmpty())) {
      paramTuple.addPair("redirect_uri", redirectUri);
    }
    if ((scope != null) && (!scope.isEmpty())) {
      paramTuple.addPair("scope", scope);
    }
    if ((state != null) && (!state.isEmpty())) {
      paramTuple.addPair("state", state);
    }
    if ((userName != null) && (!userName.isEmpty())) {
      paramTuple.addPair("user_name", userName);
    }

    return HTTPCodec.urlEncode(paramTuple);
  }
}