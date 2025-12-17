/*
 * Copyright (c) 2007 through 2026 David Berkman
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
import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.IncomprehensibleRequestException;
import org.smallmind.memcached.cubby.IncomprehensibleResponseException;

/**
 * Parses raw protocol lines into structured {@link Response} objects.
 */
public class ResponseParser {

  /**
   * Parses the provided buffer window into a response.
   *
   * @param joinedBuffer buffer spanning accumulated and newly read bytes
   * @param offset       starting offset
   * @param length       number of bytes representing the response line
   * @return parsed response
   * @throws IOException if parsing fails or the buffer is malformed
   */
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

      if (ResponseCode.MN.begins(first, second)) {
        response = new Response(ResponseCode.MN);
      } else if (ResponseCode.HD.begins(first, second)) {
        response = new Response(ResponseCode.HD);
      } else if (ResponseCode.VA.begins(first, second)) {
        response = new Response(ResponseCode.VA);

        if ((length < 4) || joinedBuffer.get() != ' ') {
          throw createIncomprehensibleResponseException(joinedBuffer, length);
        } else {
          response.setValueLength(accumulateInt(joinedBuffer, offset, length));
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

  /**
   * Determines whether the current buffer window represents an ERROR response.
   *
   * @param joinedBuffer buffer positioned at response start
   * @param length       length of the response line
   * @return {@code true} if the line is exactly "ERROR"
   */
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

  /**
   * Parses optional flags in a response line and populates the {@link Response}.
   *
   * @param response     response to update
   * @param joinedBuffer buffer positioned after the response code
   * @param offset       offset where the response line begins
   * @param length       total response line length
   * @throws IOException if the format is invalid
   */
  private static void parseFlags (Response response, JoinedBuffer joinedBuffer, int offset, int length)
    throws IOException {

    while ((joinedBuffer.position() - offset) < length) {
      if (joinedBuffer.get() != ' ') {
        throw createIncomprehensibleResponseException(joinedBuffer, length);
      } else if ((joinedBuffer.position() - offset) < length) {
        switch (joinedBuffer.get()) {
          case 'O':
            response.setToken(accumulateToken(joinedBuffer, offset, length));
            break;
          case 'c':
            response.setCas(accumulateLong(joinedBuffer, offset, length));
            break;
          case 's':
            response.setSize(accumulateInt(joinedBuffer, offset, length));
            break;
          case 'W':
            response.setWon(true);
            break;
          case 'Z':
            response.setAlsoWon(true);
            break;
          case 'X':
            response.setStale(true);
            break;
          default:
            throw createIncomprehensibleResponseException(joinedBuffer, length);
        }
      }
    }
  }

  /**
   * Parses an integer flag value from the buffer.
   *
   * @param joinedBuffer buffer containing the value
   * @param offset       offset where the response line begins
   * @param length       total response line length
   * @return parsed integer
   * @throws IOException if the token is not a number or the format is invalid
   */
  private static int accumulateInt (JoinedBuffer joinedBuffer, int offset, int length)
    throws IOException {

    try {
      return Integer.parseInt(accumulateToken(joinedBuffer, offset, length));
    } catch (NumberFormatException numberFormatException) {
      throw createIncomprehensibleResponseException(joinedBuffer, length);
    }
  }

  /**
   * Parses a long flag value from the buffer.
   *
   * @param joinedBuffer buffer containing the value
   * @param offset       offset where the response line begins
   * @param length       total response line length
   * @return parsed long
   * @throws IOException if the token is not a number or the format is invalid
   */
  private static long accumulateLong (JoinedBuffer joinedBuffer, int offset, int length)
    throws IOException {

    try {
      return Long.parseLong(accumulateToken(joinedBuffer, offset, length));
    } catch (NumberFormatException numberFormatException) {
      throw createIncomprehensibleResponseException(joinedBuffer, length);
    }
  }

  /**
   * Extracts a token up to the next space or end of the response line.
   *
   * @param joinedBuffer buffer containing the token
   * @param offset       offset where the response line begins
   * @param length       total response line length
   * @return token string
   */
  private static String accumulateToken (JoinedBuffer joinedBuffer, int offset, int length) {

    int index = 0;

    while ((joinedBuffer.position() + index) < (offset + length)) {

      if (joinedBuffer.peek(index) == ' ') {
        return new String(joinedBuffer.get(new byte[index]), StandardCharsets.UTF_8);
      }

      index++;
    }

    return new String(joinedBuffer.get(new byte[index]), StandardCharsets.UTF_8);
  }

  /**
   * Builds an {@link IncomprehensibleResponseException} using the current response slice.
   *
   * @param joinedBuffer buffer positioned at the start of the response
   * @param length       length of the response line
   * @return exception populated with the offending response text
   */
  private static IncomprehensibleResponseException createIncomprehensibleResponseException (JoinedBuffer joinedBuffer, int length) {

    byte[] slice = new byte[length];

    joinedBuffer.reset();
    joinedBuffer.get(slice);

    return new IncomprehensibleResponseException(new String(slice, StandardCharsets.UTF_8));
  }
}
