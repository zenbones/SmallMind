/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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

import java.util.concurrent.ThreadLocalRandom;

public class Frame {

  public static byte[] ping (byte[] message)
    throws WebSocketException {

    return control(OpCode.PING, message);
  }

  public static byte[] pong (byte[] message)
    throws WebSocketException {

    return control(OpCode.PONG, message);
  }

  public static byte[] close (byte[] status, String reason)
    throws WebSocketException {

    byte[] message;

    if (reason == null) {
      message = status;
    } else {

      byte[] reasonsBytes = reason.getBytes();

      message = new byte[reasonsBytes.length + 2];
      System.arraycopy(status, 0, message, 0, 2);
      System.arraycopy(reasonsBytes, 0, message, 2, reasonsBytes.length);
    }

    return control(OpCode.CLOSE, message);
  }

  private static byte[] control (OpCode opCode, byte[] message)
    throws WebSocketException {

    if (message.length > 125) {
      throw new WebSocketException("Control frame data length exceeds 125 bytes");
    }

    return data(opCode, message);
  }

  public static byte[] text (String message) {

    return data(OpCode.TEXT, message.getBytes());
  }

  public static byte[] binary (byte[] message) {

    return data(OpCode.BINARY, message);
  }

  private static byte[] data (OpCode opCode, byte[] message) {

    int start = (message.length < 126) ? 6 : (message.length < 65536) ? 8 : 14;
    byte[] out = new byte[message.length + start];
    byte[] mask = new byte[4];

    ThreadLocalRandom.current().nextBytes(mask);

    out[0] = (byte)(0x80 | opCode.getCode());

    if (message.length < 126) {
      out[1] = (byte)(0x80 | message.length);

      System.arraycopy(mask, 0, out, 2, 4);
    } else if (message.length < 65536) {
      out[1] = (byte)(0x80 | 126);
      out[2] = (byte)(message.length >>> 8);
      out[3] = (byte)(message.length & 0xFF);

      System.arraycopy(mask, 0, out, 4, 4);
    } else {
      out[1] = (byte)(0x80 | 127);
      // largest array will never be more than 2^31-1
      out[2] = 0;
      out[3] = 0;
      out[4] = 0;
      out[5] = 0;
      out[6] = (byte)(message.length >>> 24);
      out[7] = (byte)(message.length >>> 16);
      out[8] = (byte)(message.length >>> 8);
      out[9] = (byte)(message.length & 0xFF);

      System.arraycopy(mask, 0, out, 10, 4);
    }

    for (int index = 0; index < message.length; index++) {
      out[index + start] = (byte)(message[index] ^ mask[index % 4]);
    }

    return out;
  }

  public static Fragment decode (byte[] buffer)
    throws SyntaxException {

    OpCode opCode;
    boolean fin;
    int start;
    byte[] message;
    byte length;

    if ((opCode = OpCode.convert(buffer[0])) == null) {
      throw new SyntaxException("Unknown op code(%d)", buffer[0] & 0xF);
    }
    fin = (buffer[0] & 0x80) != 0;

    if ((length = (byte)(buffer[1] & 0x7F)) < 126) {
      start = 2;
      message = new byte[length];
    } else if (length == 126) {
      message = new byte[((buffer[2] & 0xFF) << 8) + (buffer[3] & 0xFF)];
      start = 4;
    } else {
      message = new byte[((buffer[6] & 0xFF) << 24) + ((buffer[7] & 0xFF) << 16) + ((buffer[8] & 0xFF) << 8) + (buffer[9] & 0xFF)];
      start = 10;
    }

    System.arraycopy(buffer, start, message, 0, message.length);

    return new Fragment(fin, opCode, message);
  }
}
