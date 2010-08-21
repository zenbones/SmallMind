package org.smallmind.swing.io;

import java.io.IOException;
import java.io.InputStream;
import org.smallmind.swing.progress.ProgressDataHandler;
import org.smallmind.swing.progress.ProgressPanel;

public class ProgressInputStream extends InputStream implements ProgressDataHandler {

   private ProgressPanel progressPanel;
   private InputStream inputStream;
   private long length;
   private long index;
   private long markIndex;

   public ProgressInputStream (InputStream inputStream, long length, long pulseTime) {

      this.inputStream = inputStream;
      this.length = length;

      index = 0;
      markIndex = 0;

      progressPanel = new ProgressPanel(this, pulseTime);
   }

   public long getLength () {

      return length;
   }

   public synchronized long getIndex () {

      return index;
   }

   public ProgressPanel getIOProgressPanel () {

      return progressPanel;
   }

   public synchronized int read ()
      throws IOException {

      int readValue;

      if ((readValue = inputStream.read()) >= 0) {
         index++;
      }

      return readValue;
   }

   public synchronized int read (byte buf[])
      throws IOException {

      int readValue;

      if ((readValue = inputStream.read(buf)) >= 0) {
         index += readValue;
      }

      return readValue;
   }

   public synchronized int read (byte buf[], int off, int len)
      throws IOException {

      int readValue;

      if ((readValue = inputStream.read(buf, off, len)) >= 0) {
         index += readValue;
      }

      return readValue;
   }

   public synchronized long skip (long n)
      throws IOException {

      long skipValue;

      skipValue = inputStream.skip(n);
      index += skipValue;

      return skipValue;
   }

   public synchronized void mark (int readAheadLimit) {

      inputStream.mark(readAheadLimit);
      markIndex = index;
   }

   public synchronized void reset ()
      throws IOException {

      inputStream.reset();
      index = markIndex;
   }

   public void close ()
      throws IOException {

      inputStream.close();
   }

}
