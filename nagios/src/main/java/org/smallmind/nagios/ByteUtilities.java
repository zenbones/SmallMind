/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nagios;

public class ByteUtilities {

   public static void writeFixedString (byte[] dest, String value, int offset, int fixedSize) {

      System.arraycopy(getFixedSizeBytes(fixedSize, value), 0, dest, offset, fixedSize);
   }

   public static void writeShort (byte[] dest, short value, int offset) {

      System.arraycopy(ByteUtilities.shortToByteArray(value), 0, dest, offset, 2);
   }

   public static void writeInteger (byte[] dest, int value, int offset) {

      System.arraycopy(ByteUtilities.intToByteArray(value), 0, dest, offset, 4);
   }

   public static byte[] getFixedSizeBytes (int fixedSize, String value) {

      byte[] data = new byte[fixedSize];

      if (value == null) {
         return data;
      }

      System.arraycopy(value.getBytes(), 0, data, 0, Math.min(value.getBytes().length, fixedSize));

      return data;
   }

   public static byte[] intToByteArray (int value) {

      final byte[] data = new byte[4];

      for (int i = 0; i < data.length; i++) {
         int offset = (data.length - 1 - i) * 8;
         data[i] = (byte)((value >>> offset) & 0xFF);
      }

      return data;
   }

   public static byte[] shortToByteArray (short value) {

      final byte[] data = new byte[2];

      for (int i = 0; i < data.length; i++) {
         int offset = (data.length - 1 - i) * 8;
         data[i] = (byte)((value >>> offset) & 0xFF);
      }

      return data;
   }
}

