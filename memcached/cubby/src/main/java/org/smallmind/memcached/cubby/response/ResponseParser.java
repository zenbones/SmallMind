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
package org.smallmind.memcached.cubby.response;

import java.io.IOException;
import org.smallmind.memcached.cubby.IncomprehensibleRequestException;
import org.smallmind.memcached.cubby.IncomprehensibleResponseException;

public class ResponseParser {

  public static ServerResponse parse (StringBuilder responseBuilder)
    throws IOException {

    if (responseBuilder.length() < 2) {
      throw new IncomprehensibleResponseException(responseBuilder.toString());
    } else if (isError(responseBuilder)) {
      throw new IncomprehensibleRequestException();
    } else {

      ServerResponse response;
      int index = 2;

      switch (responseBuilder.substring(0, 2)) {
        case "HD":
          response = new ServerResponse(ResponseCode.HD);
          break;
        case "VA":
          response = new ServerResponse(ResponseCode.VA);
          if ((responseBuilder.length() < 4) || responseBuilder.charAt(index++) != ' ') {
            throw new IncomprehensibleResponseException(responseBuilder.toString());
          } else {

            StringBuilder lengthBuilder = new StringBuilder();
            char singleChar;

            while ((index < responseBuilder.length()) && ((singleChar = responseBuilder.charAt(index)) != ' ')) {
              lengthBuilder.append(singleChar);
              index++;
            }
            try {
              response.setValueLength(Integer.parseInt(lengthBuilder.toString()));
            } catch (NumberFormatException numberFormatException) {
              throw new IncomprehensibleResponseException(responseBuilder.toString());
            }
          }
          break;
        case "EN":
          response = new ServerResponse(ResponseCode.EN);
          break;
        case "EX":
          response = new ServerResponse(ResponseCode.EX);
          break;
        case "NF":
          response = new ServerResponse(ResponseCode.NF);
          break;
        case "NS":
          response = new ServerResponse(ResponseCode.NS);
          break;
        default:
          throw new IncomprehensibleResponseException(responseBuilder.toString());
      }

      parseFlags(response, responseBuilder, index);

      return response;
    }
  }

  private static boolean isError (StringBuilder responseBuilder) {

    return (responseBuilder.length() == 5)
             && (responseBuilder.charAt(0) == 'E')
             && (responseBuilder.charAt(1) == 'R')
             && (responseBuilder.charAt(2) == 'R')
             && (responseBuilder.charAt(3) == 'O')
             && (responseBuilder.charAt(4) == 'R');
  }

  private static void parseFlags (ServerResponse response, StringBuilder responseBuilder, int index)
    throws IOException {

    int flagIndex = index;

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
          case 'W':
            flagIndex += 2;
            response.setWon(true);
            break;
          case 'Z':
            flagIndex += 2;
            response.setAlsoWon(true);
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
