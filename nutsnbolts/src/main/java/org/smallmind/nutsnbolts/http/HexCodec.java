package org.smallmind.nutsnbolts.http;

import java.text.StringCharacterIterator;

public class HexCodec {

   static final String validHex = "1234567890ABCDEFabcdef";

   public static String hexDecode (String value)
      throws NumberFormatException {

      StringCharacterIterator valueIter;
      StringBuilder modBuilder = new StringBuilder();
      String hexNum;
      int hexInt;

      valueIter = new StringCharacterIterator(value);
      while (valueIter.current() != StringCharacterIterator.DONE) {
         if (valueIter.current() != '%') {
            modBuilder.append(valueIter.current());
         }
         else {
            hexNum = "";
            valueIter.next();
            if (validHex.indexOf(valueIter.current()) >= 0) {
               hexNum += valueIter.current();
               valueIter.next();
               if (validHex.indexOf(valueIter.current()) >= 0) {
                  hexNum += valueIter.current();
                  hexInt = Integer.valueOf(hexNum, 16);
                  modBuilder.append((char)hexInt);
               }
               else {
                  modBuilder.append('%');
                  modBuilder.append(hexNum);
                  modBuilder.append(valueIter.current());
               }
            }
            else {
               modBuilder.append('%');
               modBuilder.append(valueIter.current());
            }
         }
         valueIter.next();
      }
      return modBuilder.toString();
   }

   public static String hexEncode (String value) {

      StringCharacterIterator valueIter;
      StringBuilder modBuilder = new StringBuilder();

      valueIter = new StringCharacterIterator(value);
      while (valueIter.current() != StringCharacterIterator.DONE) {
         if (Character.isLetterOrDigit(valueIter.current())) {
            modBuilder.append(valueIter.current());
         }
         else {
            modBuilder.append('%');
            modBuilder.append(Integer.toHexString((int)valueIter.current()));
         }
         valueIter.next();
      }
      return modBuilder.toString();
   }
}
