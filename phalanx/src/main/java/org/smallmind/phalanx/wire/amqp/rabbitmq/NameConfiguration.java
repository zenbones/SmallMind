/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.phalanx.wire.amqp.rabbitmq;

public class NameConfiguration {

  private String requestExchange = "requestExchange";
  private String responseExchange = "responseExchange";
  private String responseQueue = "responseQueue";
  private String talkQueue = "talkQueue";
  private String whisperQueue = "whisperQueue";

  public String getRequestExchange () {

    return requestExchange;
  }

  public void setRequestExchange (String requestExchange) {

    this.requestExchange = requestExchange;
  }

  public String getResponseExchange () {

    return responseExchange;
  }

  public void setResponseExchange (String responseExchange) {

    this.responseExchange = responseExchange;
  }

  public String getResponseQueue () {

    return responseQueue;
  }

  public void setResponseQueue (String responseQueue) {

    this.responseQueue = responseQueue;
  }

  public String getTalkQueue () {

    return talkQueue;
  }

  public void setTalkQueue (String talkQueue) {

    this.talkQueue = talkQueue;
  }

  public String getWhisperQueue () {

    return whisperQueue;
  }

  public void setWhisperQueue (String whisperQueue) {

    this.whisperQueue = whisperQueue;
  }
}
