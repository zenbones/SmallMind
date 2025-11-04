/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.phalanx.wire.transport.kafka;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.ConversationType;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.smallmind.phalanx.wire.transport.AbstractRequestTransport;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.amqp.rabbitmq.RabbitMQRequestTransport;

public class KafkaRequestTransport extends AbstractRequestTransport {

  private final KafkaConnector connector;
  private final SignalCodec signalCodec;
  private final TopicNames topicNames;
  private final ConcurrentHashMap<String, Producer<Long, byte[]>> producerMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Consumer<Long, byte[]>> consumerMap = new ConcurrentHashMap<>();
  private final String nodeName;
  private final String callerId = SnowflakeId.newInstance().generateDottedString();

  public KafkaRequestTransport (String nodeName, SignalCodec signalCodec, long defaultTimeoutSeconds, KafkaServer... servers) {

    super(defaultTimeoutSeconds);

    this.signalCodec = signalCodec;
    this.nodeName = nodeName;

    topicNames = new TopicNames("wire");
    connector = new KafkaConnector(servers);
  }

  @Override
  public String getCallerId () {

    return callerId;
  }

  private Producer<Long, byte[]> getProducer (String topic) {

    return producerMap.computeIfAbsent(topic, key -> connector.createProducer("wire-producer-" + key + "-" + nodeName));
  }

  private Consumer<Long, byte[]> createConsumer (String topic, int index, String groupId) {

    return consumerMap.computeIfAbsent(topic + "-" + index, key -> connector.createConsumer("wire-consumer-" + index + "-" + key + "-" + nodeName, groupId, key));
  }

  @Override
  public Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    String messageId = SnowflakeId.newInstance().generateDottedString();
    boolean inOnly = voice.getConversation().getConversationType().equals(ConversationType.IN_ONLY);

    String topic = switch (voice.getMode()) {
      case SHOUT -> topicNames.getShoutTopicName((String)voice.getServiceGroup());
      case TALK -> topicNames.getTalkTopicName((String)voice.getServiceGroup());
      case WHISPER -> topicNames.getWhisperTopicName((String)voice.getServiceGroup(), callerId);
    };

    getProducer(topic).send(new ProducerRecord<>(topic, signalCodec.encode(new InvocationSignal(messageId, inOnly, route, arguments, contexts))));

    return Instrument.with(RabbitMQRequestTransport.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.ACQUIRE_RESULT.getDisplay())).on(
      () -> acquireResult(signalCodec, route, voice, messageId, inOnly)
    );
  }

  @Override
  public void close ()
    throws Exception {

  }
}
