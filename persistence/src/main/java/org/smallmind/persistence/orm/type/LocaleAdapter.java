/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm.type;

import java.util.Locale;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocaleAdapter extends XmlAdapter<String, Locale> {

  @Override
  public Locale unmarshal (String value) throws Exception {

    if (value == null) {

      return null;
    }
    else if (value.toLowerCase().equals("default")) {

      return Locale.getDefault();
    }
    else {

      String[] localeParts = value.split("_", 3);

      switch (localeParts.length) {
        case 1:
          return new Locale(localeParts[0]);
        case 2:
          return new Locale(localeParts[0], localeParts[1]);
        case 3:
          return new Locale(localeParts[0], localeParts[1], localeParts[2]);
        default:
          throw new TypeFormatException("Not a valid locale(%s)", value);
      }
    }
  }

  @Override
  public String marshal (Locale locale) throws Exception {

    return locale.toString();
  }
}
