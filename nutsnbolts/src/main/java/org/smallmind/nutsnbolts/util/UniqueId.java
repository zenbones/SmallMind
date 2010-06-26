package org.smallmind.nutsnbolts.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.Arrays;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

public class UniqueId implements Comparable<UniqueId> {

   private static final int[] DOT_OFFSET_0 = {0, 0, 0};
   private static final int[] DOT_OFFSET_1 = {0, 0, 0};
   private static final int[] DOT_OFFSET_2 = {0, 1, 0};
   private static final int[] DOT_OFFSET_3 = {0, 1, 1};

   private static final int CODE_TEMPLATE_BITS = 5;
   private static final String CODE_TEMPLATE = "ABCDEFGHIJKMNPQRSTUVWXYZ23456789";

   private static final int COMPACT_CODE_TEMPLATE_BITS = 6;
   private static final String COMPACT_CODE_TEMPLATE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890$*";

   private static final int[] POWER_ARRAY = {1, 2, 4, 8, 16, 32, 64, 128};

   private static final SecureRandom RANDOM = new SecureRandom(Bytes.getBytes(System.currentTimeMillis()));

   private static final byte[] MAC_BYTES = new byte[6];
   private static final byte[] JVM_BYTES = new byte[2];

   private static long TIME;
   private static byte[] TIME_BYTES;
   private static short COUNT = Short.MIN_VALUE;

   private final byte[] uniqueArray;

   static {

      byte[] macAddress;

      try {
         macAddress = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
         System.arraycopy(macAddress, 0, MAC_BYTES, 0, 6);
      }
      catch (Exception exception) {
         throw new StaticInitializationError(exception);
      }

      TIME = System.currentTimeMillis();
      TIME_BYTES = Bytes.getBytes(TIME);
      RANDOM.nextBytes(JVM_BYTES);
   }

   public static int byteSize () {

      return 18;
   }

   private synchronized static byte[] generateByteArray () {

      byte[] bytes = new byte[18];

      if (COUNT++ == Short.MAX_VALUE) {

         long currentTime = System.currentTimeMillis();

         if (currentTime <= TIME) {
           TIME += 1;
         }
         else {
            TIME = currentTime;
         }

         TIME_BYTES = Bytes.getBytes(TIME);
      }

      System.arraycopy(MAC_BYTES, 0, bytes, 0, 6);
      System.arraycopy(JVM_BYTES, 0, bytes, 6, 2);
      System.arraycopy(TIME_BYTES, 0, bytes, 8, 8);
      System.arraycopy(Bytes.getBytes(COUNT), 0, bytes, 16, 2);

      return bytes;
   }

   public static UniqueId newInstance () {

      return new UniqueId();
   }

   public UniqueId () {

      uniqueArray = generateByteArray();
   }

   public UniqueId (byte[] uniqueArray) {

      this.uniqueArray = uniqueArray;
   }

   public byte[] asByteArray () {

      return uniqueArray;
   }

   public BigInteger generateBigInteger () {

      return new BigInteger(uniqueArray);
   }

   public String generateCompactString () {

      return generateTemplateString(COMPACT_CODE_TEMPLATE, COMPACT_CODE_TEMPLATE_BITS).toString();
   }

   public String generateDottedString () {

      StringBuilder dottedIdBuilder = generateTemplateString(CODE_TEMPLATE, CODE_TEMPLATE_BITS);
      int dashSize;

      dashSize = dottedIdBuilder.length() / 4;
      switch (dottedIdBuilder.length() % 4) {
         case 0:
            return insertDots(dottedIdBuilder, DOT_OFFSET_0, dashSize);
         case 1:
            return insertDots(dottedIdBuilder, DOT_OFFSET_1, dashSize);
         case 2:
            return insertDots(dottedIdBuilder, DOT_OFFSET_2, dashSize);
         default:
            return insertDots(dottedIdBuilder, DOT_OFFSET_3, dashSize);
      }
   }

   private String insertDots (StringBuilder dottedIdBuilder, int[] offsets, int dashSize) {

      for (int count = 0; count < offsets.length; count++) {
         dottedIdBuilder.insert((dashSize * (count + 1)) + count + offsets[count], '.');
      }

      return dottedIdBuilder.toString();
   }

   private StringBuilder generateTemplateString (String template, int templateBits) {

      StringBuilder uniqueIdBuilder = new StringBuilder();

      int codeIndex = 0;
      int codeValue = 0;

      for (byte codeByte : uniqueArray) {
         for (int count = 0; count < 8; count++) {
            if ((codeByte & POWER_ARRAY[count]) != 0) {
               codeValue += POWER_ARRAY[codeIndex];
            }

            if (++codeIndex == templateBits) {
               uniqueIdBuilder.append(template.charAt(codeValue));
               codeIndex = 0;
               codeValue = 0;
            }
         }
      }

      if (codeIndex > 0) {
         uniqueIdBuilder.append(CODE_TEMPLATE.charAt(codeValue));
      }

      return uniqueIdBuilder;
   }

   @Override
   public int hashCode () {

      return Arrays.hashCode(uniqueArray);
   }

   @Override
   public boolean equals (Object obj) {

      return (obj instanceof UniqueId) && Arrays.equals(uniqueArray, ((UniqueId)obj).asByteArray());
   }

   public int compareTo (UniqueId uniqueId) {

      int comparison;

      if ((comparison = compareTimeBytes(uniqueId)) == 0) {
         if ((comparison = compareCountBytes(uniqueId)) == 0) {
            if ((comparison = compareIPBytes(uniqueId)) == 0) {

               return compareJVMBytes(uniqueId);
            }
         }
      }

      return comparison;
   }

   private int compareIPBytes (UniqueId uniqueId) {

      int comparison;

      for (int count = 0; count < 6; count++) {
         if ((comparison = this.asByteArray()[count] - uniqueId.asByteArray()[count]) != 0) {

            return comparison;
         }
      }

      return 0;
   }

   private int compareJVMBytes (UniqueId uniqueId) {

      int comparison;

      for (int count = 6; count < 8; count++) {
         if ((comparison = this.asByteArray()[count] - uniqueId.asByteArray()[count]) != 0) {

            return comparison;
         }
      }

      return 0;
   }

   private int compareTimeBytes (UniqueId uniqueId) {

      return (int)(Bytes.getLong(Arrays.copyOfRange(this.asByteArray(), 8, 16)) - Bytes.getLong(Arrays.copyOfRange(uniqueId.asByteArray(), 8, 16)));
   }

   private int compareCountBytes (UniqueId uniqueId) {

      return Bytes.getShort(Arrays.copyOfRange(this.asByteArray(), 16, 18)) - Bytes.getShort(Arrays.copyOfRange(uniqueId.asByteArray(), 16, 18));
   }
}
