package org.smallmind.nutsnbolts.email;

import org.smallmind.nutsnbolts.http.HTMLUtilities;
import org.smallmind.nutsnbolts.util.Tuple;

public class MailUtilities {

   public static String swapValues (Tuple<String, String> tuple, String message, String startDelimiter, String stopDelimiter) {

      SwapName swapName;
      StringBuilder messageBuilder;
      String[] namePossibilityArray;
      String swapValue;
      boolean matched;
      int startPos;
      int stopPos;
      int index = 0;

      messageBuilder = new StringBuilder();
      while ((startPos = message.indexOf(startDelimiter, index)) >= 0) {
         if ((stopPos = message.indexOf(stopDelimiter, startPos + startDelimiter.length())) >= 0) {
            namePossibilityArray = message.substring(startPos + startDelimiter.length(), stopPos).split("\\|");
            matched = false;
            for (String namePossibility : namePossibilityArray) {
               swapName = new SwapName(namePossibility);
               if (tuple.isKey(swapName.getName())) {
                  messageBuilder.append(message.substring(index, startPos));
                  swapValue = swapValues(tuple, tuple.getValue(swapName.getName()), startDelimiter, stopDelimiter);
                  if (swapName.isAdjective('*')) {
                     messageBuilder.append(HTMLUtilities.convertLineBreaks(swapValue));
                  }
                  else {
                     messageBuilder.append(swapValue);
                  }
                  matched = true;
                  break;
               }
            }

            if (!matched) {
               messageBuilder.append(message.substring(index, stopPos + stopDelimiter.length()));
            }

            index = stopPos + stopDelimiter.length();
         }
         else {
            messageBuilder.append(message.substring(index, startPos + startDelimiter.length()));
            index = startDelimiter.length();
         }
      }
      messageBuilder.append(message.substring(index));

      return messageBuilder.toString();
   }

}