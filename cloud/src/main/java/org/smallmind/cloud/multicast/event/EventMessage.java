package org.smallmind.cloud.multicast.event;

import java.nio.ByteBuffer;
import org.smallmind.nutsnbolts.util.UniqueId;

public abstract class EventMessage {

   public static final int MESSAGE_HEADER_SIZE = UniqueId.byteSize() + 16;

   private ByteBuffer translationBuffer;

   public EventMessage (byte[] messageId, MessageType messageType, int messageLength, int extraSize) {

      byte[] messageArray;

      messageArray = new byte[messageId.length + 12 + extraSize];
      translationBuffer = ByteBuffer.wrap(messageArray);

      translationBuffer.putInt(MessageStatus.MULTICAST.ordinal());
      translationBuffer.put(messageId);
      translationBuffer.putInt(messageType.ordinal());
      translationBuffer.putInt(messageLength);
   }

   public void put (byte[] b) {

      translationBuffer.put(b);
   }

   public void putInt (int i) {

      translationBuffer.putInt(i);
   }

   public void putLong (long l) {

      translationBuffer.putLong(l);
   }

   public ByteBuffer getByteBuffer () {

      return translationBuffer;
   }

}
