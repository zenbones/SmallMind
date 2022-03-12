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

  public static Response parse (JoinedBuffer joinedBuffer, int offset, int length)
    throws IOException {

    joinedBuffer.mark();

    if (length < 2) {
      throw createIncomprehensibleResponseException(joinedBuffer, length);
    } else if (isError(joinedBuffer, length)) {
      throw new IncomprehensibleRequestException();
    } else {

      Response response;
      byte first = joinedBuffer.get();
      byte second = joinedBuffer.get();

      if (ResponseCode.HD.begins(first, second)) {
        response = new Response(ResponseCode.HD);
      } else if (ResponseCode.VA.begins(first, second)) {
        response = new Response(ResponseCode.VA);

        if ((length < 4) || joinedBuffer.get() != ' ') {
          throw createIncomprehensibleResponseException(joinedBuffer, length);
        } else {

          StringBuilder lengthBuilder = new StringBuilder();

          while ((joinedBuffer.position() - offset) < length) {

            char singleChar;

            if ((singleChar = (char)joinedBuffer.get()) != ' ') {
              lengthBuilder.append(singleChar);
            } else {
              joinedBuffer.position(joinedBuffer.position() - 1);
              break;
            }
          }
          try {
            response.setValueLength(Integer.parseInt(lengthBuilder.toString()));
          } catch (NumberFormatException numberFormatException) {
            throw createIncomprehensibleResponseException(joinedBuffer, length);
          }
        }
      } else if (ResponseCode.EN.begins(first, second)) {
        response = new Response(ResponseCode.EN);
      } else if (ResponseCode.EX.begins(first, second)) {
        response = new Response(ResponseCode.EX);
      } else if (ResponseCode.NF.begins(first, second)) {
        response = new Response(ResponseCode.NF);
      } else if (ResponseCode.NS.begins(first, second)) {
        response = new Response(ResponseCode.NS);
      } else {
        throw createIncomprehensibleResponseException(joinedBuffer, length);
      }

      parseFlags(response, joinedBuffer, offset, length);

      return response;
    }
  }

  private static boolean isError (JoinedBuffer joinedBuffer, int length) {

    try {
      return (length == 5)
               && (joinedBuffer.get() == 'E')
               && (joinedBuffer.get() == 'R')
               && (joinedBuffer.get() == 'R')
               && (joinedBuffer.get() == 'O')
               && (joinedBuffer.get() == 'R');
    } finally {
      joinedBuffer.reset();
    }
  }

  private static void parseFlags (Response response, JoinedBuffer joinedBuffer, int offset, int length)
    throws IOException {

    while ((joinedBuffer.position() - offset) < length) {
      if (joinedBuffer.get() != ' ') {
        throw createIncomprehensibleResponseException(joinedBuffer, length);
      } else {
        switch (joinedBuffer.get()) {
          case 'O':
            response.setToken(accumulateToken(joinedBuffer, offset, length));
            break;
          case 'c':
            response.setCas(Long.parseLong(accumulateToken(joinedBuffer, offset, length)));
            break;
          case 'W':
            response.setWon(true);
            break;
          case 'Z':
            response.setAlsoWon(true);
            break;
          default:
            throw createIncomprehensibleResponseException(joinedBuffer, length);
        }
      }
    }
  }

  private static String accumulateToken (JoinedBuffer joinedBuffer, int offset, int length) {

    StringBuilder tokenBuilder = new StringBuilder();

    while ((joinedBuffer.position() - offset) < length) {

      char tokenChar;

      if ((tokenChar = (char)joinedBuffer.get()) == ' ') {
        joinedBuffer.position(joinedBuffer.position() - 1);

        return tokenBuilder.toString();
      }

      tokenBuilder.append(tokenChar);
    }

    return tokenBuilder.toString();
  }

  private static IncomprehensibleResponseException createIncomprehensibleResponseException (JoinedBuffer joinedBuffer, int length) {

    byte[] slice = new byte[length];

    joinedBuffer.reset();
    joinedBuffer.get(slice);

    return new IncomprehensibleResponseException(new String(slice));
  }
}
