/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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