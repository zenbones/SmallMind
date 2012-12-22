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
