/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.sso.oauth.jersey;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.smallmind.sso.oauth.spi.InvalidClientIdException;
import org.smallmind.sso.oauth.spi.InvalidRedirectUriException;
import org.smallmind.sso.oauth.spi.MismatchingRedirectUriException;
import org.smallmind.sso.oauth.spi.MissingClientIdException;
import org.smallmind.sso.oauth.spi.MissingRedirectUriException;
import org.smallmind.sso.oauth.spi.ResponseType;
import org.smallmind.sso.oauth.spi.server.AuthorizationError;
import org.smallmind.sso.oauth.spi.server.AuthorizationErrorType;
import org.smallmind.sso.oauth.spi.server.AuthorizationHandler;
import org.smallmind.sso.oauth.spi.server.AuthorizationResponse;

@Path("")
public class JerseyOAuthResource {

  private AuthorizationHandler authorizationHandler;

  public void setAuthorizationHandler (AuthorizationHandler authorizationHandler) {

    this.authorizationHandler = authorizationHandler;
  }

  @GET
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public AuthorizationResponse authorization (@QueryParam("response_type") String responseType,
                                              @QueryParam("client_id") String clientId,
                                              @QueryParam("redirect_uri") String redirectUri,
                                              @QueryParam("scope") String scope,
                                              @QueryParam("state") String state)
    throws MissingClientIdException, InvalidClientIdException, MissingRedirectUriException, InvalidRedirectUriException, MismatchingRedirectUriException {

    AuthorizationError error;
    ResponseType decodedResponseType;

    if (clientId == null) {
      throw new MissingClientIdException();
    } else {

    }

    if (responseType == null) {
      error= new AuthorizationError(AuthorizationErrorType.INVALID_REQUEST, "Missing response_type");
    } else if ((decodedResponseType = ResponseType.fromCode(responseType)) == null) {
      error= new AuthorizationError(AuthorizationErrorType.INVALID_REQUEST, "Unknown response_type(%s)", responseType);
    } else {
    }

    return null;
  }
}
