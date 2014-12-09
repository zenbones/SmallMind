/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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

import org.smallmind.nutsnbolts.util.Tuple;

public class ServerAuthorizationRedirectResponse {

  private String redirectUri;
  private String code;
  private String state;

  private ServerAuthorizationRedirectResponse (String redirectUri) {

    if ((redirectUri == null) || redirectUri.isEmpty()) {
      throw new NullPointerException("redirect uri");
    }

    this.redirectUri = redirectUri;
  }

  public static ServerAuthorizationRedirectResponse redirectUri (String redirectUri) {

    return new ServerAuthorizationRedirectResponse(redirectUri);
  }

  public ServerAuthorizationRedirectResponse setCode (String code) {

    this.code = code;

    return this;
  }

  public ServerAuthorizationRedirectResponse setState (String state) {

    this.state = state;

    return this;
  }

  public String build ()
    throws OAuthProtocolException {

    if ((code == null) || code.isEmpty()) {
      throw new MissingParameterException("missing code");
    }

    Tuple<String, String> paramTuple = new Tuple<>();

    paramTuple.addPair("code", code);

    if ((state != null) && (!state.isEmpty())) {
      paramTuple.addPair("state", state);
    }

    return URIUtilities.composeWithQueryParameters(redirectUri, paramTuple);
  }
}