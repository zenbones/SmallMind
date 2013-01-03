/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.websocket;

import java.io.IOException;
import java.net.URI;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.util.ThreadLocalRandom;

public class ClientHandshake {

  public static byte[] constructRequest (URI uri, String... protocols)
    throws IOException {

    StringBuilder handshakeBuilder = new StringBuilder();
    byte[] keyBytes = new byte[16];

    ThreadLocalRandom.current().nextBytes(keyBytes);

    handshakeBuilder.append("GET ").append((uri.getPath() == null) || (uri.getPath().length() == 0) ? "/" : uri.getPath());
    if ((uri.getQuery() != null) && (uri.getQuery().length() > 0)) {
      handshakeBuilder.append('?').append(uri.getQuery());
    }
    handshakeBuilder.append(" HTTP/1.1").append('\n');

    handshakeBuilder.append("Host: ").append(uri.getHost().toLowerCase()).append(':').append((uri.getPort() != -1) ? uri.getPort() : uri.getScheme().equals("ws") ? 80 : 443).append('\n');
    handshakeBuilder.append("Upgrade: websocket\n");
    handshakeBuilder.append("Connection: Upgrade\n");
    handshakeBuilder.append("Sec-WebSocket-Key: ").append(Base64Codec.encode(keyBytes)).append('\n');

    if ((protocols != null) && (protocols.length > 0)) {

      boolean first = true;

      handshakeBuilder.append("Sec-WebSocket-Protocol: ");
      for (String protocol : protocols) {
        if (!first) {
          handshakeBuilder.append(',');
        }

        handshakeBuilder.append(protocol);
        first = false;
      }
      handshakeBuilder.append('\n');
    }

    handshakeBuilder.append("Sec-WebSocket-Version: 13\n");
    handshakeBuilder.append('\n');

    return handshakeBuilder.toString().getBytes();
  }

  public static void validateResponse (String response) {

    /*
    HTTP/1.1 101 Switching Protocols
Server: Apache-Coyote/1.1
Upgrade: websocket
Connection: upgrade
Sec-WebSocket-Accept: u58zgW+m3iGeL3qiduB/XPwkurQ=
Transfer-Encoding: chunked
Date: Fri, 21 Dec 2012 22:54:11 GMT
Connection: close
     */

    /*
    Sec-WebSocket-Protocol: chat
     */
  }
}
