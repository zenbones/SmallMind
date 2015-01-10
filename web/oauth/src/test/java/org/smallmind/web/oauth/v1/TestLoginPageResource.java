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
package org.smallmind.web.oauth.v1;

import java.net.URI;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.smallmind.web.oauth.URIUtilities;
import org.smallmind.nutsnbolts.util.Tuple;

@Path("/login")
public class TestLoginPageResource {

  private OAuthConfiguration oauthConfiguration;

  @Context
  private HttpServletRequest request;

  public void setOauthConfiguration (OAuthConfiguration oauthConfiguration) {

    this.oauthConfiguration = oauthConfiguration;
  }

  @Path("/display")
  @GET
  @Produces(MediaType.APPLICATION_FORM_URLENCODED)
  public Response display ()
    throws Exception {

    Tuple<String, String> paramTuple = new Tuple<>();
    Map<String, String[]> paramMap = request.getParameterMap();

    for (Map.Entry<String, String[]> paramEntry : paramMap.entrySet()) {
      if (!(paramEntry.getKey().equals("authorization_uri") || paramEntry.getKey().equals("user_name"))) {
        for (String value : paramEntry.getValue()) {
          paramTuple.addPair(paramEntry.getKey(), value);
        }
      }
    }

    paramTuple.setPair("auth_data", MungedCodec.encrypt(new SSOAuthData("test", "foobar"), true));

    return Response.status(Response.Status.FOUND).location(URI.create(URIUtilities.composeWithQueryParameters(paramMap.get("authorization_uri")[0], paramTuple))).build();
  }
}
