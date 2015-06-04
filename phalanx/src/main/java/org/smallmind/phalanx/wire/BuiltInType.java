package org.smallmind.phalanx.wire;

public enum BuiltInType {

  BOOLEAN("Z"), BYTE("B"), U_BYTE("Y"), SHORT("S"), U_SHORT("H"), INTEGER("I"), U_INTEGER("N"), LONG("L"), U_LONG("U"), FLOAT("F"), DOUBLE("D"), CHARACTER("C"), STRING("G"), DATE("T"), VOID("V"), FAULT("A"), OBJECT("O");

  private String code;

  private BuiltInType (String code) {

    this.code = code;
  }

  public String getCode () {

    return code;
  }
}