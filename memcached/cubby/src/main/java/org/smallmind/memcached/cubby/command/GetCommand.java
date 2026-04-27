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
package org.smallmind.memcached.cubby.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.UnexpectedResponseException;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseCode;
import org.smallmind.memcached.cubby.translator.KeyTranslator;

/**
 * Command that implements the memcached meta-get ({@code mg}) operation,
 * covering plain gets, get-and-touch (refresh TTL), CAS retrieval, and
 * touch-only (update TTL without returning the value).
 *
 * <p>The behavior is controlled by three independent flags:</p>
 * <ul>
 *   <li>{@link #setValue(boolean)} — when {@code true} (the default) the value
 *       body is returned; set to {@code false} for touch-only operations.</li>
 *   <li>{@link #setCas(boolean)} — when {@code true} the CAS token is included
 *       in the response flags and carried in the returned {@link Result}.</li>
 *   <li>{@link #setExpiration(Integer)} — when set the server updates the item's
 *       TTL as part of the read, enabling get-and-touch semantics.</li>
 * </ul>
 *
 * <p>A cache miss ({@code EN}), a lost read-lease ({@code won}/{@code alsoWon}),
 * are all surfaced as unsuccessful results. A hit with a value body returns
 * {@code VA}; a touch-only hit returns {@code HD}.</p>
 */
public class GetCommand extends Command {

  private String key;
  private String opaqueToken;
  private boolean cas;
  private boolean value = true;
  private Integer expiration;

  /**
   * Returns the cache key targeted by this command, used by the connection layer
   * to route the command to the correct server node.
   *
   * @return the cache key associated with this command
   * @throws CubbyOperationException if the key cannot be determined or is not
   *                                 applicable for this command type
   */
  @Override
  public String getKey () {

    return key;
  }

  /**
   * Sets the cache key to retrieve.
   *
   * @param key the key whose value is to be fetched
   * @return this command instance, for method chaining
   */
  public GetCommand setKey (String key) {

    this.key = key;

    return this;
  }

  /**
   * Requests that the server include the CAS token in the response so it can
   * be used in a subsequent conditional write.
   *
   * @param cas {@code true} to request the CAS token; {@code false} to omit it
   * @return this command instance, for method chaining
   */
  public GetCommand setCas (boolean cas) {

    this.cas = cas;

    return this;
  }

  /**
   * Sets a new TTL to apply to the item as part of the read, implementing
   * get-and-touch semantics. When set, the server refreshes the item's
   * expiration to the given value before returning it.
   *
   * @param expiration the new time-to-live in seconds
   * @return this command instance, for method chaining
   */
  public GetCommand setExpiration (Integer expiration) {

    this.expiration = expiration;

    return this;
  }

  /**
   * Attaches an opaque correlation token that the server echoes back verbatim
   * in its response, allowing callers to correlate pipelined replies.
   *
   * @param opaqueToken an arbitrary string token to include in the request
   * @return this command instance, for method chaining
   */
  public GetCommand setOpaqueToken (String opaqueToken) {

    this.opaqueToken = opaqueToken;

    return this;
  }

  /**
   * Controls whether the value body is returned with the response.
   * Set to {@code false} for touch-only operations where only the TTL
   * needs to be refreshed and the value is not required.
   * Defaults to {@code true}.
   *
   * @param value {@code true} to fetch the value bytes; {@code false} for a
   *              touch-only request
   * @return this command instance, for method chaining
   */
  public GetCommand setValue (boolean value) {

    this.value = value;

    return this;
  }

  /**
   * Serializes this command into its wire-protocol byte representation, ready
   * to be written to the memcached server socket.
   *
   * <p>Builds the {@code mg} meta-get command line, appending the {@code v}
   * flag when the value body is requested, the {@code c} flag when a CAS token
   * is wanted, the {@code T} flag when a new expiration is supplied, and the
   * {@code O} flag when an opaque token is present.</p>
   *
   * @param keyTranslator translator used to sanitize and encode the cache key
   *                      into a protocol-safe form
   * @return the fully assembled command bytes, including any trailing CRLF and
   * value payload where applicable
   * @throws IOException             if an I/O error occurs during encoding
   * @throws CubbyOperationException if the command cannot be constructed due to
   *                                 invalid or missing configuration
   */
  @Override
  public byte[] construct (KeyTranslator keyTranslator)
    throws IOException, CubbyOperationException {

    StringBuilder line = new StringBuilder("mg ").append(keyTranslator.encode(key)).append(" b");

    if (value) {
      line.append(" v");
    }
    if (cas) {
      line.append(" c");
    }
    if (expiration != null) {
      line.append(" T").append(expiration);
    }
    if (opaqueToken != null) {
      line.append(" O").append(opaqueToken);
    }

    return line.append("\r\n").toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Interprets the server {@link Response} for this command and returns a
   * normalized {@link Result} describing the outcome.
   *
   * <p>An {@code EN} (not found) response, or a response where the read lease
   * was won or also-won, are treated as cache misses and return an unsuccessful
   * result. A {@code VA} response carries the retrieved value bytes and is
   * returned as a successful result. When the value body was suppressed
   * ({@link #setValue(boolean) setValue(false)}), a {@code HD} response
   * represents a successful touch.</p>
   *
   * @param response the decoded server response corresponding to this command
   * @return a {@link Result} encapsulating success status, returned value bytes,
   * and the CAS token
   * @throws UnexpectedResponseException if the response code does not match
   *                                     any expected code for this command configuration
   */
  @Override
  public Result process (Response response)
    throws UnexpectedResponseException {

    if (ResponseCode.EN.equals(response.getCode()) || response.isWon() || response.isAlsoWon()) {

      return new Result(null, false, response.getCas());
    } else if (value && ResponseCode.VA.equals(response.getCode())) {

      return new Result(response.getValue(), true, response.getCas());
    } else if ((!value) && ResponseCode.HD.equals(response.getCode())) {

      return new Result(null, true, response.getCas());
    } else {
      throw new UnexpectedResponseException("Unexpected response code(%s)", response.getCode().name());
    }
  }
}
