/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.web.oauth.v1;

import java.net.URI;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import org.smallmind.nutsnbolts.http.HexCodec;
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.jwt.JWTCodec;
import org.smallmind.web.jwt.JWTToken;
import org.smallmind.web.jwt.SymmetricJWTKeyMaster;
import org.smallmind.web.oauth.GrantType;
import org.smallmind.web.oauth.OAuthProtocolException;
import org.smallmind.web.oauth.ResponseType;
import org.smallmind.web.oauth.ServerAccessTokenRequest;
import org.smallmind.web.oauth.ServerAccessTokenResponse;
import org.smallmind.web.oauth.ServerAuthorizationRedirectResponse;
import org.smallmind.web.oauth.ServerAuthorizationRequest;
import org.smallmind.web.oauth.ServerErrorJsonResponse;
import org.smallmind.web.oauth.ServerErrorRedirectResponse;
import org.smallmind.web.oauth.ServerLoginRedirectRequest;
import org.smallmind.web.oauth.TokenType;

@Path("/v1/oauth")
public class OAuthResource {

  private OAuthConfiguration oauthConfiguration;

  @Context
  private HttpServletRequest request;

  public void setOauthConfiguration (OAuthConfiguration oauthConfiguration) {

    this.oauthConfiguration = oauthConfiguration;
  }

  private Response.ResponseBuilder crossSiteAnoint (Response.ResponseBuilder responseBuilder) {

    return responseBuilder.header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Headers", "Origin, Content-Type, X-Requested-With");
  }

  @Path("/authorization")
  @GET
  @Produces(MediaType.APPLICATION_FORM_URLENCODED)
  public Response authorization () {

    ServerAuthorizationRequest serverAuthorizationRequest = null;

    try {

      serverAuthorizationRequest = new ServerAuthorizationRequest(request);
      OAuthRegistration oauthRegistration;

      if (!ResponseType.CODE.getParameter().equals(serverAuthorizationRequest.getResponseType())) {
        return Response.status(Response.Status.FOUND).location(URI.create(ServerErrorRedirectResponse.redirectUri(serverAuthorizationRequest.getRedirectUri()).setError("unsupported_response_type").setErrorDescription("only 'code' response types are supported").setState(serverAuthorizationRequest.getState()).build())).build();
      } else if ((oauthRegistration = oauthConfiguration.getRegistrationMap().get(serverAuthorizationRequest.getClientId())) == null) {
        return Response.status(Response.Status.FOUND).location(URI.create(ServerErrorRedirectResponse.redirectUri(serverAuthorizationRequest.getRedirectUri()).setError("unauthorized_client").setErrorDescription("unregistered client id").setState(serverAuthorizationRequest.getState()).build())).build();
      } else if ((!oauthRegistration.isUnsafeRedirection()) && (!oauthRegistration.getRedirectUri().equals(serverAuthorizationRequest.getRedirectUri()))) {
        return Response.status(Response.Status.FOUND).location(URI.create(ServerErrorRedirectResponse.redirectUri(serverAuthorizationRequest.getRedirectUri()).setError("unauthorized_client").setErrorDescription("mismatching redirect uri").setState(serverAuthorizationRequest.getState()).build())).build();
      }

      JWTToken jwtToken = null;
      SSOAuthData ssoAuthData = null;
      String cookieValue = null;
      String userName = null;

      if ((serverAuthorizationRequest.getAuthData() != null) && (!serverAuthorizationRequest.getAuthData().isEmpty())) {
        try {
          ssoAuthData = MungedCodec.decrypt(SSOAuthData.class, cookieValue = serverAuthorizationRequest.getAuthData());
        } catch (Exception exception) {
          LoggerManager.getLogger(OAuthResource.class).error(exception);

          return Response.status(Response.Status.FOUND).location(URI.create(ServerErrorRedirectResponse.redirectUri(serverAuthorizationRequest.getRedirectUri()).setError("server_error").setErrorDescription(exception.getMessage()).setState(serverAuthorizationRequest.getState()).build())).build();
        }
      } else {

        Cookie[] cookies;

        if (((cookies = request.getCookies()) != null) && (cookies.length > 0)) {
          for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(oauthConfiguration.getSsoCookieName())) {
              try {
                ssoAuthData = MungedCodec.decrypt(SSOAuthData.class, cookieValue = HexCodec.hexDecode(cookie.getValue()));
              } catch (Exception exception) {
                LoggerManager.getLogger(OAuthResource.class).error(exception);

                return Response.status(Response.Status.FOUND).location(URI.create(ServerErrorRedirectResponse.redirectUri(serverAuthorizationRequest.getRedirectUri()).setError("server_error").setErrorDescription(exception.getMessage()).setState(serverAuthorizationRequest.getState()).build())).build();
              }
              break;
            }
          }
        }
      }

      if (ssoAuthData != null) {
        if (System.currentTimeMillis() - ssoAuthData.getCreated() <= oauthConfiguration.getOauthProtocolLeaseDuration().toMilliseconds()) {
          try {
            jwtToken = oauthConfiguration.getSecretService().validate(userName = ssoAuthData.getUser(), ssoAuthData.getPassword());
          } catch (Exception exception) {
            LoggerManager.getLogger(OAuthResource.class).error(exception);

            return Response.status(Response.Status.FOUND).location(URI.create(ServerErrorRedirectResponse.redirectUri(serverAuthorizationRequest.getRedirectUri()).setError("server_error").setErrorDescription(exception.getMessage()).setState(serverAuthorizationRequest.getState()).build())).build();
          }
        }
      }

      if (jwtToken == null) {

        String loginUri;

        return Response.status(Response.Status.FOUND).location(URI.create(ServerLoginRedirectRequest.loginUri(((loginUri = serverAuthorizationRequest.getLoginUri()) != null) ? loginUri : oauthRegistration.getLoginUri()).setAuthorizationUri(oauthRegistration.getOauthUri()).setUserName(userName).setResponseType(serverAuthorizationRequest.getResponseType()).setClientId(serverAuthorizationRequest.getClientId()).setRedirectUri(serverAuthorizationRequest.getRedirectUri()).setState(serverAuthorizationRequest.getState()).build())).build();
      } else {

        String code;

        jwtToken.setSub(serverAuthorizationRequest.getClientId());
        jwtToken.setExp(System.currentTimeMillis() / 1000);

        try {
          code = JWTCodec.encode(jwtToken, new SymmetricJWTKeyMaster(oauthRegistration.getSecret()));
        } catch (Exception exception) {
          LoggerManager.getLogger(OAuthResource.class).error(exception);

          return Response.status(Response.Status.FOUND).location(URI.create(ServerErrorRedirectResponse.redirectUri(serverAuthorizationRequest.getRedirectUri()).setError("server_error").setErrorDescription(exception.getMessage()).setState(serverAuthorizationRequest.getState()).build())).build();
        }

        return Response.status(Response.Status.FOUND).cookie(new NewCookie(oauthConfiguration.getSsoCookieName(), HexCodec.hexEncode(cookieValue))).location(URI.create(ServerAuthorizationRedirectResponse.redirectUri(serverAuthorizationRequest.getRedirectUri()).setCode(code).setState(serverAuthorizationRequest.getState()).build())).build();
      }
    } catch (OAuthProtocolException oauthProtocolException) {
      LoggerManager.getLogger(OAuthResource.class).error(oauthProtocolException);

      if (serverAuthorizationRequest != null) {

        return Response.status(Response.Status.FOUND).location(URI.create(ServerErrorRedirectResponse.redirectUri(serverAuthorizationRequest.getRedirectUri()).setError("server_error").setErrorDescription(oauthProtocolException.getMessage()).setState(serverAuthorizationRequest.getState()).build())).build();
      }

      return null;
    }
  }

  @Path("/token")
  @OPTIONS
  public Response options () {

    return crossSiteAnoint(Response.status(Response.Status.OK)).build();
  }

  @Path("/token")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response token () {

    ServerAccessTokenRequest serverAccessTokenRequest;

    try {
      serverAccessTokenRequest = new ServerAccessTokenRequest(request);
      OAuthRegistration oauthRegistration;

      if ((oauthRegistration = oauthConfiguration.getRegistrationMap().get(serverAccessTokenRequest.getClientId())) == null) {
        return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorJsonResponse.instance().setError("invalid_client").setErrorDescription("unregistered client id").build()).type(MediaType.APPLICATION_JSON).build();
      } else if ((!oauthRegistration.isUnsafeRedirection()) && (!oauthRegistration.getRedirectUri().equals(serverAccessTokenRequest.getRedirectUri()))) {
        return crossSiteAnoint(Response.status(Response.Status.UNAUTHORIZED)).entity(ServerErrorJsonResponse.instance().setError("invalid_client").setErrorDescription("mismatching redirect uri").build()).type(MediaType.APPLICATION_JSON).build();
      }

      if (oauthRegistration.getSecret() != null) {
        try {
          if (!oauthRegistration.getSecret().equals(MungedCodec.decrypt(String.class, serverAccessTokenRequest.getClientSecret()))) {
            return crossSiteAnoint(Response.status(Response.Status.UNAUTHORIZED)).entity(ServerErrorJsonResponse.instance().setError("invalid_client").setErrorDescription("failed client application authentication").build()).type(MediaType.APPLICATION_JSON).build();
          }
        } catch (Exception exception) {
          LoggerManager.getLogger(OAuthResource.class).error(exception);

          return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorJsonResponse.instance().setError("server_error").setErrorDescription(exception.getMessage()).build()).type(MediaType.APPLICATION_JSON).build();
        }
      }

      if (GrantType.AUTHORIZATION_CODE.getParameter().equals(serverAccessTokenRequest.getGrantType())) {

        JWTToken jwtToken;

        try {
          jwtToken = (JWTToken)JWTCodec.decode(serverAccessTokenRequest.getCode(), new SymmetricJWTKeyMaster(serverAccessTokenRequest.getClientSecret()), oauthConfiguration.getSecretService().getSecretClass());
        } catch (Exception exception) {
          LoggerManager.getLogger(OAuthResource.class).error(exception);

          return crossSiteAnoint(Response.status(Response.Status.UNAUTHORIZED)).entity(ServerErrorJsonResponse.instance().setError("invalid_client").setErrorDescription("could not parse code").build()).type(MediaType.APPLICATION_JSON).build();
        }

        if (!jwtToken.getSub().equals(serverAccessTokenRequest.getClientId())) {

          return crossSiteAnoint(Response.status(Response.Status.UNAUTHORIZED)).entity(ServerErrorJsonResponse.instance().setError("invalid_client").setErrorDescription("code does not belong to this client").build()).type(MediaType.APPLICATION_JSON).build();
        }
        if (System.currentTimeMillis() - (jwtToken.getExp() * 1000) > oauthConfiguration.getOauthTokenGrantDuration().toMilliseconds()) {

          return crossSiteAnoint(Response.status(Response.Status.UNAUTHORIZED)).entity(ServerErrorJsonResponse.instance().setError("invalid_client").setErrorDescription("stale code").build()).type(MediaType.APPLICATION_JSON).build();
        }

        return emitAccessToken(jwtToken, oauthConfiguration.getOauthTokenGrantDuration(), serverAccessTokenRequest.getClientSecret());
      } else if (GrantType.REFRESH_TOKEN.getParameter().equals(serverAccessTokenRequest.getGrantType())) {

        JWTToken jwtToken;

        try {
          jwtToken = (JWTToken)JWTCodec.decode(serverAccessTokenRequest.getCode(), new SymmetricJWTKeyMaster(serverAccessTokenRequest.getClientSecret()), oauthConfiguration.getSecretService().getSecretClass());
        } catch (Exception exception) {
          LoggerManager.getLogger(OAuthResource.class).error(exception);

          return crossSiteAnoint(Response.status(Response.Status.UNAUTHORIZED)).entity(ServerErrorJsonResponse.instance().setError("invalid_client").setErrorDescription("could not parse refresh token").build()).type(MediaType.APPLICATION_JSON).build();
        }

        return emitAccessToken(jwtToken, oauthConfiguration.getOauthTokenGrantDuration(), serverAccessTokenRequest.getClientSecret());
      } else {

        return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorJsonResponse.instance().setError("unsupported_grant_type").setErrorDescription("only 'authorization_code' and 'refresh_token' grant types are supported").build()).type(MediaType.APPLICATION_JSON).build();
      }
    } catch (OAuthProtocolException oauthProtocolException) {
      LoggerManager.getLogger(OAuthResource.class).error(oauthProtocolException);

      return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorJsonResponse.instance().setError("server_error").setErrorDescription(oauthProtocolException.getMessage()).build()).type(MediaType.APPLICATION_JSON).build();
    }
  }

  private Response emitAccessToken (JWTToken jwtToken, Duration grantDuration, String key)
    throws OAuthProtocolException {

    String accessToken;
    String refreshToken;
    long now = System.currentTimeMillis();

    try {
      jwtToken.setExp((now + grantDuration.toMilliseconds()) / 1000);
      accessToken = JWTCodec.encode(jwtToken, new SymmetricJWTKeyMaster(key));
      jwtToken.setExp(now / 1000);
      refreshToken = JWTCodec.encode(jwtToken, new SymmetricJWTKeyMaster(key));
    } catch (Exception exception) {
      LoggerManager.getLogger(OAuthResource.class).error(exception);

      return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorJsonResponse.instance().setError("server_error").setErrorDescription(exception.getMessage()).build()).type(MediaType.APPLICATION_JSON).build();
    }

    return crossSiteAnoint(Response.ok(ServerAccessTokenResponse.instance().setTokenType(TokenType.BEARER.getParameter()).setAccessToken(accessToken).setRefreshToken(refreshToken).setExpiresIn(String.valueOf(oauthConfiguration.getOauthTokenGrantDuration().getTimeUnit().toSeconds(oauthConfiguration.getOauthTokenGrantDuration().getTime()))).build(), MediaType.APPLICATION_JSON)).build();
  }
}
