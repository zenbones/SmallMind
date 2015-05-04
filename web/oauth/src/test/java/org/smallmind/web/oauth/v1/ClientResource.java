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

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.smallmind.web.oauth.ClientAccessTokenFromCodeRequest;
import org.smallmind.web.oauth.ClientAuthorizationRequest;
import org.smallmind.web.oauth.GrantType;
import org.smallmind.web.oauth.OAuthProtocolException;
import org.smallmind.web.oauth.ResponseType;

@Path("/spoof")
public class ClientResource {

  private String clientId;
  private String restUri;

  @Context
  private HttpServletResponse response;

  public void setClientId (String clientId) {

    this.clientId = clientId;
  }

  public void setRestUri (String restUri) {

    this.restUri = restUri;
  }

  //http://localhost:9015/rest/spoof/login
  @Path("/login")
  @GET
  public Response login ()
    throws IOException, OAuthProtocolException {

    String oauthGet = ClientAuthorizationRequest.locationUri(restUri + "/v1/oauth/authorization")
                        .setResponseType(ResponseType.CODE.getParameter())
                        .setClientId(clientId)
                        .setRedirectUri(restUri + "/spoof/exchange")
                        .setState("my-application-state")
                        .build();

    response.sendRedirect(oauthGet);

    return null;
  }

  @Path("/exchange")
  @GET
  public String exchange (@QueryParam("code") String code)
    throws Exception {

    HttpPost httpPost = new HttpPost(restUri + "/v1/oauth/token");
    String jsonTokenPostEntity = ClientAccessTokenFromCodeRequest.instance().setClientId(clientId).setGrantType(GrantType.AUTHORIZATION_CODE.getParameter()).setCode(code).setRedirectUri(restUri + "/spoof/exchange").setClientSecret("monkeys eat smores").build();

    httpPost.setEntity(new StringEntity(jsonTokenPostEntity, ContentType.APPLICATION_FORM_URLENCODED));
    try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(httpPost)) {

      HttpEntity responseEntity = httpResponse.getEntity();

      return EntityUtils.toString(responseEntity);
    }
  }
}

