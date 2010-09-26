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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.io.InputStream;

public class ParrotInputStream extends InputStream {

   private InputStream inputStream;

   public ParrotInputStream (InputStream inputStream) {

      this.inputStream = inputStream;
   }

   public synchronized int read ()
      throws IOException {

      int readValue;

      if ((readValue = inputStream.read()) >= 0) {
         System.out.println(readValue + ":" + (char)readValue);
      }

      return readValue;
   }

   public synchronized int read (byte buf[])
      throws IOException {

      int readValue;

      if ((readValue = inputStream.read(buf)) >= 0) {
         for (byte aByte : buf) {
            System.out.println(aByte + ":" + (char)aByte);
         }
      }

      return readValue;
   }

   public synchronized int read (byte buf[], int off, int len)
      throws IOException {

      int readValue;

      if ((readValue = inputStream.read(buf, off, len)) >= 0) {
         for (int count = off; count < off + len; count++) {
            System.out.println(buf[count] + ":" + (char)buf[count]);
         }
      }

      return readValue;
   }

   public synchronized long skip (long n)
      throws IOException {

      long skipValue;

      skipValue = inputStream.skip(n);

      return skipValue;
   }

   public synchronized void mark (int readAheadLimit) {

      inputStream.mark(readAheadLimit);
   }

   public synchronized void reset ()
      throws IOException {

      inputStream.reset();
   }

   public void close ()
      throws IOException {

      inputStream.close();
   }

}
