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

import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.UnexpectedResponseException;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseCode;
import org.smallmind.memcached.cubby.translator.KeyTranslator;

/**
 * Command that implements the memcached meta-noop ({@code mn}) operation,
 * used to verify that a connection to the server is alive and responsive.
 *
 * <p>The {@code mn} command carries no key or value payload and elicits an
 * {@code MN} response from the server. The {@link #setKey(String)} method
 * exists only so that the connection layer can route this command to a
 * specific server node; the key is never transmitted on the wire.</p>
 *
 * <p>Because the wire representation is a fixed four-byte sequence
 * ({@code "mn\r\n"}), it is pre-computed as a static constant and reused
 * across all instances.</p>
 */
public class NoopCommand extends Command {

  private static final byte[] BYTES = "mn\r\n".getBytes(StandardCharsets.UTF_8);

  private String key;

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
   * Sets an arbitrary cache key used solely for routing this command to the
   * correct server node. The key is not transmitted to the server.
   *
   * @param key the routing key
   * @return this command instance, for method chaining
   */
  public NoopCommand setKey (String key) {

    this.key = key;

    return this;
  }

  /**
   * Serializes this command into its wire-protocol byte representation, ready
   * to be written to the memcached server socket.
   *
   * <p>Returns the pre-computed, immutable {@code "mn\r\n"} byte sequence.
   * The {@code keyTranslator} parameter is not used because the noop command
   * has no key on the wire.</p>
   *
   * @param keyTranslator unused; present only to satisfy the {@link Command} contract
   * @return the fixed {@code mn} command bytes
   */
  @Override
  public byte[] construct (KeyTranslator keyTranslator) {

    return BYTES;
  }

  /**
   * Interprets the server {@link Response} for this command and returns a
   * normalized {@link Result} describing the outcome.
   *
   * <p>The only valid response to a noop is {@code MN}. Any other response code
   * indicates a protocol error.</p>
   *
   * @param response the decoded server response corresponding to this command
   * @return a {@link Result} encapsulating success status, returned value bytes,
   * and the CAS token
   * @throws UnexpectedResponseException if the response code is not {@code MN}
   */
  @Override
  public Result process (Response response)
    throws UnexpectedResponseException {

    if (response.getCode().equals(ResponseCode.MN)) {

      return new Result(null, true, response.getCas());
    } else {
      throw new UnexpectedResponseException("Unexpected response code(%s)", response.getCode().name());
    }
  }
}
