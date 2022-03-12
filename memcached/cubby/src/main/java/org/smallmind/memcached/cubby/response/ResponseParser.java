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
import java.nio.ByteBuffer;
import org.smallmind.memcached.cubby.IncomprehensibleRequestException;
import org.smallmind.memcached.cubby.IncomprehensibleResponseException;

public class ResponseParser {

  public static Response parse (ByteBuffer readBuffer, int length)
    throws IOException {

    readBuffer.mark();

    if (length < 2) {
      throw createIncomprehensibleResponseException(readBuffer, length);
    } else if (isError(readBuffer, length)) {
      throw new IncomprehensibleRequestException();
    } else {

      Response response;
      byte first = readBuffer.get();
      byte second = readBuffer.get();

      if (ResponseCode.HD.begins(first, second)) {
        response = new Response(ResponseCode.HD);
      } else if (ResponseCode.VA.begins(first, second)) {
        response = new Response(ResponseCode.VA);

        if ((length < 4) || readBuffer.get() != ' ') {
          throw createIncomprehensibleResponseException(readBuffer, length);
        } else {

          StringBuilder lengthBuilder = new StringBuilder();

          while (readBuffer.position() < length) {

            char singleChar;

            if ((singleChar = (char)readBuffer.get()) != ' ') {
              lengthBuilder.append(singleChar);
            } else {
              readBuffer.position(readBuffer.position() - 1);
              break;
            }
          }
          try {
            response.setValueLength(Integer.parseInt(lengthBuilder.toString()));
          } catch (NumberFormatException numberFormatException) {
            throw createIncomprehensibleResponseException(readBuffer, length);
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
        throw createIncomprehensibleResponseException(readBuffer, length);
      }

      parseFlags(response, readBuffer, length);

      return response;
    }
  }

  private static boolean isError (ByteBuffer readBuffer, int length) {

    try {
      return (length == 5)
               && (readBuffer.get() == 'E')
               && (readBuffer.get() == 'R')
               && (readBuffer.get() == 'R')
               && (readBuffer.get() == 'O')
               && (readBuffer.get() == 'R');
    } finally {
      readBuffer.reset();
    }
  }

  private static void parseFlags (Response response, ByteBuffer readBuffer, int length)
    throws IOException {

    while (readBuffer.position() < length) {
      if (readBuffer.get() != ' ') {
        throw createIncomprehensibleResponseException(readBuffer, length);
      } else {
        switch (readBuffer.get()) {
          case 'O':
            response.setToken(accumulateToken(readBuffer, length));
            break;
          case 'c':
            response.setCas(Long.parseLong(accumulateToken(readBuffer, length)));
            break;
          case 'W':
            response.setWon(true);
            break;
          case 'Z':
            response.setAlsoWon(true);
            break;
          default:
            throw createIncomprehensibleResponseException(readBuffer, length);
        }
      }
    }
  }

  private static String accumulateToken (ByteBuffer readBuffer, int length) {

    StringBuilder tokenBuilder = new StringBuilder();

    while (readBuffer.position() < length) {

      char tokenChar;

      if ((tokenChar = (char)readBuffer.get()) == ' ') {
        readBuffer.position(readBuffer.position() - 1);

        return tokenBuilder.toString();
      }

      tokenBuilder.append(tokenChar);
    }

    return tokenBuilder.toString();
  }

  private static IncomprehensibleResponseException createIncomprehensibleResponseException (ByteBuffer readBuffer, int length) {

    byte[] slice = new byte[length];

    readBuffer.reset();
    readBuffer.get(slice);

    return new IncomprehensibleResponseException(new String(slice));
  }
}
