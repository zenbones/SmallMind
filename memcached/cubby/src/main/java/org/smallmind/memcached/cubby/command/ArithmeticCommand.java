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
 * Command that implements the memcached meta-arithmetic ({@code ma}) operation,
 * supporting both increment and decrement of a numeric counter stored at a given key.
 *
 * <p>In addition to basic delta arithmetic, this command supports optional
 * initialization (creating the key with a seed value when absent), a TTL for
 * the key, CAS-guarded updates, and an opaque correlation token. When a negative
 * {@code delta} is supplied the mode is automatically flipped so the sign is
 * absorbed into the direction of the operation.</p>
 *
 * <p>A successful response returns the post-operation counter value inside
 * the {@link Result#getValue()} byte array.</p>
 */
public class ArithmeticCommand extends Command {

  private ArithmeticMode mode = ArithmeticMode.INCREMENT;
  private String key;
  private String opaqueToken;
  private Integer initial;
  private Integer delta;
  private Integer expiration;
  private Long cas;

  /**
   * Returns the cache key targeted by this command, used by the connection layer
   * to route the command to the correct server node.
   *
   * @return the cache key associated with this command
   */
  @Override
  public String getKey () {

    return key;
  }

  /**
   * Sets the cache key whose numeric value will be modified.
   *
   * @param key the cache key to target
   * @return this command instance, for method chaining
   */
  public ArithmeticCommand setKey (String key) {

    this.key = key;

    return this;
  }

  /**
   * Sets whether the operation increments or decrements the stored counter.
   * Defaults to {@link ArithmeticMode#INCREMENT} when not called.
   *
   * @param mode the arithmetic direction to apply
   * @return this command instance, for method chaining
   */
  public ArithmeticCommand setMode (ArithmeticMode mode) {

    this.mode = mode;

    return this;
  }

  /**
   * Supplies a CAS token so the mutation is applied only when the server-side
   * version matches, providing optimistic-concurrency protection.
   *
   * @param cas the compare-and-swap token obtained from a previous read
   * @return this command instance, for method chaining
   */
  public ArithmeticCommand setCas (Long cas) {

    this.cas = cas;

    return this;
  }

  /**
   * Sets the seed value used to initialize the counter when the key does not
   * yet exist in the cache. Requires {@link #setExpiration(Integer)} to also
   * be set if a TTL is desired at creation time.
   *
   * @param initial the value to store when creating a previously absent key
   * @return this command instance, for method chaining
   */
  public ArithmeticCommand setInitial (Integer initial) {

    this.initial = initial;

    return this;
  }

  /**
   * Specifies the amount to add to (or subtract from) the stored counter.
   * If the value is negative the operation's {@link ArithmeticMode} is
   * automatically flipped to preserve correct semantics.
   *
   * @param delta the magnitude of the change; may be negative
   * @return this command instance, for method chaining
   */
  public ArithmeticCommand setDelta (Integer delta) {

    this.delta = delta;

    return this;
  }

  /**
   * Sets the TTL applied to the key. When used together with
   * {@link #setInitial(Integer)}, the expiration governs the lifetime of the
   * newly created key; otherwise it refreshes the TTL of an existing key.
   *
   * @param expiration the time-to-live in seconds
   * @return this command instance, for method chaining
   */
  public ArithmeticCommand setExpiration (Integer expiration) {

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
  public ArithmeticCommand setOpaqueToken (String opaqueToken) {

    this.opaqueToken = opaqueToken;

    return this;
  }

  /**
   * Serializes this command into its wire-protocol byte representation, ready
   * to be written to the memcached server socket.
   *
   * <p>Builds the {@code ma} meta-arithmetic command line. If the configured
   * {@code delta} is negative its absolute value is used and the {@code mode}
   * is flipped accordingly before serialization.</p>
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

    if ((delta != null) && (delta < 0)) {
      delta = Math.abs(delta);
      mode = mode.flip();
    }

    StringBuilder line = new StringBuilder("ma ").append(keyTranslator.encode(key)).append(' ').append(" b v");

    if (mode != null) {
      line.append(" M").append(mode.getToken());
    }
    if (cas != null) {
      line.append(" C").append(cas).append(" c");
    }
    if (initial != null) {
      line.append(" J").append(initial);
      line.append(" N").append((expiration == null) ? 0 : expiration);
    }
    if (delta != null) {
      line.append(" D").append(delta);
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
   * <p>Interprets the server response for the arithmetic operation.
   * Response codes {@code EX}, {@code NF}, and {@code NS} indicate the operation
   * did not complete (expired, not found, or not stored); {@code HD} indicates
   * success and carries the resulting counter value.</p>
   *
   * @param response the decoded server response corresponding to this command
   * @return a {@link Result} encapsulating success status, returned value bytes,
   * and the CAS token
   * @throws UnexpectedResponseException if the response code is none of
   *                                     {@code EX}, {@code NF}, {@code NS}, or {@code HD}
   */
  @Override
  public Result process (Response response)
    throws UnexpectedResponseException {

    if (response.getCode().in(ResponseCode.EX, ResponseCode.NF, ResponseCode.NS)) {

      return new Result(null, false, response.getCas());
    } else if (ResponseCode.HD.equals(response.getCode())) {

      return new Result(response.getValue(), true, response.getCas());
    } else {
      throw new UnexpectedResponseException("Unexpected response code(%s)", response.getCode().name());
    }
  }
}
