package org.smallmind.phalanx.wire;

import java.util.Date;
import org.smallmind.nutsnbolts.context.ContextFactory;

public class WireTestingServiceImpl implements WireTestingService {

  @Override
  public int getVersion () {

    return 1;
  }

  @Override
  public String getServiceName () {

    return "WireTestService";
  }

  @Override
  public boolean hasContext () {

    return ContextFactory.exists(TestWireContext.class);
  }

  @Override
  public void requiresContext () {

  }

  @Override
  public void doNothing () {

  }

  @Override
  public boolean echoBoolean (boolean b) {

    return b;
  }

  @Override
  public byte echoByte (byte b) {

    return b;
  }

  @Override
  public short echoShort (short s) {

    return s;
  }

  @Override
  public int echoInt (int i) {

    return i;
  }

  @Override
  public long echoLong (long l) {

    return l;
  }

  @Override
  public float echoFloat (float f) {

    return f;
  }

  @Override
  public double echoDouble (double d) {

    return d;
  }

  @Override
  public char echoChar (char c) {

    return c;
  }

  @Override
  public String echoString (@Argument("string") String string) {

    return string;
  }

  @Override
  public Date echoDate (@Argument("date") Date date) {

    return date;
  }

  @Override
  public Color[] echoColors (@Argument("colors") Color... colors) {

    return colors;
  }

  @Override
  public Integer addNumbers (@Argument("x") int x, @Argument("y") int y) {

    return x + y;
  }

  @Override
  public void throwError ()
    throws WireTestingException {

    throw new WireTestingException();
  }
}
