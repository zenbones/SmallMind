package org.smallmind.cloud.multicast.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.TreeMap;

public class EventMessageMold {

   private TreeMap<Integer, byte[]> messageSegmentMap;
   private int messageLength = 0;
   private int totalBytes = 0;

   public EventMessageMold () {

      messageSegmentMap = new TreeMap<Integer, byte[]>();
   }

   public void setMessageLength (int messageLength) {

      this.messageLength = messageLength;
   }

   public void addData (int messageIndex, byte[] messageSegment) {

      messageSegmentMap.put(messageIndex, messageSegment);
      totalBytes += messageSegment.length;
   }

   public boolean isComplete () {

      return (messageLength != 0) && (totalBytes >= messageLength);
   }

   public Object unmoldMessageBody ()
      throws IOException, ClassNotFoundException {

      Object unmoldedObject;
      ObjectInputStream objectInputStream;
      ByteArrayOutputStream byteOutputStream;
      Iterator<Integer> segmentKeyIter;

      if (totalBytes == 0) {
         return null;
      }

      byteOutputStream = new ByteArrayOutputStream(totalBytes);
      segmentKeyIter = messageSegmentMap.keySet().iterator();
      while (segmentKeyIter.hasNext()) {
         byteOutputStream.write(messageSegmentMap.get(segmentKeyIter.next()));
      }
      byteOutputStream.close();

      objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteOutputStream.toByteArray()));
      unmoldedObject = objectInputStream.readObject();
      objectInputStream.close();

      return unmoldedObject;
   }

}
