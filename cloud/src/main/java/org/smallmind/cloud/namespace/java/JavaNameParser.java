package org.smallmind.cloud.namespace.java;

import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import org.smallmind.cloud.namespace.java.backingStore.NameTranslator;

public class JavaNameParser implements NameParser {

   private NameTranslator nameTranslator;

   public JavaNameParser (NameTranslator nameTranslator) {

      this.nameTranslator = nameTranslator;
   }

   public Name parse (String name)
      throws NamingException {

      Name parsedName;
      String[] parseArray;
      int colonPos;
      int count;

      parsedName = new JavaName(nameTranslator);

      if (name.equals("")) {
         return parsedName;
      }

      parseArray = name.split("/", -1);
      for (count = 0; count < parseArray.length; count++) {
         if (count == 0) {
            if ((colonPos = parseArray[count].indexOf(":")) >= 0) {
               parsedName.add(parseArray[count].substring(0, colonPos + 1));
               if ((colonPos + 1) < parseArray[count].length()) {
                  parsedName.add(parseArray[count].substring(colonPos + 1));
               }
            }
            else {
               parsedName.add(parseArray[count]);
            }
         }
         else {
            parsedName.add(parseArray[count]);
         }
      }
      return parsedName;
   }

   public String unparse (Name name) {

      StringBuilder nameBuilder;
      int count;

      if (name.size() == 0) {
         return "";
      }
      nameBuilder = new StringBuilder();
      for (count = 0; count < name.size(); count++) {
         if (count > 0) {
            nameBuilder.append('/');
         }
         nameBuilder.append(name.get(count));
      }
      return nameBuilder.toString();
   }

}
