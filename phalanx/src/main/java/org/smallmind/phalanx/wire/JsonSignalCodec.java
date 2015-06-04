package org.smallmind.phalanx.wire;

import java.io.IOException;
import javax.ws.rs.core.MediaType;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.smallmind.web.jersey.util.JsonCodec;

public class JsonSignalCodec implements SignalCodec {

  @Override
  public String getContentType () {

    return MediaType.APPLICATION_JSON;
  }

  @Override
  public byte[] encode (Signal signal)
    throws JsonProcessingException {

    return JsonCodec.writeAsBytes(signal);
  }

  @Override
  public <S extends Signal> S decode (byte[] buffer, int offset, int len, Class<S> signalClass)
    throws IOException {

    return JsonCodec.read(buffer, offset, len, signalClass);
  }

  @Override
  public <T> T extractObject (Object value, Class<T> clazz) {

    return JsonCodec.convert(value, clazz);
  }
}
