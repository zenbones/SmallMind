package org.smallmind.cloud.multicast.event;

import java.util.Arrays;

public class EventMessageKey {

   private byte[] keyArray;

   public EventMessageKey (byte[] keyArray) {

      this.keyArray = new byte[keyArray.length];
      System.arraycopy(keyArray, 0, this.keyArray, 0, keyArray.length);
   }

   public byte[] getKey () {

      return keyArray;
   }

   public int hashCode () {

      int hashCode = 0;

      for (byte keyByte : keyArray) {
         hashCode += keyByte;
      }

      return hashCode;
   }

   public boolean equals (Object object) {

      return (object instanceof EventMessageKey) && Arrays.equals(keyArray, ((EventMessageKey)object).getKey());
   }

}
