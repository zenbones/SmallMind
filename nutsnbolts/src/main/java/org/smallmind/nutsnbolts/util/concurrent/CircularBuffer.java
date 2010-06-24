package org.smallmind.nutsnbolts.util.concurrent;

public class CircularBuffer {

   private static enum State {

      READ, WRITE
   }

   private RangeSegment[] segments;
   private boolean closed = false;
   private int position = 0;
   private int filled = 0;
   private byte[] buffer;

   public CircularBuffer (int size) {

      buffer = new byte[size];
      segments = new RangeSegment[2];
   }

   public synchronized boolean isClosed () {

      return closed;
   }

   public synchronized void close () {

      closed = true;
      notifyAll();
   }

   public synchronized int available () {

      return (buffer.length - filled);
   }

   public int read (byte[] data) {

      return read(data, 0);
   }

   public synchronized int read (byte[] data, long millis) {

      int bytesRead;

      if (data.length == 0) {
         return 0;
      }

      while ((bytesRead = get(data)) == 0) {
         if (closed) {
            return -1;
         }

         try {
            wait(millis);
         }
         catch (InterruptedException i) {
         }
      }

      notifyAll();
      return bytesRead;
   }

   public void write (byte[] data) {

      write(data, 0, data.length);
   }

   public synchronized void write (byte[] data, int off, int length) {

      int totalBytes = 0;
      int bytesWritten;

      if (closed) {
         throw new IllegalStateException("The close() method has previously been called");
      }

      do {
         if ((bytesWritten = put(data, totalBytes, length - totalBytes)) > 0) {
            totalBytes += bytesWritten;
            notifyAll();
         }

         if (totalBytes < length) {
            try {
               wait();
            }
            catch (InterruptedException i) {
            }
         }
      } while (totalBytes < length);
   }

   private int put (byte[] data, int off, int length) {

      int totalBytes = 0;
      int writeBytes;

      setSegments(State.WRITE);
      for (int count = 0; count < segments.length; count++) {
         if (segments[count] != null) {
            writeBytes = Math.min(segments[count].getStop() - segments[count].getStart(), length - totalBytes);
            if (writeBytes > 0) {
               System.arraycopy(data, off + totalBytes, buffer, segments[count].getStart(), writeBytes);
               totalBytes += writeBytes;
            }
         }
      }

      filled += totalBytes;
      return totalBytes;
   }

   private int get (byte[] data) {

      int totalBytes = 0;
      int readBytes;

      setSegments(State.READ);
      for (int count = 0; count < segments.length; count++) {
         if (segments[count] != null) {
            readBytes = Math.min(segments[count].getStop() - segments[count].getStart(), data.length - totalBytes);
            if (readBytes > 0) {
               System.arraycopy(buffer, segments[count].getStart(), data, totalBytes, readBytes);
               totalBytes += readBytes;
            }
         }
      }

      filled -= totalBytes;
      position += totalBytes;
      if (position > buffer.length) {
         position -= buffer.length;
      }

      return totalBytes;
   }

   private void setSegments (State state) {

      if (position + filled <= buffer.length) {
         switch (state) {
            case READ:
               segments[0] = new RangeSegment(position, position + filled);
               segments[1] = null;
               break;
            case WRITE:
               segments = new RangeSegment[2];
               segments[0] = new RangeSegment(position + filled, buffer.length);
               segments[1] = new RangeSegment(0, position);
         }
      }
      else {
         switch (state) {
            case READ:
               segments = new RangeSegment[2];
               segments[0] = new RangeSegment(position, buffer.length);
               segments[1] = new RangeSegment(0, position + filled - buffer.length);
               break;
            case WRITE:
               segments[0] = new RangeSegment(position + filled - buffer.length, position);
               segments[1] = null;
         }
      }
   }

   public class RangeSegment {

      private int start;
      private int stop;

      public RangeSegment (int start, int stop) {

         this.start = start;
         this.stop = stop;
      }

      public int getStart () {

         return start;
      }

      public int getStop () {

         return stop;
      }

   }

}
