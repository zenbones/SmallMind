/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire.signal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * JSON-based implementation of {@link SignalCodec} with optional verbose logging of payloads.
 */
public class JsonSignalCodec implements SignalCodec {

  private Level verboseLogLevel = Level.DEBUG;
  private boolean verbose = false;

  /**
   * Enables or disables verbose logging of encoded/decoded payloads.
   *
   * @param verbose {@code true} to log payloads
   */
  public void setVerbose (boolean verbose) {

    this.verbose = verbose;
  }

  /**
   * Sets the log level used when verbose logging is enabled.
   *
   * @param verboseLogLevel log level for payload output
   */
  public void setVerboseLogLevel (Level verboseLogLevel) {

    this.verboseLogLevel = verboseLogLevel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getContentType () {

    return MediaType.APPLICATION_JSON;
  }

  /**
   * Encodes the signal to JSON bytes and optionally logs the result.
   */
  @Override
  public byte[] encode (Signal signal)
    throws JsonProcessingException {

    byte[] bytes = JsonCodec.writeAsBytes(signal);

    if (verbose) {
      LoggerManager.getLogger(JsonSignalCodec.class).log(verboseLogLevel, "=>%s", new StringConverter(bytes));
    }

    return bytes;
  }

  /**
   * Decodes JSON bytes into a signal instance, with optional logging of the payload.
   */
  @Override
  public <S extends Signal> S decode (byte[] buffer, int offset, int len, Class<S> signalClass)
    throws IOException {

    if (verbose) {
      LoggerManager.getLogger(JsonSignalCodec.class).log(verboseLogLevel, "<=%s", new StringConverter(buffer, offset, len));
    }

    return JsonCodec.read(buffer, offset, len, signalClass);
  }

  /**
   * Converts a decoded JSON value into the requested type.
   */
  @Override
  public <T> T extractObject (Object value, Class<T> clazz) {

    return JsonCodec.convert(value, clazz);
  }

  /**
   * Utility wrapper to log byte arrays as UTF-8 strings.
   */
  private static class StringConverter {

    private final byte[] buffer;
    private final int offset;
    private final int len;

    /**
     * Wraps an entire byte array for conversion.
     *
     * @param buffer byte array to convert
     */
    public StringConverter (byte[] buffer) {

      this(buffer, 0, buffer.length);
    }

    /**
     * Wraps a subsection of a byte array for conversion.
     *
     * @param buffer byte array containing the data
     * @param offset start offset
     * @param len    number of bytes to include
     */
    public StringConverter (byte[] buffer, int offset, int len) {

      this.buffer = buffer;
      this.offset = offset;
      this.len = len;
    }

    /**
     * Returns the wrapped bytes as a UTF-8 string.
     */
    @Override
    public String toString () {

      return new String(buffer, offset, len, StandardCharsets.UTF_8);
    }
  }
}
