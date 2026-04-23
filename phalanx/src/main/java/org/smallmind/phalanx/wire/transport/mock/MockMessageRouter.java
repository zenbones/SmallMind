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

/**
 * In-memory router that exposes the shared queue and topics used by the mock transport for testing.
 */
public class MockMessageRouter {

  private final MockQueue talkRequestQueue = new MockQueue();
  private final MockTopic whisperRequestTopic = new MockTopic();
  private final MockTopic responseTopic = new MockTopic();

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
}
