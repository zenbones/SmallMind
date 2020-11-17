/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.phalanx.wire.mock;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.smallmind.phalanx.wire.AbstractRequestTransport;
import org.smallmind.phalanx.wire.ConversationType;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.Route;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.VocalMode;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.WireContext;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.scribe.pen.LoggerManager;

public class MockRequestTransport extends AbstractRequestTransport {

  private final MockMessageRouter messageRouter;
  private final SignalCodec signalCodec;
  private final String callerId = UUID.randomUUID().toString();

  public MockRequestTransport (MockMessageRouter messageRouter, final SignalCodec signalCodec, int defaultTimeoutSeconds) {

    super(defaultTimeoutSeconds);

    this.messageRouter = messageRouter;
    this.signalCodec = signalCodec;

    messageRouter.getResponseTopic().addListener(new MockMessageListener() {

      @Override
      public boolean match (MockMessageProperties properties) {

        return properties.getHeader(WireProperty.CALLER_ID.getKey()).equals(callerId);
      }

      @Override
      public void handle (MockMessage message) {

        try {
          completeCallback(new String(message.getProperties().getCorrelationId()), signalCodec.decode(message.getBytes(), 0, message.getBytes().length, ResultSignal.class));
        } catch (Exception exception) {
          LoggerManager.getLogger(MockRequestTransport.class).error(exception);
        }
      }
    });
  }

  @Override
  public String getCallerId () {

    return callerId;
  }

  @Override
  public Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    MockMessage message;
    String messageId = UUID.randomUUID().toString();
    boolean inOnly = voice.getConversation().getConversationType().equals(ConversationType.IN_ONLY);

    message = new MockMessage(signalCodec.encode(new InvocationSignal(inOnly, route, arguments, contexts)));

    if (!inOnly) {
      message.getProperties().setHeader(WireProperty.CALLER_ID.getKey(), callerId);
    }

    message.getProperties().setMessageId(messageId);
    message.getProperties().setTimestamp(new Date());
    message.getProperties().setContentType(signalCodec.getContentType());
    message.getProperties().setHeader(WireProperty.CLOCK.getKey(), System.currentTimeMillis());
    message.getProperties().setHeader(WireProperty.SERVICE_GROUP.getKey(), voice.getServiceGroup());

    if (voice.getMode().equals(VocalMode.WHISPER)) {
      message.getProperties().setHeader(WireProperty.INSTANCE_ID.getKey(), voice.getInstanceId());
      messageRouter.getWhisperRequestTopic().send(message);
    } else {
      messageRouter.getTalkRequestQueue().send(message);
    }

    return acquireResult(signalCodec, route, voice, messageId, inOnly);
  }

  @Override
  public void close ()
    throws Exception {

    getCallbackMap().shutdown();
  }
}
