/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.wicket.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StyleAttribute {

  private HashMap<String, String> styleMap = new HashMap<String, String>();

  public StyleAttribute () {

  }

  public StyleAttribute (String style) {

    int colonPos;

    if (style != null) {
      for (String styleSegment : style.split(";", -1)) {
        if ((colonPos = styleSegment.indexOf(':')) < 0) {
          throw new IllegalArgumentException(style);
        }

        styleMap.put(styleSegment.substring(0, colonPos).trim(), styleSegment.substring(colonPos + 1).trim());
      }
    }
  }

  public synchronized String[] getKeys () {

    String[] keys;
    Set<String> keySet = styleMap.keySet();

    keys = new String[keySet.size()];
    keySet.toArray(keys);

    return keys;
  }

  public synchronized String getAttribute (String styleKey) {

    return styleMap.get(styleKey);
  }

  public synchronized void putAttribute (String styleKey, String styleValue) {

    styleMap.put(styleKey, styleValue);
  }

  public synchronized String removeAttribute (String styleKey) {

    return styleMap.remove(styleKey);
  }

  public synchronized boolean isEmpty () {

    return styleMap.isEmpty();
  }

  public synchronized String getStyle () {

    StringBuilder styleBuilder = new StringBuilder();

    for (Map.Entry<String, String> entry : styleMap.entrySet()) {
      if (styleBuilder.length() > 0) {
        styleBuilder.append(';');
      }
      styleBuilder.append(entry.getKey()).append(": ").append(entry.getValue());
    }

    return styleBuilder.toString();
  }
}