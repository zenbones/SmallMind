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
