/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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

public enum OpCode {

  CONTINUATION((byte)0x0), TEXT((byte)0x1), BINARY((byte)0x2), CLOSE((byte)0x8), PING((byte)0x9), PONG((byte)0xA);
  private byte code;

  private OpCode (byte code) {

    this.code = code;
  }

  public static OpCode convert (byte singleByte) {

    byte maskedValue = (byte)(singleByte & 0xF);

    for (OpCode opCode : OpCode.values()) {
      if (opCode.getCode() == maskedValue) {

        return opCode;
      }
    }

    return null;
  }

  public byte getCode () {

    return code;
  }
}
