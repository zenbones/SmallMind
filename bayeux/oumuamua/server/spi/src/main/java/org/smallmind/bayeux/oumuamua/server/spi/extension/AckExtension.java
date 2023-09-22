package org.smallmind.bayeux.oumuamua.server.spi.extension;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.meta.Meta;

public class AckExtension<V extends Value<V>> extends AbstractServerPacketListener<V> {

  private static final String ACK_FLAG_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.flag";
  private static final String ACK_COUNTER_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.counter";
  private static final String ACK_SIZE_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.size";
  private static final String ACK_MAP_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.ack.map";
  private int maxAckQueueSize;

  public AckExtension (int maxAckQueueSize) {

    this.maxAckQueueSize = maxAckQueueSize;
  }

  @Override
  public void onRequest (Session<V> sender, Packet<V> packet) {

    if (sender != null) {
      if (Meta.HANDSHAKE.getRoute().equals(packet.getRoute())) {
        for (Message<V> message : packet.getMessages()) {

          ObjectValue<V> extValue;

          if ((extValue = message.getExt()) != null) {

            Value<V> ackValue;

            if (((ackValue = extValue.get("ack")) != null) && ValueType.BOOLEAN.equals(ackValue.getType()) && ((BooleanValue<V>)ackValue).asBoolean()) {
              synchronized (sender) {
                if (!Boolean.TRUE.equals(sender.getAttribute(ACK_FLAG_ATTRIBUTE))) {
                  sender.setAttribute(ACK_FLAG_ATTRIBUTE, Boolean.TRUE);
                  sender.setAttribute(ACK_COUNTER_ATTRIBUTE, new AtomicLong(0));
                  sender.setAttribute(ACK_SIZE_ATTRIBUTE, new AtomicLong(0));
                  sender.setAttribute(ACK_MAP_ATTRIBUTE, new ConcurrentSkipListMap<Long, Packet<V>>());
                }
              }
              break;
            }
          }
        }
      } else if (Meta.CONNECT.getRoute().equals(packet.getRoute())) {
        if (Boolean.TRUE.equals(sender.getAttribute(ACK_FLAG_ATTRIBUTE))) {

          Long ackId = null;

          for (Message<V> message : packet.getMessages()) {

            ObjectValue<V> extValue;

            if ((extValue = message.getExt()) != null) {

              Value<V> ackValue;

              if (((ackValue = extValue.get("ack")) != null) && ValueType.NUMBER.equals(ackValue.getType())) {
                if ((ackId == null) || (ackId < ((NumberValue<V>)ackValue).asLong())) {
                  ackId = ((NumberValue<V>)ackValue).asLong();
                }
              }
            }
          }

          if (ackId != null) {

            ConcurrentSkipListMap<Long, Packet<V>> ackMap = (ConcurrentSkipListMap<Long, Packet<V>>)sender.getAttribute(ACK_MAP_ATTRIBUTE);
            AtomicLong ackSize = (AtomicLong)sender.getAttribute(ACK_SIZE_ATTRIBUTE);


          }
        }
      }
    }
  }

  @Override
  public void onResponse (Session<V> sender, Packet<V> packet) {

    if (sender != null) {
      if (Meta.HANDSHAKE.getRoute().equals(packet.getRoute())) {
        if (Boolean.TRUE.equals(sender.getAttribute(ACK_FLAG_ATTRIBUTE))) {
          sender.setLongPolling(true);

          for (Message<V> message : packet.getMessages()) {
            if (message.isSuccessful()) {
              message.getExt(true).put("ack", true);
            }
          }
        }
      } else if (Meta.CONNECT.getRoute().equals(packet.getRoute())) {
        if (Boolean.TRUE.equals(sender.getAttribute(ACK_FLAG_ATTRIBUTE))) {

          Long ackId = null;

          for (Message<V> message : packet.getMessages()) {
            if (message.isSuccessful() && Meta.CONNECT.getRoute().getPath().equals(message.getChannel())) {
              if (ackId == null) {
                ackId = ((AtomicLong)sender.getAttribute(ACK_COUNTER_ATTRIBUTE)).incrementAndGet();
              }

              message.getExt(true).put("ack", ackId);
            }
          }

          if (ackId != null) {

            ConcurrentSkipListMap<Long, Packet<V>> ackMap = (ConcurrentSkipListMap<Long, Packet<V>>)sender.getAttribute(ACK_MAP_ATTRIBUTE);
            AtomicLong ackSize = (AtomicLong)sender.getAttribute(ACK_SIZE_ATTRIBUTE);

            if (ackSize.incrementAndGet() > maxAckQueueSize) {
              if (ackMap.pollLastEntry() != null) {
                ackSize.decrementAndGet();
              }
            }

            ackMap.put(ackId, packet);
          }
        }
      }
    }
  }
}
