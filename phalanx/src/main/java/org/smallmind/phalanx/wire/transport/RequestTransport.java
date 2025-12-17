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
package org.smallmind.phalanx.wire.transport;

import java.util.Map;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.WireContext;

/**
 * Transport interface for sending invocation requests over a chosen medium.
 */
public interface RequestTransport {

  /**
   * Returns an identifier for the caller, used to correlate responses.
   *
   * @return caller id string
   */
  String getCallerId ();

  /**
   * Transmits an invocation with routing information, arguments, and contexts.
   *
   * @param voice     voice describing destination and conversation style
   * @param route     route identifying the service and function
   * @param arguments serialized argument map
   * @param contexts  wire contexts to propagate
   * @return result of the call, or {@code null} for in-only conversations
   * @throws Throwable if transport submission fails or the remote call raises an error
   */
  Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable;

  /**
   * Completes a previously issued asynchronous invocation by delivering a result.
   *
   * @param correlationId id correlating the result to the original request
   * @param resultSignal  signal containing the outcome
   */
  void completeCallback (String correlationId, ResultSignal resultSignal);

  /**
   * Releases any resources held by the transport.
   *
   * @throws Exception if shutdown fails
   */
  void close ()
    throws Exception;
}
