package org.smallmind.nutsnbolts.swing.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import org.smallmind.nutsnbolts.swing.progress.ProgressDataHandler;
import org.smallmind.nutsnbolts.swing.progress.ProgressPanel;

public class ProgressReader extends Reader implements ProgressDataHandler {

   private ProgressPanel progressPanel;
   private Reader reader;
   private char[] separatorArray;
   private long length;
   private long index;
   private long markIndex;

   public ProgressReader (Reader reader, long length, long pulseTime, String lineSeperator) {

      this.reader = reader;
      this.length = length;

      separatorArray = lineSeperator.toCharArray();
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

      if (index >= length) {
         throw new EOFException("Unexpected file termination");
      }

      if ((readValue = reader.read()) >= 0) {
         index++;
      }

      return readValue;
   }

   public synchronized int read (char cbuf[])
      throws IOException {

      int readValue;

      if (index >= length) {
         throw new EOFException("Unexpected file termination");
      }

      if ((readValue = reader.read(cbuf)) >= 0) {
         index += readValue;
      }

      return readValue;
   }

   public synchronized int read (char cbuf[], int off, int len)
      throws IOException {

      int readValue;

      if (index >= length) {
         throw new EOFException("Unexpected file termination");
      }

      if ((readValue = reader.read(cbuf, off, len)) >= 0) {
         index += readValue;
      }

      return readValue;
   }

   public String readLine ()
      throws IOException {

      StringBuilder lineBuilder;
      boolean eol = false;
      int[] bufferArray = new int[separatorArray.length];
      int oneChar;

      if (index >= length) {
         return null;
      }

      for (int count = 0; count < bufferArray.length; count++) {
         bufferArray[count] = 0;
      }

      lineBuilder = new StringBuilder();
      do {
         oneChar = read();
         if (oneChar > 0) {
            if (bufferArray[0] > 0) {
               lineBuilder.append((char)bufferArray[0]);
            }

            System.arraycopy(bufferArray, 1, bufferArray, 0, bufferArray.length - 1);
            bufferArray[bufferArray.length - 1] = oneChar;

            eol = true;
            for (int count = 0; count < bufferArray.length; count++) {
               if (bufferArray[count] != separatorArray[count]) {
                  eol = false;
                  break;
               }
            }

            if (eol) {
               break;
            }
         }
      } while ((index < length) && (oneChar >= 0));

      if (!eol) {
         for (int bufferToken : bufferArray) {
            if (bufferToken > 0) {
               lineBuilder.append((char)bufferToken);
            }
         }
      }

      if (lineBuilder.length() == 0) {
         return null;
      }

      return lineBuilder.toString();
   }

   public synchronized long skip (long n)
      throws IOException {

      long skipValue;

      if (index >= length) {
         throw new EOFException("Unexpected file termination");
      }

      skipValue = reader.skip(n);
      index += skipValue;

      return skipValue;
   }

   public synchronized void mark (int readAheadLimit)
      throws IOException {

      reader.mark(readAheadLimit);
      markIndex = index;
   }

   public synchronized void reset ()
      throws IOException {

      reader.reset();
      index = markIndex;
   }

   public void close ()
      throws IOException {

      reader.close();
   }

}
