/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.namespace.java;

import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import org.smallmind.quorum.namespace.java.backingStore.NameTranslator;

public class JavaNameParser implements NameParser {

  private final NameTranslator nameTranslator;

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
        } else {
          parsedName.add(parseArray[count]);
        }
      } else {
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
