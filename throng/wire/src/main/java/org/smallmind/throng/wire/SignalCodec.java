package org.smallmind.throng.wire;

public interface SignalCodec {

  public abstract String getContentType ();

  public abstract byte[] encode (Signal signal)
    throws Exception;

  public abstract <S extends Signal> S decode (byte[] buffer, int offset, int len, Class<S> signalClass)
    throws Exception;

  public abstract <T> T extractObject (Object value, Class<T> clazz);
}
