package org.smallmind.nutsnbolts.util;

import java.nio.ByteBuffer;

public class Bytes {

   public static byte[] getBytes (long l) {

      ByteBuffer translationBuffer;
      byte[] byteArray = new byte[8];

      translationBuffer = ByteBuffer.wrap(byteArray);
      translationBuffer.putLong(l);
      return byteArray;
   }

   public static byte[] getBytes (int i) {

      ByteBuffer translationBuffer;
      byte[] byteArray = new byte[4];

      translationBuffer = ByteBuffer.wrap(byteArray);
      translationBuffer.putInt(i);
      return byteArray;
   }

   public static byte[] getBytes (short s) {

      ByteBuffer translationBuffer;
      byte[] byteArray = new byte[2];

      translationBuffer = ByteBuffer.wrap(byteArray);
      translationBuffer.putShort(s);
      return byteArray;
   }

   public static long getLong (byte[] byteArray) {

      ByteBuffer translationBuffer;

      translationBuffer = ByteBuffer.wrap(byteArray);
      return translationBuffer.getLong();
   }

   public static int getInt (byte[] byteArray) {

      ByteBuffer translationBuffer;

      translationBuffer = ByteBuffer.wrap(byteArray);
      return translationBuffer.getInt();
   }

   public static short getShort (byte[] byteArray) {

      ByteBuffer translationBuffer;

      translationBuffer = ByteBuffer.wrap(byteArray);
      return translationBuffer.getShort();
   }
}
