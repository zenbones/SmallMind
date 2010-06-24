package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileProcessorGopher implements Runnable {

   private CountDownLatch terminationLatch;
   private CountDownLatch exitLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
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

      if (finished.compareAndSet(false, true)) {
         terminationLatch.countDown();
      }

      exitLatch.await();
   }

   public void run () {

      while (!finished.get()) {
         try {
            for (File file : new FileIterator(directory, fileFilter)) {
               fileProcessorQueue.push(file);
            }

            terminationLatch.await(pulse, timeUnit);
         }
         catch (InterruptedException interruptedException) {
            finished.set(true);
         }
      }

      exitLatch.countDown();
   }
}
