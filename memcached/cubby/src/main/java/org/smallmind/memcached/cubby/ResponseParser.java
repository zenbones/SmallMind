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
package org.smallmind.memcached.cubby;

import java.io.IOException;

public class ResponseParser {

  /*
  - "HD" (STORED), to indicate success.

- "NS" (NOT_STORED), to indicate the data was not stored, but not
because of an error.

- "EX" (EXISTS), to indicate that the item you are trying to store with
CAS semantics has been modified since you last fetched it.

- "NF" (NOT_FOUND), to indicate that the item you are trying to store
with CAS semantics did not exist.


  EN
  HD
  VA

  - "NF" (NOT_FOUND), to indicate that the item with this key was not found.

- "EX" (EXISTS), to indicate that the supplied CAS token does not match the
  stored item.

- "HD" to indicate success

- "NF" (NOT_FOUND), to indicate that the item with this key was not found.

- "NS" (NOT_STORED), to indicate that the item was not created as requested
  after a miss.

- "EX" (EXISTS), to indicate that the supplied CAS token does not match the
  stored item.

   */

  public static Response parse (StringBuilder responseBuilder)
    throws IOException {

    if (responseBuilder.length() < 2) {
      throw new IncomprehensibleResponseException(responseBuilder.toString());
    } else {

      Response response;

      switch (responseBuilder.substring(0, 2)) {
        case "HD":
          response = new Response(ResponseCode.HD);
          break;
        case "VA":
          response = new Response(ResponseCode.VA);
          break;
        case "EN":
          response = new Response(ResponseCode.EN);
          break;
        case "EX":
          response = new Response(ResponseCode.EX);
          break;
        case "NF":
          response = new Response(ResponseCode.NF);
          break;
        case "NS":
          response = new Response(ResponseCode.NS);
          break;
        default:
          throw new IncomprehensibleResponseException(responseBuilder.toString());
      }

      parseFlags(response, responseBuilder);

      return response;
    }
  }

  private static void parseFlags (Response response, StringBuilder responseBuilder)
    throws IOException {

    int flagIndex = 2;

    while (responseBuilder.length() != flagIndex) {
      if (responseBuilder.charAt(flagIndex) != ' ') {
        throw new IncomprehensibleResponseException(responseBuilder.toString());
      } else {

        String token;

        switch (responseBuilder.charAt(flagIndex + 1)) {
          case 'O':
            flagIndex += (token = accumulateToken(responseBuilder, flagIndex + 2)).length() + 2;
            response.setToken(token);
            break;
          case 'c':
            flagIndex += (token = accumulateToken(responseBuilder, flagIndex + 2)).length() + 2;
            response.setCas(Long.parseLong(token));
            break;
          default:
            throw new IncomprehensibleResponseException(responseBuilder.toString());
        }
      }
    }
  }

  private static String accumulateToken (StringBuilder responseBuilder, int index) {

    StringBuilder tokenBuilder = new StringBuilder();
    int tokenIndex = index;

    while (responseBuilder.length() > tokenIndex) {

      char tokenChar;

      if ((tokenChar = responseBuilder.charAt(tokenIndex)) == ' ') {

        return tokenBuilder.toString();
      }

      tokenBuilder.append(tokenChar);
      tokenIndex++;
    }

    return tokenBuilder.toString();
  }
}
