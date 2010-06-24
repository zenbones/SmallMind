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
