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

public class ClientAuthorizationRequest {

  private String locationUri;
  private String clientId;
  private String responseType;
  private String redirectUri;
  private String state;
  private String scope;

  private ClientAuthorizationRequest (String locationUri) {

    if ((locationUri == null) || locationUri.isEmpty()) {
      throw new NullPointerException("location uri");
    }

    this.locationUri = locationUri;
  }

  public static ClientAuthorizationRequest locationUri (String locationUri) {

    return new ClientAuthorizationRequest(locationUri);
  }

  public ClientAuthorizationRequest setClientId (String clientId) {

    this.clientId = clientId;

    return this;
  }

  public ClientAuthorizationRequest setResponseType (String responseType) {

    this.responseType = responseType;

    return this;
  }

  public ClientAuthorizationRequest setRedirectUri (String redirectUri) {

    this.redirectUri = redirectUri;

    return this;
  }

  public ClientAuthorizationRequest setState (String state) {

    this.state = state;

    return this;
  }

  public ClientAuthorizationRequest setScope (String scope) {

    this.scope = scope;

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

    Tuple<String, String> paramTuple = new Tuple<>();

    paramTuple.addPair("client_id", clientId);
    paramTuple.addPair("response_type", responseType);

    if ((redirectUri != null) && (!redirectUri.isEmpty())) {
      paramTuple.addPair("redirect_uri", redirectUri);
    }
    if ((scope != null) && (!scope.isEmpty())) {
      paramTuple.addPair("scope", scope);
    }
    if ((state != null) && (!state.isEmpty())) {
      paramTuple.addPair("state", state);
    }

    return locationUri + '?' + HTTPCodec.urlEncode(paramTuple);
  }
}