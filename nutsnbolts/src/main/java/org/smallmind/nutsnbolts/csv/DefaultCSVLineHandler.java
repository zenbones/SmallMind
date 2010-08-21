package org.smallmind.nutsnbolts.csv;

import java.util.Arrays;

public class DefaultCSVLineHandler implements CSVLineHandler {

   public void startDocument () {
   }

   public void endDocument () {
   }

   public void handleFields (String[] fields) {

      Arrays.toString(fields);
   }
}
