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
package org.smallmind.web.oauth.v1;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import org.smallmind.web.oauth.OAuthProtocolException;
import org.smallmind.web.oauth.ResponseType;
import org.smallmind.web.oauth.ServerAuthorizationFormEncodedResponse;
import org.smallmind.web.oauth.ServerAuthorizationRequest;
import org.smallmind.web.oauth.ServerErrorFormEncodedResponse;
import org.smallmind.web.oauth.ServerLoginFormEncodedRequest;
import org.smallmind.nutsnbolts.http.HexCodec;
import org.smallmind.scribe.pen.LoggerManager;

@Path("/v1/oauthsansredirect")
public class OAuthSansRedirectResource {

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
  @OPTIONS
  public Response options () {

    return crossSiteAnoint(Response.status(Response.Status.OK)).build();
  }

  @Path("/authorization")
  @GET
  @Produces(MediaType.APPLICATION_FORM_URLENCODED)
  public Response authorizationSansRedirect () {

    ServerAuthorizationRequest serverAuthorizationRequest = null;

    try {

      serverAuthorizationRequest = new ServerAuthorizationRequest(request);
      OAuthRegistration oauthRegistration;

      if (!ResponseType.CODE.getParameter().equals(serverAuthorizationRequest.getResponseType())) {
        return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorFormEncodedResponse.instance().setError("unsupported_response_type").setErrorDescription("only 'code' response types are supported").setState(serverAuthorizationRequest.getState()).build()).build();
      }
      else if ((oauthRegistration = oauthConfiguration.getRegistrationMap().get(serverAuthorizationRequest.getClientId())) == null) {
        return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorFormEncodedResponse.instance().setError("unauthorized_client").setErrorDescription("unregistered client id").setState(serverAuthorizationRequest.getState()).build()).build();
      }
      else if ((!oauthRegistration.isUnsafeRedirection()) && (!oauthRegistration.getRedirectUri().equals(serverAuthorizationRequest.getRedirectUri()))) {
        return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorFormEncodedResponse.instance().setError("unauthorized_client").setErrorDescription("mismatching redirect uri").setState(serverAuthorizationRequest.getState()).build()).build();
      }

      JWTToken jwtToken = null;
      SSOAuthData ssoAuthData = null;
      String cookieValue = null;
      String userName = null;

      if ((serverAuthorizationRequest.getAuthData() != null) && (!serverAuthorizationRequest.getAuthData().isEmpty())) {
        try {
          ssoAuthData = MungedCodec.decrypt(SSOAuthData.class, cookieValue = serverAuthorizationRequest.getAuthData());
        }
        catch (Exception exception) {
          LoggerManager.getLogger(OAuthResource.class).error(exception);

          return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorFormEncodedResponse.instance().setError("server_error").setErrorDescription(exception.getMessage()).setState(serverAuthorizationRequest.getState()).build()).build();
        }
      }
      else {

        Cookie[] cookies;

        if (((cookies = request.getCookies()) != null) && (cookies.length > 0)) {
          for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(oauthConfiguration.getSsoCookieName())) {
              try {
                ssoAuthData = MungedCodec.decrypt(SSOAuthData.class, cookieValue = HexCodec.hexDecode(cookie.getValue()));
              }
              catch (Exception exception) {
                LoggerManager.getLogger(OAuthResource.class).error(exception);

                return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorFormEncodedResponse.instance().setError("server_error").setErrorDescription(exception.getMessage()).setState(serverAuthorizationRequest.getState()).build()).build();
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
          }
          catch (Exception exception) {
            LoggerManager.getLogger(OAuthResource.class).error(exception);

            return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorFormEncodedResponse.instance().setError("server_error").setErrorDescription(exception.getMessage()).setState(serverAuthorizationRequest.getState()).build()).build();
          }
        }
      }

      if (jwtToken == null) {
        return crossSiteAnoint(Response.status(Response.Status.OK)).entity(ServerLoginFormEncodedRequest.instance().setAuthorizationUri(oauthRegistration.getOauthUri()).setUserName(userName).setResponseType(serverAuthorizationRequest.getResponseType()).setClientId(serverAuthorizationRequest.getClientId()).setRedirectUri(serverAuthorizationRequest.getRedirectUri()).setState(serverAuthorizationRequest.getState()).build()).build();
      }
      else {

        String code;

        jwtToken.setSub(serverAuthorizationRequest.getClientId());
        jwtToken.setExp(System.currentTimeMillis() / 1000);

        try {
          code = JWTCodec.encode(jwtToken, oauthRegistration.getSecret());
        }
        catch (Exception exception) {
          LoggerManager.getLogger(OAuthResource.class).error(exception);

          return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorFormEncodedResponse.instance().setError("server_error").setErrorDescription(exception.getMessage()).setState(serverAuthorizationRequest.getState()).build()).build();
        }

        return crossSiteAnoint(Response.status(Response.Status.OK)).cookie(new NewCookie(oauthConfiguration.getSsoCookieName(), HexCodec.hexEncode(cookieValue))).entity(ServerAuthorizationFormEncodedResponse.instance().setCode(code).setState(serverAuthorizationRequest.getState()).build()).build();
      }
    }
    catch (OAuthProtocolException oauthProtocolException) {
      LoggerManager.getLogger(OAuthResource.class).error(oauthProtocolException);

      if (serverAuthorizationRequest != null) {

        return crossSiteAnoint(Response.status(Response.Status.BAD_REQUEST)).entity(ServerErrorFormEncodedResponse.instance().setError("server_error").setErrorDescription(oauthProtocolException.getMessage()).setState(serverAuthorizationRequest.getState()).build()).build();
      }

      return null;
    }
  }
}
