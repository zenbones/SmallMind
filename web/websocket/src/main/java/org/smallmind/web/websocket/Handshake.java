/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.web.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

public class Handshake {

  private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("HTTP/(\\d+\\.\\d+)\\s(\\d+)\\s(.+)");
  private static final Pattern HTTP_HEADER_PATTERN = Pattern.compile("([^:]+):\\s*(.+)\\s*");

//  HTTP-Version SP Status-Code SP Reason-Phrase

  public static byte[] constructRequest (URI uri, byte[] keyBytes, String... protocols)
    throws IOException {

    StringBuilder handshakeBuilder = new StringBuilder();

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

  public static void validateResponse (String response, byte[] keyBytes, String... protocols)
    throws IOException, NoSuchAlgorithmException, SyntaxException {

    BufferedReader reader = new BufferedReader(new StringReader(response));
    HashMap<String, String> fieldMap;
    Matcher httpStatusMatcher;
    String httpStatus;
    String httpField;

    do {
      httpStatus = reader.readLine();
    } while ((httpStatus != null) && (httpStatus.length() == 0));

    if (httpStatus == null) {
      throw new SyntaxException("The handshake response could not be parsed");
    }
    if (!(httpStatusMatcher = HTTP_STATUS_PATTERN.matcher(httpStatus)).matches()) {
      throw new SyntaxException("The http status line(%s) of the handshake response could not be parsed", httpStatus);
    }
    if (!httpStatusMatcher.group(2).equals("101")) {
      throw new SyntaxException("Incorrect http status code(%s) in the handshake response", httpStatusMatcher.group(2));
    }

    fieldMap = new HashMap<>();
    while (((httpField = reader.readLine()) != null) && (httpField.length() > 0)) {

      Matcher fieldMatcher;

      if (!(fieldMatcher = HTTP_HEADER_PATTERN.matcher(httpField)).matches()) {
        throw new SyntaxException("The http header line(%s) of the handshake response could not be parsed", httpField);
      }

      if (!fieldMap.containsKey(fieldMatcher.group(1))) {
        fieldMap.put(fieldMatcher.group(1), fieldMatcher.group(2));
      }
    }

    if (!fieldMap.containsKey("Upgrade")) {
      throw new SyntaxException("The http header does not contain an 'Upgrade' field");
    }
    if (!fieldMap.get("Upgrade").equalsIgnoreCase("websocket")) {
      throw new SyntaxException("The 'Upgrade' field(%s) of the http header does not contain the value 'websocket'", fieldMap.get("Upgrade"));
    }

    if (!fieldMap.containsKey("Connection")) {
      throw new SyntaxException("The http header does not contain a 'Connection' field");
    }
    if (!fieldMap.get("Connection").equalsIgnoreCase("upgrade")) {
      throw new SyntaxException("The 'Connection' field(%s) of the http header does not contain the value 'upgrade'", fieldMap.get("Connection"));
    }

    if (!fieldMap.containsKey("Sec-WebSocket-Accept")) {
      throw new SyntaxException("The http header does not contain a 'Sec-WebSocket-Accept' field");
    }
    if (!fieldMap.get("Sec-WebSocket-Accept").equals(Base64Codec.encode(EncryptionUtility.hash(HashAlgorithm.SHA_1, (Base64Codec.encode(keyBytes) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes())))) {
      throw new SyntaxException("The 'Sec-WebSocket-Accept' field(%s) of the http header does not contain the correct value", fieldMap.get("Sec-WebSocket-Accept"));
    }

    if (fieldMap.containsKey("Sec-WebSocket-Protocol")) {

      boolean matched = false;

      for (String protocol : protocols) {
        if (protocol.equals(fieldMap.get("Sec-WebSocket-Protocol"))) {
          matched = true;
          break;
        }
      }

      if (!matched) {
        throw new SyntaxException("The 'Sec-WebSocket-Protocol' field(%s) of the http header does not contain one of the requested sub-protocols", fieldMap.get("Sec-WebSocket-Protocol"));
      }
    }

    if (fieldMap.containsKey("Sec-WebSocket-Extensions")) {
      throw new SyntaxException("This client does not support the use of websocket extensions");
    }
  }
}
