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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

/**
 * Holds the logical name suffixes for RabbitMQ exchanges and queues used by the phalanx transports.
 */
public class NameConfiguration {

  private String requestExchange = "requestExchange";
  private String responseExchange = "responseExchange";
  private String responseQueue = "responseQueue";
  private String shoutQueue = "shoutQueue";
  private String talkQueue = "talkQueue";
  private String whisperQueue = "whisperQueue";

  /**
   * Returns the suffix applied to the request exchange name.
   *
   * @return request exchange name suffix.
   */
  public String getRequestExchange () {

    return requestExchange;
  }

  /**
   * Sets the suffix applied to the request exchange name.
   *
   * @param requestExchange request exchange name suffix.
   */
  public void setRequestExchange (String requestExchange) {

    this.requestExchange = requestExchange;
  }

  /**
   * Returns the suffix applied to the response exchange name.
   *
   * @return response exchange name suffix.
   */
  public String getResponseExchange () {

    return responseExchange;
  }

  /**
   * Sets the suffix applied to the response exchange name.
   *
   * @param responseExchange response exchange name suffix.
   */
  public void setResponseExchange (String responseExchange) {

    this.responseExchange = responseExchange;
  }

  /**
   * Returns the suffix applied to the response queue name.
   *
   * @return response queue name suffix.
   */
  public String getResponseQueue () {

    return responseQueue;
  }

  /**
   * Sets the suffix applied to the response queue name.
   *
   * @param responseQueue response queue name suffix.
   */
  public void setResponseQueue (String responseQueue) {

    this.responseQueue = responseQueue;
  }

  /**
   * Returns the suffix applied to the shout queue name.
   *
   * @return shout queue name suffix.
   */
  public String getShoutQueue () {

    return shoutQueue;
  }

  /**
   * Sets the suffix applied to the shout queue name.
   *
   * @param shoutQueue shout queue name suffix.
   */
  public void setShoutQueue (String shoutQueue) {

    this.shoutQueue = shoutQueue;
  }

  /**
   * Returns the suffix applied to the talk queue name.
   *
   * @return talk queue name suffix.
   */
  public String getTalkQueue () {

    return talkQueue;
  }

  /**
   * Sets the suffix applied to the talk queue name.
   *
   * @param talkQueue talk queue name suffix.
   */
  public void setTalkQueue (String talkQueue) {

    this.talkQueue = talkQueue;
  }

  /**
   * Returns the suffix applied to the whisper queue name.
   *
   * @return whisper queue name suffix.
   */
  public String getWhisperQueue () {

    return whisperQueue;
  }

  /**
   * Sets the suffix applied to the whisper queue name.
   *
   * @param whisperQueue whisper queue name suffix.
   */
  public void setWhisperQueue (String whisperQueue) {

    this.whisperQueue = whisperQueue;
  }
}
