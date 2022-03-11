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
package org.smallmind.memcached.cubby.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.smallmind.memcached.cubby.response.ErrorResponse;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseParser;
import org.smallmind.memcached.cubby.response.ServerResponse;

public class ResponseReader {

  private ServerResponse partialResponse;
  private StringBuilder responseBuilder = new StringBuilder();
  private boolean complete = false;
  private byte[] value;
  private int valueIndex;

  public Response read (ByteBuffer byteBuffer) {

    char singleChar;

    do {
      if (value != null) {

        int valueBytesRead;

        byteBuffer.get(value, valueIndex, valueBytesRead = Math.min(byteBuffer.remaining(), value.length - valueIndex));
        if ((valueIndex += valueBytesRead) == value.length) {

          ServerResponse response = partialResponse;

          if (value.length > 2) {

            byte[] truncatedValue = new byte[value.length - 2];
            System.arraycopy(value, 0, truncatedValue, 0, truncatedValue.length);

            partialResponse.setValue(truncatedValue);
          }

          partialResponse = null;
          value = null;
          valueIndex = 0;

          return response;
        }
      } else {
        switch (singleChar = (char)byteBuffer.get()) {
          case '\r':
            if (complete) {
              responseBuilder.append('\r');
            }
            complete = true;
            break;
          case '\n':
            if (!complete) {
              responseBuilder.append('\n');
            } else {
              try {

                ServerResponse response = ResponseParser.parse(responseBuilder);
                int valueLength;

                if ((valueLength = response.getValueLength()) >= 0) {
                  partialResponse = response;
                  value = new byte[valueLength + 2];
                } else {

                  return response;
                }
              } catch (IOException ioException) {

                return new ErrorResponse(ioException);
              } finally {
                complete = false;
                responseBuilder = new StringBuilder();
              }
            }
            break;
          default:
            responseBuilder.append(singleChar);
        }
      }
    } while (byteBuffer.remaining() > 0);

    return null;
  }
}
