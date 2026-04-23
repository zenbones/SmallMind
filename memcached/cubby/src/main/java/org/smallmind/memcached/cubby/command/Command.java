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
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.UnexpectedResponseException;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.translator.KeyTranslator;

/**
 * Abstract base class for all memcached protocol commands in the Cubby client.
 *
 * <p>Each concrete subclass represents a distinct memcached meta-protocol operation
 * (get, set, delete, arithmetic, etc.) and is responsible for serializing itself
 * into wire-protocol bytes and interpreting the server's response into a
 * {@link Result}. The three-method contract ({@link #getKey}, {@link #construct},
 * {@link #process}) is the integration point used by the connection layer to route,
 * send, and complete every request.</p>
 */
public abstract class Command {

  /**
   * Returns the cache key targeted by this command, used by the connection layer
   * to route the command to the correct server node.
   *
   * @return the cache key associated with this command
   * @throws CubbyOperationException if the key cannot be determined or is not
   *                                 applicable for this command type
   */
  public abstract String getKey ()
    throws CubbyOperationException;

  /**
   * Serializes this command into its wire-protocol byte representation, ready
   * to be written to the memcached server socket.
   *
   * @param keyTranslator translator used to sanitize and encode the cache key
   *                      into a protocol-safe form
   * @return the fully assembled command bytes, including any trailing CRLF and
   * value payload where applicable
   * @throws IOException             if an I/O error occurs during encoding
   * @throws CubbyOperationException if the command cannot be constructed due to
   *                                 invalid or missing configuration
   */
  public abstract byte[] construct (KeyTranslator keyTranslator)
    throws IOException, CubbyOperationException;

  /**
   * Interprets the server {@link Response} for this command and returns a
   * normalized {@link Result} describing the outcome.
   *
   * @param response the decoded server response corresponding to this command
   * @return a {@link Result} encapsulating success status, returned value bytes,
   * and the CAS token
   * @throws UnexpectedResponseException if the response code is not one of the
   *                                     codes valid for this command type
   */
  public abstract Result process (Response response)
    throws UnexpectedResponseException;
}
