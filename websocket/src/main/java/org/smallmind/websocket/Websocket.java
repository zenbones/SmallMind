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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Websocket {

  private SocketChannel channel;
  private Selector selector;
  private AtomicReference<ReadyState> readyStateRef = new AtomicReference<>(ReadyState.CONNECTING);
  private String url;

  public Websocket (URI uri, String... protocols)
    throws IOException, SyntaxException {

    ByteArrayOutputStream responseStream;
    ByteBuffer responseBuffer;

    if (!uri.isAbsolute()) {
      throw new SyntaxException("A websocket uri must be absolute");
    }
    if ((uri.getScheme() == null) || (!(uri.getScheme().equals("ws") || uri.getScheme().equals("wss")))) {
      throw new SyntaxException("A websocket requires a uri with either the 'ws' or 'wss' scheme");
    }
    if ((uri.getFragment() != null) && (uri.getFragment().length() > 0)) {
      throw new SyntaxException("A websocket uri may not contain a fragment");
    }

    if (!ProtocolValidator.validate(protocols)) {
      throw new SyntaxException("The provided protocols(%s) are not valid", Arrays.toString(protocols));
    }

    url = uri.toString();
    channel = (SocketChannel)SocketChannel.open().configureBlocking(false);
    channel.register(selector = Selector.open(), SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
    channel.connect(new InetSocketAddress(uri.getHost().toLowerCase(), (uri.getPort() != -1) ? uri.getPort() : uri.getScheme().equals("ws") ? 80 : 443));

    while (true) {
      if (selector.select(1000) > 0) {

        Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

        while (keyIter.hasNext()) {

          SelectionKey key = keyIter.next();

          if (key.isConnectable()) {
            channel.finishConnect();
            channel.write(ByteBuffer.wrap(ClientHandshake.constructRequest(uri, protocols)));
          }
          else if (key.isReadable()) {
            responseStream = new ByteArrayOutputStream();
            responseBuffer = ByteBuffer.wrap(new byte[32]);

            do {
              channel.read((ByteBuffer)responseBuffer.rewind());
              responseStream.write(responseBuffer.array(), 0, responseBuffer.position());
            } while (responseBuffer.position() > 0);

            if (responseStream.size() > 0) {
              System.out.println("readable...");
              System.out.print(new String(responseStream.toByteArray()));
            }
          }

          keyIter.remove();
        }
      }
    }
  }

  public String url () {

    return url;
  }

  public ReadyState getReadyState () {

    return readyStateRef.get();
  }

  public int readyState () {

    return readyStateRef.get().ordinal();
  }

  public String extensions () {

    return "";
  }

  private String stuff () {

    return "/v2/presence/v1/shout*{\n" +
      "  \"authentication\" : {\n" +
      "    \"data\" : null,\n" +
      "    \"encoding\" : null,\n" +
      "    \"encryption\" : null\n" +
      "  },\n" +
      "  \"message\" : {\n" +
      "    \"content\" : {\n" +
      "      \"message\" : \"Logged in\"\n" +
      "    },\n" +
      "    \"header\" : {\n" +
      "      \"device\" : {\n" +
      "        \"country\" : \"en_US\",\n" +
      "        \"language\" : \"en\",\n" +
      "        \"platform\" : \"APPLE\",\n" +
      "        \"make\" : \"iPad\",\n" +
      "        \"model\" : \"iPad3,1\"\n" +
      "      },\n" +
      "      \"client\" : {\n" +
      "        \"installId\" : {\n" +
      "          \"type\" : \"UDID\",\n" +
      "          \"value\" : \"19764a064571b00924b0f228bfed180bd168c3f7\"\n" +
      "        },\n" +
      "        \"sku\" : \"com.glu.gunbros2\",\n" +
      "        \"version\" : \"1.0.0\"\n" +
      "      },\n" +
      "      \"authorization\" : {\n" +
      "        \"avatar\" : {\n" +
      "          \"id\" : 61\n" +
      "        }\n" +
      "      }\n" +
      "    },\n" +
      "    \"conversation\" : \"" + UUID.randomUUID().toString() + "\",\n" +
      "    \"sequence\" : 10,\n" +
      "    \"timestamp\" : {\n" +
      "      \"milliseconds\" : {\n" +
      "        \"time\" : " + System.currentTimeMillis() + "\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
  }
}
