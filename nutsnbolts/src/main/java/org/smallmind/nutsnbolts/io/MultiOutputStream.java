package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.io.OutputStream;

public class MultiOutputStream extends OutputStream {

   private OutputStream[] streams;

   public MultiOutputStream (OutputStream[] streams) {

      this.streams = streams;
   }

   public void write (int b)
      throws IOException {

      for (int count = 0; count < streams.length; count++) {
         streams[count].write(new byte[] {(byte)b});
      }
   }

   public void write (byte buffer[])
      throws IOException {

      for (int count = 0; count < streams.length; count++) {
         streams[count].write(buffer, 0, buffer.length);
      }
   }

   public void write (byte buffer[], int off, int len)
      throws IOException {

      for (int count = 0; count < streams.length; count++) {
         streams[count].write(buffer, off, len);
      }
   }

}
