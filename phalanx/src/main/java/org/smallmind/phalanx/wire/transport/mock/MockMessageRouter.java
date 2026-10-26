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
package org.smallmind.phalanx.wire.transport.mock;

import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.phalanx.worker.BindingAdvisory;

/**
 * In-memory router that exposes the shared queue and topics used by the mock transport for testing.
 */
public class MockMessageRouter {

  private final MockQueue talkRequestQueue;
  private final MockTopic whisperRequestTopic;
  private final MockTopic responseTopic;

  /**
   * Constructs a router whose queue and topics dispatch inside the given scope.
   *
   * @param perApplicationScope the scope every dispatch thread runs inside, or {@code null} to run them with no
   *                            scope, carrying only the {@code PerApplicationContext} of the thread constructing
   *                            this router (the mock transports take no scope of their own); a WARN is logged
   *                            through {@code BindingAdvisory} when this is {@code null} but the constructing thread
   *                            is bound to a scope and carries no context
   */
  public MockMessageRouter (PerApplicationScope perApplicationScope) {

    BindingAdvisory.warnIfUnbound(MockMessageRouter.class, "Mock message router", perApplicationScope);

    talkRequestQueue = new MockQueue("talk-request", perApplicationScope);
    whisperRequestTopic = new MockTopic("whisper-request", perApplicationScope);
    responseTopic = new MockTopic("response", perApplicationScope);
  }

  /**
   * Returns the queue used to dispatch talk-mode invocation requests.
   *
   * @return talk request queue.
   */
  public MockQueue getTalkRequestQueue () {

    return talkRequestQueue;
  }

  /**
   * Returns the topic used to dispatch whisper-mode invocation requests.
   *
   * @return whisper request topic.
   */
  public MockTopic getWhisperRequestTopic () {

    return whisperRequestTopic;
  }

  /**
   * Returns the topic used to deliver invocation results back to callers.
   *
   * @return response topic.
   */
  public MockTopic getResponseTopic () {

    return responseTopic;
  }

  /**
   * Stops the three dispatch threads this router started. The transports built on the router should be closed first;
   * messages still queued when this is called are dropped.
   */
  public void close () {

    talkRequestQueue.close();
    whisperRequestTopic.close();
    responseTopic.close();
  }
}
