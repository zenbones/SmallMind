package org.smallmind.phalanx.wire;

import java.io.IOException;
import javax.ws.rs.core.MediaType;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.jersey.util.JsonCodec;

public class JsonSignalCodec implements SignalCodec {

  private Level verboseLogLevel = Level.DEBUG;
  private boolean verbose = false;

  public void setVerbose (boolean verbose) {

    this.verbose = verbose;
  }

  public void setVerboseLogLevel (Level verboseLogLevel) {

    this.verboseLogLevel = verboseLogLevel;
  }

  @Override
  public String getContentType () {

    return MediaType.APPLICATION_JSON;
  }

  @Override
  public byte[] encode (Signal signal)
    throws JsonProcessingException {

    byte[] bytes = JsonCodec.writeAsBytes(signal);

    if (verbose) {
      LoggerManager.getLogger(JsonSignalCodec.class).log(verboseLogLevel, "=>%s", new StringConverter(bytes));
    }

    return bytes;
  }

  @Override
  public <S extends Signal> S decode (byte[] buffer, int offset, int len, Class<S> signalClass)
    throws IOException {

    if (verbose) {
      LoggerManager.getLogger(JsonSignalCodec.class).log(verboseLogLevel, "<=%s", new StringConverter(buffer, offset, len));
    }

    return JsonCodec.read(buffer, offset, len, signalClass);
  }

  @Override
  public <T> T extractObject (Object value, Class<T> clazz) {

    return JsonCodec.convert(value, clazz);
  }

  private class StringConverter {

    private byte[] buffer;
    private int offset;
    private int len;

    public StringConverter (byte[] buffer) {

      this(buffer, 0, buffer.length);
    }

    public StringConverter (byte[] buffer, int offset, int len) {

      this.buffer = buffer;
      this.offset = offset;
      this.len = len;
    }

    @Override
    public String toString () {

      return new String(buffer, offset, len);
    }
  }
}
