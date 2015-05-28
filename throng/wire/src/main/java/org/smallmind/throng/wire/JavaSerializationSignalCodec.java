package org.smallmind.throng.wire;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.ws.rs.core.MediaType;

public class JavaSerializationSignalCodec implements SignalCodec {

  @Override
  public String getContentType () {

    return MediaType.APPLICATION_OCTET_STREAM;
  }

  @Override
  public byte[] encode (Signal signal)
    throws IOException {

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

      objectOutputStream.writeObject(signal);

      return byteArrayOutputStream.toByteArray();
    }
  }

  @Override
  public <S extends Signal> S decode (byte[] buffer, int offset, int len, Class<S> signalClass)
    throws IOException, ClassNotFoundException {

    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, offset, len); ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {

      return signalClass.cast(objectInputStream.readObject());
    }
  }

  @Override
  public <T> T extractObject (Object value, Class<T> clazz) {

    return clazz.cast(value);
  }
}
