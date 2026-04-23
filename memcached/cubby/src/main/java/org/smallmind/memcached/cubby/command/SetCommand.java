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
 * Command that implements the memcached meta-set ({@code ms}) operation,
 * covering unconditional set, add-only, replace-only, append, and prepend
 * mutations along with optional CAS protection.
 *
 * <p>The specific mutation variant is selected via {@link #setMode(SetMode)}.
 * When no mode is supplied and the CAS token is {@code 0} the command
 * automatically promotes itself to an add-only ({@code E}) operation and
 * requests the resulting CAS token, which is the convention used by the
 * Cubby client for optimistic upserts.</p>
 *
 * <p>Append and prepend operations support optional <em>vivification</em>:
 * when {@link #setVivify(boolean) vivify} is {@code true} and the key is
 * absent, the server creates it with the supplied value and applies the
 * configured expiration as the initial TTL.</p>
 *
 * <p>The resulting byte array from {@link #construct(KeyTranslator)} contains
 * the {@code ms} command header, the value payload, and a trailing CRLF as
 * required by the meta-protocol.</p>
 */
public class SetCommand extends Command {

  private static final Long ZERO = 0L;

  private SetMode mode;
  private byte[] value;
  private String key;
  private String opaqueToken;
  private boolean vivify;
  private Integer expiration;
  private Long cas;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getKey () {

    return key;
  }

  /**
   * Sets the cache key to write.
   *
   * @param key the key to store the value under
   * @return this command instance, for method chaining
   */
  public SetCommand setKey (String key) {

    this.key = key;

    return this;
  }

  /**
   * Supplies the pre-serialized value payload to store in the cache.
   *
   * @param value the encoded value bytes to write
   * @return this command instance, for method chaining
   */
  public SetCommand setValue (byte[] value) {

    this.value = value;

    return this;
  }

  /**
   * Selects the mutation variant (set, add, replace, append, or prepend).
   * When not set and the CAS token is {@code 0}, the command defaults to
   * an add-only upsert mode.
   *
   * @param mode the {@link SetMode} describing the desired mutation semantics
   * @return this command instance, for method chaining
   */
  public SetCommand setMode (SetMode mode) {

    this.mode = mode;

    return this;
  }

  /**
   * Supplies a CAS token so the mutation is applied only when the server-side
   * version of the item matches the token, providing optimistic-concurrency
   * protection. A token of {@code 0} is treated as a sentinel that triggers
   * add-only semantics.
   *
   * @param cas the compare-and-swap token obtained from a previous read,
   *            or {@code 0} to request add-only upsert behavior
   * @return this command instance, for method chaining
   */
  public SetCommand setCas (Long cas) {

    this.cas = cas;

    return this;
  }

  /**
   * Sets the time-to-live for the stored item. For append and prepend
   * operations this value is used as the initial TTL when vivification
   * creates a new key; for all other modes it sets (or refreshes) the
   * item's expiration.
   *
   * @param expiration the time-to-live in seconds
   * @return this command instance, for method chaining
   */
  public SetCommand setExpiration (Integer expiration) {

    this.expiration = expiration;

    return this;
  }

  /**
   * Enables <em>vivification</em> for append and prepend operations: when
   * {@code true} and the target key does not exist, the server creates it
   * with the supplied value and expiration rather than silently discarding
   * the write.
   *
   * @param vivify {@code true} to create a missing key during append/prepend;
   *               {@code false} to leave absent keys untouched
   * @return this command instance, for method chaining
   */
  public SetCommand setVivify (boolean vivify) {

    this.vivify = vivify;

    return this;
  }

  /**
   * Attaches an opaque correlation token that the server echoes back verbatim
   * in its response, allowing callers to correlate pipelined replies.
   *
   * @param opaqueToken an arbitrary string token to include in the request
   * @return this command instance, for method chaining
   */
  public SetCommand setOpaqueToken (String opaqueToken) {

    this.opaqueToken = opaqueToken;

    return this;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Builds the {@code ms} meta-set command line followed by the value bytes
   * and a trailing CRLF. The flags appended depend on the configured mode, CAS
   * token, expiration, vivify setting, and opaque token. A CAS value of {@code 0}
   * with no explicit mode (or with {@link SetMode#SET}) causes the command to be
   * promoted to add-only ({@link SetMode#ADD}) with the {@code c} flag so the
   * server returns the resulting CAS token.</p>
   */
  @Override
  public byte[] construct (KeyTranslator keyTranslator)
    throws IOException, CubbyOperationException {

    byte[] bytes;
    byte[] commandBytes;

    StringBuilder line = new StringBuilder("ms ").append(keyTranslator.encode(key)).append(' ').append(value.length).append(" b");

    if (ZERO.equals(cas) && ((mode == null) || SetMode.SET.equals(mode))) {
      line.append(" M").append(SetMode.ADD.getToken());
      line.append(" c");
    } else {
      if (mode != null) {
        line.append(" M").append(mode.getToken());
      }
      if (cas != null) {
        line.append(" C").append(cas).append(" c");
      }
    }
    if (expiration != null) {
      if (SetMode.APPEND.equals(mode) || SetMode.PREPEND.equals(mode)) {
        if (vivify) {
          line.append(" N").append(expiration);
        }
      } else {
        line.append(" T").append(expiration);
      }
    } else if (SetMode.APPEND.equals(mode) || SetMode.PREPEND.equals(mode)) {
      if (vivify) {
        line.append(" N0");
      }
    }

    if (opaqueToken != null) {
      line.append(" O").append(opaqueToken);
    }

    commandBytes = line.append("\r\n").toString().getBytes(StandardCharsets.UTF_8);
    bytes = new byte[commandBytes.length + value.length + 2];

    System.arraycopy(commandBytes, 0, bytes, 0, commandBytes.length);
    System.arraycopy(value, 0, bytes, commandBytes.length, value.length);
    System.arraycopy("\r\n".getBytes(StandardCharsets.UTF_8), 0, bytes, commandBytes.length + value.length, 2);

    return bytes;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Response codes {@code EX} (CAS mismatch), {@code NF} (not found, for
   * replace-only), and {@code NS} (not stored, for add-only) all indicate that
   * the write did not take effect and return an unsuccessful result.
   * {@code HD} indicates that the item was stored successfully.</p>
   *
   * @throws UnexpectedResponseException if the response code is none of
   *                                     {@code EX}, {@code NF}, {@code NS}, or {@code HD}
   */
  @Override
  public Result process (Response response)
    throws UnexpectedResponseException {

    if (response.getCode().in(ResponseCode.EX, ResponseCode.NF, ResponseCode.NS)) {

      return new Result(null, false, response.getCas());
    } else if (ResponseCode.HD.equals(response.getCode())) {

      return new Result(null, true, response.getCas());
    } else {
      throw new UnexpectedResponseException("Unexpected response code(%s)", response.getCode().name());
    }
  }
}
