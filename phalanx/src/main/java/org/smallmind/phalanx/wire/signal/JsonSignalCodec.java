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
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * JSON-based {@link SignalCodec} that encodes signals as UTF-8 JSON and decodes them using
 * {@code JsonCodec}; optionally logs raw payloads at a configurable level.
 */
public class JsonSignalCodec implements SignalCodec {

  private Level verboseLogLevel = Level.DEBUG;
  private boolean verbose = false;

  /**
   * Enables or disables verbose payload logging for both encode and decode operations.
   *
   * @param verbose {@code true} to log each payload; {@code false} to suppress logging
   */
  public void setVerbose (boolean verbose) {

    this.verbose = verbose;
  }

  /**
   * Sets the log level at which payloads are written when verbose logging is enabled.
   *
   * @param verboseLogLevel the desired log level (defaults to {@link Level#DEBUG})
   */
  public void setVerboseLogLevel (Level verboseLogLevel) {

    this.verboseLogLevel = verboseLogLevel;
  }

  /**
   * Returns {@code application/json} as the content type produced by this codec.
   *
   * @return the MIME type string {@code application/json}
   */
  @Override
  public String getContentType () {

    return MediaType.APPLICATION_JSON;
  }

  /**
   * Encodes {@code signal} to a JSON byte array, logging the payload if verbose mode is active.
   *
   * @param signal the signal to encode
   * @return UTF-8 JSON bytes representing the signal
   */
  @Override
  public byte[] encode (Signal signal) {

    byte[] bytes = JsonCodec.writeAsBytes(signal);

    if (verbose) {
      LoggerManager.getLogger(JsonSignalCodec.class).log(verboseLogLevel, "=>%s", new StringConverter(bytes));
    }

    return bytes;
  }

  /**
   * Decodes the specified region of {@code buffer} from JSON into a signal of type {@code signalClass},
   * logging the raw bytes if verbose mode is active.
   *
   * @param buffer      byte array containing the JSON payload
   * @param offset      starting offset within {@code buffer}
   * @param len         number of bytes to decode
   * @param signalClass the target signal type
   * @param <S>         the signal type parameter
   * @return the deserialized signal instance
   * @throws IOException if JSON parsing fails
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
   * Converts a decoded JSON value (typically a {@code JsonNode} or {@code Map}) into an instance
   * of {@code clazz} using {@code JsonCodec}.
   *
   * @param value the raw decoded value to convert
   * @param clazz the target type
   * @param <T>   the target type parameter
   * @return the converted object
   */
  @Override
  public <T> T extractObject (Object value, Class<T> clazz) {

    return JsonCodec.convert(value, clazz);
  }

  /**
   * Lazy {@code toString} wrapper that converts a byte-array slice to a UTF-8 string only when
   * needed (e.g., when the logger actually writes a log record).
   */
  private record StringConverter(byte[] buffer, int offset, int len) {

    /**
     * Creates a converter that covers the entire {@code buffer}.
     *
     * @param buffer the byte array to wrap
     */
    public StringConverter (byte[] buffer) {

      this(buffer, 0, buffer.length);
    }

    /**
     * Canonical record constructor; parameters are assigned by the record mechanism.
     *
     * @param buffer byte array containing the data
     * @param offset start offset within the array
     * @param len    number of bytes to include
     */
    private StringConverter {

    }

    /**
     * Decodes the wrapped byte slice as a UTF-8 string.
     *
     * @return UTF-8 string representation of the wrapped bytes
     */
    @Override
    public String toString () {

      return new String(buffer, offset, len, StandardCharsets.UTF_8);
    }
  }
}
