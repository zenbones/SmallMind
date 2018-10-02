/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.Address;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.MetricInteraction;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.VocalMode;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.WireContext;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.scribe.pen.LoggerManager;

public class RequestMessageRouter extends MessageRouter {

  private static final String CALLER_ID_AMQP_KEY = "x-opt-" + WireProperty.CALLER_ID.getKey();

  private final RabbitMQRequestTransport requestTransport;
  private final SignalCodec signalCodec;
  private final String callerId;
  private final int index;
  private final int ttlSeconds;

  public RequestMessageRouter (RabbitMQConnector connector, NameConfiguration nameConfiguration, RabbitMQRequestTransport requestTransport, SignalCodec signalCodec, String callerId, int index, int ttlSeconds) {

    super(connector, nameConfiguration);

    this.requestTransport = requestTransport;
    this.signalCodec = signalCodec;
    this.callerId = callerId;
    this.index = index;
    this.ttlSeconds = ttlSeconds;
  }

  @Override
  public final void bindQueues ()
    throws IOException {

    operate(new ChannelOperation() {

      @Override
      public void execute (Channel channel)
        throws IOException {

        String queueName;

        channel.queueDeclare(queueName = getResponseQueueName() + "-" + callerId, false, false, true, null);
        channel.queueBind(queueName, getResponseExchangeName(), "response-" + callerId);
      }
    });
  }

  @Override
  public void installConsumer ()
    throws IOException {

    operate(new ChannelOperation() {

      @Override
      public void execute (Channel channel)
        throws IOException {

        channel.basicConsume(getResponseQueueName() + "-" + callerId, true, getResponseQueueName() + "-" + callerId + "[" + index + "]", false, false, null, new DefaultConsumer(channel) {

          @Override
          public synchronized void handleDelivery (String consumerTag, Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) {

            try {

              long timeInTopic = System.currentTimeMillis() - getTimestamp(properties);

              LoggerManager.getLogger(ResponseMessageRouter.class).debug("response message received(%s) in %d ms...", properties.getMessageId(), timeInTopic);
              InstrumentationManager.instrumentWithChronometer(requestTransport, (timeInTopic >= 0) ? timeInTopic : 0, TimeUnit.MILLISECONDS, new MetricProperty("queue", MetricInteraction.RESPONSE_TRANSIT_TIME.getDisplay()));

              InstrumentationManager.execute(new ChronometerInstrument(requestTransport, new MetricProperty("event", MetricInteraction.COMPLETE_CALLBACK.getDisplay())) {

                @Override
                public void withChronometer ()
                  throws Exception {

                  requestTransport.completeCallback(properties.getCorrelationId(), signalCodec.decode(body, 0, body.length, ResultSignal.class));
                }
              });
            } catch (Exception exception) {
              LoggerManager.getLogger(ResponseMessageRouter.class).error(exception);
            }
          }
        });
      }
    });
  }

  public String publish (final boolean inOnly, final String serviceGroup, final Voice voice, final Address address, final Map<String, Object> arguments, final WireContext... contexts)
    throws Throwable {

    RabbitMQMessage rabbitMQMessage = constructMessage(inOnly, address, arguments, contexts);
    StringBuilder routingKeyBuilder = new StringBuilder(voice.getMode().getName()).append("-").append(serviceGroup);

    if (voice.getMode().equals(VocalMode.WHISPER)) {
      routingKeyBuilder.append('[').append(voice.getInstanceId()).append(']');
    }

    send(routingKeyBuilder.toString(), getRequestExchangeName(), rabbitMQMessage.getProperties(), rabbitMQMessage.getBody());

    return rabbitMQMessage.getProperties().getMessageId();
  }

  private RabbitMQMessage constructMessage (final boolean inOnly, final Address address, final Map<String, Object> arguments, final WireContext... contexts)
    throws Throwable {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<RabbitMQMessage>(requestTransport, new MetricProperty("event", MetricInteraction.CONSTRUCT_MESSAGE.getDisplay())) {

      @Override
      public RabbitMQMessage withChronometer ()
        throws Exception {

        HashMap<String, Object> headerMap = new HashMap<>();

        if (!inOnly) {
          headerMap.put(CALLER_ID_AMQP_KEY, callerId);
        }

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                                            .contentType(signalCodec.getContentType())
                                            .messageId(SnowflakeId.newInstance().generateDottedString())
                                            .timestamp(new Date())
                                            .expiration(String.valueOf(ttlSeconds * 1000))
                                            .headers(headerMap).build();

        return new RabbitMQMessage(properties, signalCodec.encode(new InvocationSignal(inOnly, address, arguments, contexts)));
      }
    });
  }
}

