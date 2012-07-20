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
package org.smallmind.wicket.property;

import java.io.IOException;
import java.util.Properties;
import java.util.WeakHashMap;
import org.apache.wicket.protocol.http.WebApplication;

public class PropertyFactory {

  private static final WeakHashMap<String, Properties> PROPERTY_MAP = new WeakHashMap<String, Properties>();

  public static synchronized Properties getProperties (WebApplication webApplication, String resourcePath)
    throws PropertyException {

    Properties properties;

    if ((properties = PROPERTY_MAP.get(resourcePath)) == null) {
      properties = new Properties();

      try {
        properties.load(webApplication.getServletContext().getResourceAsStream(resourcePath));
      }
      catch (IOException ioException) {
        throw new PropertyException(ioException);
      }

      PROPERTY_MAP.put(resourcePath, properties);
    }

    return properties;
  }
}
