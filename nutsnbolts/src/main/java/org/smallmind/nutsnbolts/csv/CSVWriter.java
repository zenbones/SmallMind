package org.smallmind.nutsnbolts.csv;

import java.io.IOException;
import java.io.OutputStream;

public class CSVWriter {

   private static char[] ESCAPED_CHARS = {'"', ',', '\n', '\r', '\f'};

   private OutputStream outputStream;
   private int lineLength;

   public CSVWriter (OutputStream outputStream, String[] headers)
      throws IOException, CSVParseException {

      this.outputStream = outputStream;

      lineLength = headers.length;
      write(headers);
   }

   public CSVWriter (OutputStream outputStream, int lineLength) {

      this.outputStream = outputStream;
      this.lineLength = lineLength;
   }

   public void write (String... fields)
      throws IOException, CSVParseException {

      boolean init = false;

      if (fields.length != lineLength) {
         throw new CSVParseException("Line must contain the set number of fields(%d)", lineLength);
      }

      for (String field : fields) {
         if (init) {
            outputStream.write(',');
         }

         if (mustBeQuoted(field)) {
            outputStream.write('"');
            outputStream.write(doubleAllQuotes(field).getBytes());
            outputStream.write('"');
         }
         else {
            outputStream.write(doubleAllQuotes(field).getBytes());
         }

         init = true;
      }

      outputStream.write('\n');
   }

   private String doubleAllQuotes (String field) {

      if (field.indexOf("\"") >= 0) {
         return field.replaceAll("\"", "\"\"");
      }

      return field;
   }

   private boolean mustBeQuoted (String field) {

      for (char escapeChar : ESCAPED_CHARS) {
         if (field.indexOf(escapeChar) >= 0) {
            return true;
         }
      }

      return false;
   }

   public void close ()
      throws IOException {

      outputStream.close();
   }
}
