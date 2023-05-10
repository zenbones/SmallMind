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

import java.io.IOException;
import java.net.URI;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.sso.oauth.spi.InvalidClientIdException;
import org.smallmind.sso.oauth.spi.InvalidRedirectUriException;
import org.smallmind.sso.oauth.spi.MismatchingRedirectUriException;
import org.smallmind.sso.oauth.spi.MissingClientIdException;
import org.smallmind.sso.oauth.spi.MissingRedirectUriException;
import org.smallmind.sso.oauth.spi.ResponseType;
import org.smallmind.sso.oauth.spi.server.AuthorizationCycle;
import org.smallmind.sso.oauth.spi.server.AuthorizationErrorType;
import org.smallmind.sso.oauth.spi.server.AuthorizationHandler;
import org.smallmind.sso.oauth.spi.server.AuthorizationRequest;
import org.smallmind.sso.oauth.spi.server.CredentialsDecoder;
import org.smallmind.sso.oauth.spi.server.ErrorAuthorizationCycle;
import org.smallmind.sso.oauth.spi.server.ErrorTokenResponse;
import org.smallmind.sso.oauth.spi.server.LoginAuthorizationCycle;
import org.smallmind.sso.oauth.spi.server.UserAndPassword;
import org.smallmind.sso.oauth.spi.server.repository.CodeContent;
import org.smallmind.sso.oauth.spi.server.repository.CodeContentRepository;

@Path("/sso/oauth")
public class OAuthResource {

  private AuthorizationHandler authorizationHandler;
  private CodeContentRepository codeContentRepository;

  public void setAuthorizationHandler (AuthorizationHandler authorizationHandler) {

    this.authorizationHandler = authorizationHandler;
  }

  public void setCodeContentRepository (CodeContentRepository codeContentRepository) {

    this.codeContentRepository = codeContentRepository;
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

    if (clientId == null) {
      throw new MissingClientIdException();
    } else {

      ResponseType decodedResponseType = (responseType == null) ? null : ResponseType.fromCode(responseType);
      AuthorizationCycle authorizationCycle = authorizationHandler.validateAuthorizationRequest(new AuthorizationRequest(decodedResponseType, clientId, redirectUri, scope, acrValues));
      StringBuilder cycleResponseBuilder = authorizationCycle.formulateResponseUri();

      switch (authorizationCycle.getCycleType()) {
        case ERROR:

          if (state != null) {
            cycleResponseBuilder.append("&state=").append(state);
          }

          return Response.seeOther(URI.create(cycleResponseBuilder.toString())).build();
        case LOGIN:

          String code;

          codeContentRepository.put(code = SnowflakeId.newInstance().generateCompactString(), maxAge, ((LoginAuthorizationCycle)authorizationCycle).generateCodeContent(clientId, state, redirectUri));

          if (maxAge != null) {
            cycleResponseBuilder.append("&max_ge=").append(maxAge);
          }

          cycleResponseBuilder.append("&code=").append(code);

          return Response.seeOther(URI.create(cycleResponseBuilder.toString())).build();
        default:
          throw new UnknownSwitchCaseException(authorizationCycle.getCycleType().name());
      }
    }
  }

  @POST
  @Path("/confirm/{code}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response confirmation (@PathParam("code") String code, @QueryParam("redirect_uri") String redirectUri, @QueryParam("max_age") Integer maxAge, @QueryParam("state") String state, ConfirmationLoginResponse loginResponse)
    throws MissingRedirectUriException {

    return formulateCodeResponse(code, redirectUri, maxAge, state, loginResponse);
  }

  @POST
  @Path("/refuse/{code}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response refusal (@PathParam("code") String code, @QueryParam("redirect_uri") String redirectUri, @QueryParam("max_age") Integer maxAge, @QueryParam("state") String state, ConfirmationLoginResponse loginResponse)
    throws MissingRedirectUriException {

    return formulateCodeResponse(code, redirectUri, maxAge, state, loginResponse);
  }

  private Response formulateCodeResponse (String code, String redirectUri, Integer maxAge, String state, LoginResponse loginResponse)
    throws MissingRedirectUriException {

    CodeContent codeContent;

    if ((codeContent = codeContentRepository.get(code)) == null) {
      if ((redirectUri == null) || redirectUri.isBlank()) {
        throw new MissingRedirectUriException();
      } else {

        StringBuilder missingCodeResponseBuilder = new ErrorAuthorizationCycle(redirectUri, AuthorizationErrorType.INSUFFICIENT_USER_AUTHENTICATION, null, maxAge, "The authentication request exceeded the allowable maximum time(%s seconds)", (maxAge == null) ? "unknown" : String.valueOf(maxAge)).formulateResponseUri();

        if (state != null) {
          missingCodeResponseBuilder.append("&state=").append(state);
        }

        return Response.seeOther(URI.create(missingCodeResponseBuilder.toString())).build();
      }
    } else {
      switch (loginResponse.getResponseType()) {
        case REFUSAL:
          codeContentRepository.remove(code);

          return Response.seeOther(URI.create(((RefusalLoginResponse)loginResponse).formulateResponseUri(codeContent.getRedirectUri()))).build();
        case CONFIRMATION:
          codeContent.setSession(((ConfirmationLoginResponse)loginResponse).generateSession());

          return Response.seeOther(URI.create(codeContent.formulateResponseUri(code, ((ConfirmationLoginResponse)loginResponse).getScope()))).build();
        default:
          throw new UnknownSwitchCaseException(loginResponse.getResponseType().name());
      }
    }
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

    CodeContent codeContent;

    if ((codeContent = codeContentRepository.remove(code)) == null) {

      return Response.ok(new ErrorTokenResponse(AuthorizationErrorType.INSUFFICIENT_USER_AUTHENTICATION, "The token request exceeded the allowable maximum time(unknown seconds)").formulateResponseBody(), MediaType.APPLICATION_JSON_TYPE).build();
    } else {

      UserAndPassword userAndPassword = null;

      if (authorization != null) {
        userAndPassword = CredentialsDecoder.basic(authorization);
      } else if ((clientId != null) && (clientSecret != null)) {
        userAndPassword = new UserAndPassword(clientId, clientSecret);
      }

      if (!authorizationHandler.validateTokenRequest(userAndPassword)) {

        return Response.ok(new ErrorTokenResponse(AuthorizationErrorType.UNAUTHORIZED_CLIENT, "Missing client authorization").formulateResponseBody(), MediaType.APPLICATION_JSON_TYPE).build();
      } else if (((codeContent.getOriginalRedirectUri() == null) && (redirectUri != null)) || ((codeContent.getOriginalRedirectUri() != null) && (!codeContent.getRedirectUri().equals(redirectUri)))) {

        return Response.ok(new ErrorTokenResponse(AuthorizationErrorType.INVALID_REQUEST, "Mismatched request uri").formulateResponseBody(), MediaType.APPLICATION_JSON_TYPE).build();
      } else if (!"authorization_code".equals(grantType)) {

        return Response.ok(new ErrorTokenResponse(AuthorizationErrorType.INVALID_REQUEST, "Invalid grant type (must be 'authorization_code')").formulateResponseBody(), MediaType.APPLICATION_JSON_TYPE).build();
      } else {

        return Response.ok(codeContent.formulateResponseBody(), MediaType.APPLICATION_JSON_TYPE).build();
      }
    }
  }
}
