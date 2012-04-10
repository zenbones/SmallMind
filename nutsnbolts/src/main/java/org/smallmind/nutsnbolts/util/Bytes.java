/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
