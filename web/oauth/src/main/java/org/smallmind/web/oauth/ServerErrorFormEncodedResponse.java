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
package org.smallmind.web.oauth;

import org.smallmind.nutsnbolts.http.HTTPCodec;
import org.smallmind.nutsnbolts.util.Tuple;

public class ServerErrorFormEncodedResponse {

  private String error = "unknown";
  private String errorDescription;
  private String errorUri;
  private String state;

  public static ServerErrorFormEncodedResponse instance () {

    return new ServerErrorFormEncodedResponse();
  }

  public ServerErrorFormEncodedResponse setError (String error) {

    if ((error != null) && (!error.isEmpty())) {
      this.error = error;
    }

    return this;
  }

  public ServerErrorFormEncodedResponse setErrorDescription (String errorDescription) {

    this.errorDescription = errorDescription;

    return this;
  }

  public ServerErrorFormEncodedResponse setErrorUri (String errorUri) {

    this.errorUri = errorUri;

    return this;
  }

  public ServerErrorFormEncodedResponse setState (String state) {

    this.state = state;

    return this;
  }

  public String build () {

    Tuple<String, String> paramTuple = new Tuple<>();

    paramTuple.addPair("error", error);

    if ((errorDescription != null) && (!errorDescription.isEmpty())) {
      paramTuple.addPair("error_description", errorDescription);
    }
    if ((errorUri != null) && (!errorUri.isEmpty())) {
      paramTuple.addPair("error_uri", errorUri);
    }
    if ((state != null) && (!state.isEmpty())) {
      paramTuple.addPair("state", state);
    }

    return HTTPCodec.urlEncode(paramTuple);
  }
}