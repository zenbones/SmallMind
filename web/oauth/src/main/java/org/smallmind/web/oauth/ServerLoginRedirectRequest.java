/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.oauth;

import org.smallmind.nutsnbolts.util.Tuple;

public class ServerLoginRedirectRequest {

  private String loginUri;
  private String clientId;
  private String responseType;
  private String redirectUri;
  private String state;
  private String scope;
  private String authorizationUri;
  private String userName;

  private ServerLoginRedirectRequest (String loginUri) {

    if ((loginUri == null) || loginUri.isEmpty()) {
      throw new NullPointerException("login uri");
    }

    this.loginUri = loginUri;
  }

  public static ServerLoginRedirectRequest loginUri (String loginUri) {

    return new ServerLoginRedirectRequest(loginUri);
  }

  public ServerLoginRedirectRequest setClientId (String clientId) {

    this.clientId = clientId;

    return this;
  }

  public ServerLoginRedirectRequest setResponseType (String responseType) {

    this.responseType = responseType;

    return this;
  }

  public ServerLoginRedirectRequest setRedirectUri (String redirectUri) {

    this.redirectUri = redirectUri;

    return this;
  }

  public ServerLoginRedirectRequest setState (String state) {

    this.state = state;

    return this;
  }

  public ServerLoginRedirectRequest setScope (String scope) {

    this.scope = scope;

    return this;
  }

  public ServerLoginRedirectRequest setAuthorizationUri (String authorizationUri) {

    this.authorizationUri = authorizationUri;

    return this;
  }

  public ServerLoginRedirectRequest setUserName (String userName) {

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

    return URIUtility.composeWithQueryParameters(loginUri, paramTuple);
  }
}