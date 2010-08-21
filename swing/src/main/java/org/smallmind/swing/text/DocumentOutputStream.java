package org.smallmind.swing.text;

import java.io.IOException;
import java.io.OutputStream;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class DocumentOutputStream extends OutputStream {

   private Document document;
   private int cursor = 0;

   public DocumentOutputStream (Document document) {

      this.document = document;
   }

   public void write (int b)
      throws IOException {

      write(new byte[] {(byte)b});
   }

   public void write (byte buffer[])
      throws IOException {

      write(buffer, 0, buffer.length);
   }

   public void write (byte buffer[], int off, int len)
      throws IOException {

      try {
         document.insertString(cursor, new String(buffer, off, len), null);
         cursor += len - off;
      }
      catch (BadLocationException badLocationException) {
         IOException ioException;

         ioException = new IOException(badLocationException.getMessage());
         ioException.initCause(badLocationException);

         throw ioException;
      }
   }

}
