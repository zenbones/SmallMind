/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;
import org.smallmind.nutsnbolts.util.Tuple;

public class Handshake {

  private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("HTTP/(\\d+\\.\\d+)\\s(\\d+)\\s(.+)");
  private static final Pattern HTTP_HEADER_PATTERN = Pattern.compile("([^:]+):\\s*(.+)\\s*");

  public static Tuple<String, String> constructHeaders (int protocolVersion, URI uri, byte[] keyBytes, WebSocketExtension[] extensions, String... protocols)
    throws IOException {

    Tuple<String, String> headerTuple = new Tuple<>();
    String extesnionsValue;

    headerTuple.addPair("Host", new StringBuilder(uri.getHost().toLowerCase()).append(':').append((uri.getPort() != -1) ? uri.getPort() : uri.getScheme().equals("ws") ? 80 : 443).toString());
    headerTuple.addPair("Upgrade", "websocket");
    headerTuple.addPair("Connection", "Upgrade");
    headerTuple.addPair("Sec-WebSocket-Key", Base64Codec.encode(keyBytes));
    headerTuple.addPair("Sec-WebSocket-Version", String.valueOf(protocolVersion));

    if ((protocols != null) && (protocols.length > 0)) {

      StringBuilder protocolBuilder = new StringBuilder();

      for (String protocol : protocols) {
        if (protocolBuilder.length() > 0) {
          protocolBuilder.append(',');
        }

        protocolBuilder.append(protocol);
      }

      headerTuple.addPair("Sec-WebSocket-Protocol", protocolBuilder.toString());
    }

    if ((extesnionsValue = HandshakeResponse.getExtensionsAsString(extensions)) != null) {
      headerTuple.addPair("Sec-WebSocket-Extensions", extesnionsValue);
    }

    return headerTuple;
  }

  public static byte[] constructRequest (URI uri, Tuple<String, String> headerTuple)
    throws IOException {

    StringBuilder handshakeBuilder = new StringBuilder();

    handshakeBuilder.append("GET ").append((uri.getPath() == null) || (uri.getPath().length() == 0) ? "/" : uri.getPath());
    if ((uri.getQuery() != null) && (uri.getQuery().length() > 0)) {
      handshakeBuilder.append('?').append(uri.getQuery());
    }
    handshakeBuilder.append(" HTTP/1.1").append('\n');

    for (String key : headerTuple.getKeys()) {
      for (String value : headerTuple.getValues(key)) {
        handshakeBuilder.append(key).append(": ").append(value).append('\n');
      }
    }
    handshakeBuilder.append('\n');

    return handshakeBuilder.toString().getBytes();
  }

  public static HandshakeResponse validateResponse (Tuple<String, String> headerTuple, String response, byte[] keyBytes, WebSocketExtension[] installedExtensions, String... protocols)
    throws IOException, NoSuchAlgorithmException, SyntaxException {

    BufferedReader reader = new BufferedReader(new StringReader(response));
    Matcher httpStatusMatcher;
    LinkedList<WebSocketExtension> negotiatedExtensionList = new LinkedList<>();
    WebSocketExtension[] negotiatedExtensions = null;
    String httpStatus;
    String httpField;
    String negotiatedProtocol = "";

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

    while (((httpField = reader.readLine()) != null) && (httpField.length() > 0)) {

      Matcher fieldMatcher;

      if (!(fieldMatcher = HTTP_HEADER_PATTERN.matcher(httpField)).matches()) {
        throw new SyntaxException("The http header line(%s) of the handshake response could not be parsed", httpField);
      }

      if (!headerTuple.containsKey(fieldMatcher.group(1))) {
        headerTuple.addPair(fieldMatcher.group(1), fieldMatcher.group(2));
      }
    }

    if (!headerTuple.containsKey("Upgrade")) {
      throw new SyntaxException("The http header does not contain an 'Upgrade' field");
    }
    if (!headerTuple.getValue("Upgrade").equalsIgnoreCase("websocket")) {
      throw new SyntaxException("The 'Upgrade' field(%s) of the http header does not contain the value 'websocket'", headerTuple.getValue("Upgrade"));
    }

    if (!headerTuple.containsKey("Connection")) {
      throw new SyntaxException("The http header does not contain a 'Connection' field");
    }
    if (!headerTuple.getValue("Connection").equalsIgnoreCase("upgrade")) {
      throw new SyntaxException("The 'Connection' field(%s) of the http header does not contain the value 'upgrade'", headerTuple.getValue("Connection"));
    }

    if (!headerTuple.containsKey("Sec-WebSocket-Accept")) {
      throw new SyntaxException("The http header does not contain a 'Sec-WebSocket-Accept' field");
    }
    if (!headerTuple.getValue("Sec-WebSocket-Accept").equals(Base64Codec.encode(EncryptionUtility.hash(HashAlgorithm.SHA_1, (Base64Codec.encode(keyBytes) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes())))) {
      throw new SyntaxException("The 'Sec-WebSocket-Accept' field(%s) of the http header does not contain the correct value", headerTuple.getValue("Sec-WebSocket-Accept"));
    }

    if (headerTuple.containsKey("Sec-WebSocket-Protocol")) {

      String headerProtocol = headerTuple.getValue("Sec-WebSocket-Protocol");
      boolean matched = false;

      for (String protocol : protocols) {
        if (protocol.equals(headerProtocol)) {
          negotiatedProtocol = protocol;
          matched = true;
          break;
        }
      }

      if (!matched) {
        throw new SyntaxException("The 'Sec-WebSocket-Protocol' field(%s) of the http header does not contain one of the requested sub-protocols", headerProtocol);
      }
    }

    if (headerTuple.containsKey("Sec-WebSocket-Extensions")) {
      for (String extensionValue : headerTuple.getValues("Sec-WebSocket-Extensions")) {

        String[] splitExtensionValues;

        if ((splitExtensionValues = extensionValue.split(",", -1)).length == 0) {
          throw new SyntaxException("The 'Sec-WebSocket-Extensions' contains an empty header value");
        }

        for (String splitExtensionValue : splitExtensionValues) {

          String[] splitParameterValues = splitExtensionValue.split(";", -1);

          if (splitParameterValues.length > 0) {

            WebSocketExtension negotiatedExtension;
            LinkedList<ExtensionParameter> parameterList = new LinkedList<>();
            ExtensionParameter[] parameters;
            String extensionName;

            if ((extensionName = splitParameterValues[0].trim()).isEmpty()) {
              throw new SyntaxException("The 'Sec-WebSocket-Extensions' contains an empty extension name");
            }

            if (splitParameterValues.length > 1) {
              for (int index = 1; index < splitParameterValues.length; index++) {

                int equalsPos;

                if ((equalsPos = splitParameterValues[index].indexOf('=')) >= 0) {

                  String parameterName;
                  String parameterValue;

                  if ((parameterName = splitParameterValues[index].substring(0, equalsPos).trim()).isEmpty()) {
                    throw new SyntaxException("The 'Sec-WebSocket-Extensions' contains an empty parameter name");
                  }
                  if ((parameterValue = splitParameterValues[index].substring(equalsPos + 1).trim()).isEmpty()) {
                    throw new SyntaxException("The 'Sec-WebSocket-Extensions' contains an empty parameter value");
                  }

                  parameterList.add(new ExtensionParameter(parameterName, parameterValue));
                }
              }
            }

            parameters = new ExtensionParameter[parameterList.size()];
            parameterList.toArray(parameters);

            negotiatedExtension = new WebSocketExtension(extensionName, parameters);
            for (WebSocketExtension installedExtension : installedExtensions) {
              if (installedExtension.equals(negotiatedExtension)) {
                negotiatedExtensionList.add(negotiatedExtension);
                break;
              }
            }
          }
        }

        negotiatedExtensions = new WebSocketExtension[negotiatedExtensionList.size()];
        negotiatedExtensionList.toArray(negotiatedExtensions);
      }
    }

    return new HandshakeResponse(negotiatedProtocol, negotiatedExtensions);
  }
}
