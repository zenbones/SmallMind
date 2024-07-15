/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.sso.oauth.v2dot0.jersey;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.smallmind.sso.oauth.v2dot0.spi.InvalidClientIdException;
import org.smallmind.sso.oauth.v2dot0.spi.InvalidRedirectUriException;
import org.smallmind.sso.oauth.v2dot0.spi.MismatchingRedirectUriException;
import org.smallmind.sso.oauth.v2dot0.spi.MissingClientIdException;
import org.smallmind.sso.oauth.v2dot0.spi.MissingRedirectUriException;
import org.smallmind.sso.oauth.v2dot0.spi.server.grant.ConfirmationLoginResponse;
import org.smallmind.sso.oauth.v2dot0.spi.server.grant.RefusalLoginResponse;
import org.smallmind.web.json.scaffold.util.JsonCodec;

@Path("/sso/oauth")
public class OAuthResource {

  private IDPService idpService;

  public void setIdpService (IDPService idpService) {

    this.idpService = idpService;
  }

  @GET
  @Path("/authorization")
  public Response authorization (@QueryParam("response_type") String responseType,
                                 @QueryParam("client_id") String clientId,
                                 @QueryParam("redirect_uri") String redirectUri,
                                 @QueryParam("scope") String scope,
                                 @QueryParam("acr_values") String acrValues,
                                 @QueryParam("max_age") Integer maxAge,
                                 @QueryParam("state") String state)
    throws MissingClientIdException, InvalidClientIdException, MissingRedirectUriException, InvalidRedirectUriException, MismatchingRedirectUriException {

    return idpService.authorization(responseType, clientId, redirectUri, scope, acrValues, maxAge, state);
  }

  @POST
  @Path("/authentication/authorization_code/{code}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response code (@PathParam("code") String code,
                        @QueryParam("redirect_uri") String redirectUri,
                        @QueryParam("max_age") Integer maxAge,
                        @QueryParam("state") String state,
                        JsonNode body)
    throws MissingRedirectUriException {

    return idpService.code(code, redirectUri, maxAge, state, body.has("error") ? JsonCodec.convert(body, RefusalLoginResponse.class) : JsonCodec.convert(body, ConfirmationLoginResponse.class));
  }

  @POST
  @Path("/token")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response token (@HeaderParam("Authorization") String authorization,
                         @FormParam("grant_type") String grantType,
                         @FormParam("code") String code,
                         @FormParam("redirect_uri") String redirectUri,
                         @FormParam("client_id") String clientId,
                         @FormParam("client_secret") String clientSecret)
    throws IOException {

    return idpService.token(authorization, grantType, code, redirectUri, clientId, clientSecret);
  }
}
