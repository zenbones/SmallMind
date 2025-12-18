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
package org.smallmind.web.websocket.spi;

import java.io.IOException;
import java.io.Reader;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.MessageHandler;

/**
 * Adapter that decodes text {@link Reader} messages before delegating to an application handler.
 *
 * @param <T> decoded message type
 */
public class DecodedReaderHandler<T> implements MessageHandler.Whole<Reader> {

  private final SessionImpl session;
  private final Endpoint endpoint;
  private final Decoder.TextStream<T> decoder;
  private final MessageHandler.Whole<T> handler;

  /**
   * Creates the handler adapter.
   *
   * @param session  the owning session
   * @param endpoint the endpoint to receive decode errors
   * @param decoder  the reader-based decoder
   * @param handler  the application handler
   */
  public DecodedReaderHandler (SessionImpl session, Endpoint endpoint, Decoder.TextStream<T> decoder, MessageHandler.Whole<T> handler) {

    this.session = session;
    this.endpoint = endpoint;
    this.decoder = decoder;
    this.handler = handler;
  }

  /**
   * Decodes the incoming reader content and passes the result to the wrapped handler.
   *
   * @param message the incoming reader
   */
  @Override
  public void onMessage (Reader message) {

    try {
      handler.onMessage(decoder.decode(message));
    } catch (DecodeException | IOException exception) {
      endpoint.onError(session, exception);
    }
  }
}
