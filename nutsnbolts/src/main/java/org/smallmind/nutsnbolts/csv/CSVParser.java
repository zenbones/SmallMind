package org.smallmind.nutsnbolts.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class CSVParser {

   private CSVLineHandler lineHandler;
   private boolean skipHeader = false;
   private boolean trimFields = false;

   public synchronized CSVLineHandler getLineHandler () {

      return lineHandler;
   }

   public synchronized void setLineHandler (CSVLineHandler lineHandler) {

      this.lineHandler = lineHandler;
   }

   public synchronized boolean isSkipHeader () {

      return skipHeader;
   }

   public synchronized void setSkipHeader (boolean skipHeader) {

      this.skipHeader = skipHeader;
   }

   public synchronized boolean isTrimFields () {

      return trimFields;
   }

   public synchronized void setTrimFields (boolean trimFields) {

      this.trimFields = trimFields;
   }

   public synchronized void parse (InputStream inputStream)
      throws IOException, CSVParseException {

      parse(new InputStreamReader(inputStream));
   }

   public synchronized void parse (Reader reader)
      throws IOException, CSVParseException {

      CSVReader csvReader;
      String[] fields;

      csvReader = new CSVReader(reader, skipHeader);
      csvReader.setTrimFields(trimFields);

      lineHandler.startDocument();

      while ((fields = csvReader.readLine()) != null) {
         lineHandler.handleFields(fields);
      }

      lineHandler.endDocument();
   }

}
