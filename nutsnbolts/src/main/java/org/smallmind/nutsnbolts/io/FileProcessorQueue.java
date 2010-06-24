package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileProcessorQueue {

   private static enum State {

      STARTED, STOPPED
   }

   private ReentrantLock exclusiveLock;
   private Condition emptyCondition;
   private FileProcessorGopher fileProcessorGopher;
   private LinkedList<File> waitingQueue;
   private HashSet<File> waitingSet;
   private HashSet<File> processingSet;
   private File directory;
   private FileFilter fileFilter;
   private TimeUnit timeUnit;
   private AtomicReference<State> atomicState = new AtomicReference<State>(State.STOPPED);
   private boolean removeEmptyDirectories = false;
   private long pulse;

   public FileProcessorQueue () {

      waitingQueue = new LinkedList<File>();
      waitingSet = new HashSet<File>();
      processingSet = new LinkedHashSet<File>();

      exclusiveLock = new ReentrantLock();
      emptyCondition = exclusiveLock.newCondition();
   }

   public FileProcessorQueue (File directory, long pulse, TimeUnit timeUnit) {

      this(directory, null, false, pulse, timeUnit);
   }

   public FileProcessorQueue (File directory, boolean removeEmptyDirectories, long pulse, TimeUnit timeUnit) {

      this(directory, null, removeEmptyDirectories, pulse, timeUnit);
   }

   public FileProcessorQueue (File directory, FileFilter fileFilter, long pulse, TimeUnit timeUnit) {

      this(directory, fileFilter, false, pulse, timeUnit);
   }

   public FileProcessorQueue (File directory, FileFilter fileFilter, boolean removeEmptyDirectories, long pulse, TimeUnit timeUnit) {

      this();

      this.directory = directory;
      this.fileFilter = fileFilter;
      this.removeEmptyDirectories = removeEmptyDirectories;
      this.pulse = pulse;
      this.timeUnit = timeUnit;
   }

   public void setDirectory (File directory) {

      this.directory = directory;
   }

   public File getDirectory () {

      return directory;
   }

   public void setFileFilter (FileFilter fileFilter) {

      this.fileFilter = fileFilter;
   }

   public void setRemoveEmptyDirectories (boolean removeEmptyDirectories) {

      this.removeEmptyDirectories = removeEmptyDirectories;
   }

   public void setPulse (long pulse) {

      this.pulse = pulse;
   }

   public void setTimeUnit (TimeUnit timeUnit) {

      this.timeUnit = timeUnit;
   }

   public void push (File file) {

      push(file, false);
   }

   public void push (File file, boolean forced) {

      exclusiveLock.lock();
      try {
         if ((forced || atomicState.get().equals(State.STARTED)) && file.exists()) {
            if (!(waitingSet.contains(file) || processingSet.contains(file))) {
               waitingQueue.addLast(file);
               waitingSet.add(file);

               emptyCondition.signal();
            }
         }
      }
      finally {
         exclusiveLock.unlock();
      }
   }

   public File poll ()
      throws InterruptedException {

      return poll(0, TimeUnit.MILLISECONDS);
   }

   public File poll (long timeout, TimeUnit timeUnit)
      throws InterruptedException {

      File file;
      boolean unexpired = true;

      exclusiveLock.lock();
      try {
         while (unexpired && atomicState.get().equals(State.STARTED) && waitingQueue.isEmpty()) {
            unexpired = emptyCondition.await(timeout, timeUnit);
         }

         if (unexpired && atomicState.get().equals(State.STARTED)) {
            waitingSet.remove(file = waitingQueue.removeFirst());
            processingSet.add(file);

            return file;
         }

         return null;
      }
      finally {
         exclusiveLock.unlock();
      }
   }

   public void delete (File file)
      throws FileProcessingException {

      exclusiveLock.lock();
      try {
         if (!processingSet.contains(file)) {
            throw new FileProcessingException("File(%s) is not currently marked for processing", file.getAbsolutePath());
         }

         processingSet.remove(file);
         if (!file.delete()) {
            throw new FileProcessingException("Unable to delete the file(%s)", file.getAbsolutePath());
         }

         if (removeEmptyDirectories && (file.getParentFile().list().length == 0)) {
            if (!file.getParentFile().delete()) {
               throw new FileProcessingException("Unable to delete the empty directory(%s)", file.getParentFile().getAbsolutePath());
            }
         }
      }
      finally {
         exclusiveLock.unlock();
      }
   }

   public void start () {

      if (atomicState.compareAndSet(State.STOPPED, State.STARTED)) {
         new Thread(fileProcessorGopher = new FileProcessorGopher(this, directory, fileFilter, pulse, timeUnit)).start();
      }
   }

   public void stop ()
      throws InterruptedException {

      if (atomicState.compareAndSet(State.STARTED, State.STOPPED)) {
         fileProcessorGopher.finish();
      }
   }
}