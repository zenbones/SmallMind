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
 * Command that implements the memcached meta-delete ({@code md}) operation,
 * removing a key from the cache with optional CAS protection.
 *
 * <p>When a CAS token is supplied the server only removes the item if its
 * current version matches the token, providing optimistic-concurrency safety.
 * An optional opaque correlation token may be attached so that pipelined
 * responses can be matched back to their originating requests.</p>
 *
 * <p>Both {@code HD} (deleted or already absent) and {@code NF} (not found)
 * response codes are treated as successful outcomes by {@link #process(Response)},
 * because deleting a key that no longer exists is an idempotent no-op.
 * Only {@code EX} (CAS mismatch) yields a failure result.</p>
 */
public class DeleteCommand extends Command {

  private String key;
  private String opaqueToken;
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
   * Sets the cache key to be deleted.
   *
   * @param key the key to remove from the cache
   * @return this command instance, for method chaining
   */
  public DeleteCommand setKey (String key) {

    this.key = key;

    return this;
  }

  /**
   * Supplies a CAS token so the delete is performed only when the server-side
   * version of the item matches the token.
   *
   * @param cas the compare-and-swap token obtained from a previous read
   * @return this command instance, for method chaining
   */
  public DeleteCommand setCas (Long cas) {

    this.cas = cas;

    return this;
  }

  /**
   * Attaches an opaque correlation token that the server echoes back verbatim
   * in its response, allowing callers to correlate pipelined replies.
   *
   * @param opaqueToken an arbitrary string token to include in the request
   * @return this command instance, for method chaining
   */
  public DeleteCommand setOpaqueToken (String opaqueToken) {

    this.opaqueToken = opaqueToken;

    return this;
  }

  /**
   * Serializes this command into its wire-protocol byte representation, ready
   * to be written to the memcached server socket.
   *
   * <p>Builds the {@code md} meta-delete command line. When a CAS token has
   * been configured the {@code C} and {@code c} flags are appended so the
   * server performs a conditional delete.</p>
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

    StringBuilder line = new StringBuilder("md ").append(keyTranslator.encode(key)).append(" b");

    if (cas != null) {
      line.append(" C").append(cas).append(" c");
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
   * <p>{@code EX} indicates a CAS mismatch and returns a failure result.
   * {@code HD} (deleted) and {@code NF} (not found) both return a success
   * result, since the end state — the key being absent — is achieved in
   * both cases.</p>
   *
   * @param response the decoded server response corresponding to this command
   * @return a {@link Result} encapsulating success status, returned value bytes,
   * and the CAS token
   * @throws UnexpectedResponseException if the response code is none of
   *                                     {@code EX}, {@code HD}, or {@code NF}
   */
  @Override
  public Result process (Response response)
    throws UnexpectedResponseException {

    if (ResponseCode.EX.equals(response.getCode())) {

      return new Result(null, false, response.getCas());
    } else if (response.getCode().in(ResponseCode.HD, ResponseCode.NF)) {

      return new Result(null, true, response.getCas());
    } else {
      throw new UnexpectedResponseException("Unexpected response code(%s)", response.getCode().name());
    }
  }
}
