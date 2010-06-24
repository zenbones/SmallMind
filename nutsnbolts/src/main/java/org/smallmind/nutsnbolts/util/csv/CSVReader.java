package org.smallmind.nutsnbolts.util.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;

public class CSVReader {

   private static enum State {

      UNQOUTED, QUOTED
   }

   private BufferedReader reader;
   private StringBuilder fieldBuilder;
   private State state;
   private String[] headers;
   private boolean trimFields;

   public CSVReader (InputStream stream)
      throws IOException, CSVParseException {

      this(new InputStreamReader(stream), false);
   }

   public CSVReader (InputStream stream, boolean useHeaders)
      throws IOException, CSVParseException {

      this(new InputStreamReader(stream), useHeaders);
   }

   public CSVReader (Reader reader)
      throws IOException, CSVParseException {

      this(reader, false);
   }

   public CSVReader (Reader reader, boolean useHeaders)
      throws IOException, CSVParseException {

      this.reader = new BufferedReader(reader);

      state = State.UNQOUTED;
      trimFields = false;
      fieldBuilder = new StringBuilder();

      if (useHeaders) {
         headers = readLine(true);
      }
   }

   public synchronized void setTrimFields (boolean trimFields) {

      this.trimFields = trimFields;
   }

   public synchronized String[] getHeaders () {

      return headers;
   }

   public synchronized String getField (String header, String[] fields) {

      if (headers == null) {
         throw new IllegalStateException("No headers are available");
      }

      for (int count = 0; count < headers.length; count++) {
         if (headers[count].equals(header)) {
            return fields[count];
         }
      }

      return null;
   }

   public synchronized String[] readLine ()
      throws IOException, CSVParseException {

      return readLine(trimFields);
   }

   private synchronized String[] readLine (boolean trimCurrentLine)
      throws IOException, CSVParseException {

      LinkedList<String> fieldList;
      String[] fields;
      String singleLine;

      fieldList = new LinkedList<String>();
      while (true) {
         if ((singleLine = reader.readLine()) != null) {
            for (int count = 0; count < singleLine.length(); count++) {
               switch (state) {
                  case UNQOUTED:
                     switch (singleLine.charAt(count)) {
                        case ',':
                           appendField(fieldList, trimCurrentLine);
                           break;
                        case '\"':
                           if (fieldBuilder.length() > 0) {
                              throw new CSVParseException("The first character in a quoted field must be '\"'");
                           }
                           else {
                              state = State.QUOTED;
                           }
                           break;
                        default:
                           fieldBuilder.append(singleLine.charAt(count));
                     }
                     break;
                  case QUOTED:
                     switch (singleLine.charAt(count)) {
                        case '\"':
                           if ((count < (singleLine.length() - 1)) && (!((singleLine.charAt(count + 1) == '\"') || (singleLine.charAt(count + 1) == ',')))) {
                              throw new CSVParseException("The last character in a quoted field must be '\"'");
                           }
                           else if ((count == (singleLine.length() - 1)) || (singleLine.charAt(count + 1) == ',')) {
                              state = State.UNQOUTED;
                           }
                           else {
                              fieldBuilder.append(singleLine.charAt(count));
                              count++;
                           }
                           break;
                        default:
                           fieldBuilder.append(singleLine.charAt(count));
                     }
               }
            }

            if (state.equals(State.QUOTED)) {
               fieldBuilder.append(System.getProperty("line.separator"));
            }
            else {
               appendField(fieldList, trimCurrentLine);

               fields = new String[fieldList.size()];
               fieldList.toArray(fields);

               return fields;
            }
         }
         else {
            if (state.equals(State.QUOTED)) {
               throw new CSVParseException("Reached the end of the stream with an open quoted field");
            }
            else {
               return null;
            }
         }
      }
   }

   private void appendField (LinkedList<String> fieldList, boolean trimCurrentLine) {

      fieldList.add((trimCurrentLine) ? fieldBuilder.toString().trim() : fieldBuilder.toString());
      fieldBuilder.setLength(0);
   }

   public synchronized void close ()
      throws IOException {

      reader.close();
   }
}
