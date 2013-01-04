package org.smallmind.websocket;

public class Data {

  private OpCode opCode;
  private boolean fin;
  private byte[] message;

  public Data (boolean fin, OpCode opCode, byte[] message) {

    this.fin = fin;
    this.opCode = opCode;
    this.message = message;
  }

  public boolean isFinal () {

    return fin;
  }

  public OpCode getOpCode () {

    return opCode;
  }

  public byte[] getMessage () {

    return message;
  }
}
