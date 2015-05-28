package org.smallmind.throng.wire;

import java.util.Date;
import org.smallmind.nutsnbolts.context.ExpectedContexts;

public interface WireTestingService extends WiredService {

  public abstract boolean hasContext ();

  @ExpectedContexts(UnknownContext.class)
  public abstract void requiresContext ();

  public abstract void doNothing ();

  public abstract boolean echoBoolean (@Argument("b") boolean b);

  public abstract byte echoByte (@Argument("b") byte b);

  public abstract short echoShort (@Argument("s") short s);

  public abstract int echoInt (@Argument("i") int i);

  public abstract long echoLong (@Argument("l") long l);

  public abstract float echoFloat (@Argument("f") float f);

  public abstract double echoDouble (@Argument("d") double d);

  public abstract char echoChar (@Argument("c") char c);

  public abstract String echoString (@Argument("string") String string);

  public abstract Date echoDate (@Argument("date") Date date);

  public abstract Color[] echoColors (@Argument("colors") Color... colors);

  public abstract Integer addNumbers (@Argument("x") int x, @Argument("y") int y);

  public abstract void throwError ()
    throws WireTestingException;
}
