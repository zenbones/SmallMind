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
package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FileProcessorGopher implements Runnable {

   private CountDownLatch terminationLatch;
   private CountDownLatch exitLatch;
   private FileProcessorQueue fileProcessorQueue;
   private File directory;
   private FileFilter fileFilter;
   private TimeUnit timeUnit;
   private long pulse;

   public FileProcessorGopher (FileProcessorQueue fileProcessorQueue, File directory, FileFilter fileFilter, long pulse, TimeUnit timeUnit) {

      this.fileProcessorQueue = fileProcessorQueue;
      this.directory = directory;
      this.fileFilter = fileFilter;
      this.pulse = pulse;
      this.timeUnit = timeUnit;

      terminationLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
   }

   public void finish ()
      throws InterruptedException {

      terminationLatch.countDown();
      exitLatch.await();
   }

   public void run () {

      try {
         do {
            for (File file : new FileIterator(directory, fileFilter)) {
               fileProcessorQueue.push(file);
            }

         } while (!terminationLatch.await(pulse, timeUnit));
      }
      catch (InterruptedException interruptedException) {
         terminationLatch.countDown();
      }
      finally {
         exitLatch.countDown();
      }
   }
}
