/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.nutsnbolts.spring.maven;

import java.util.HashMap;
import org.smallmind.nutsnbolts.maven.sax.SettingsSAXParser;
import org.smallmind.nutsnbolts.spring.RuntimeBeansException;
import org.smallmind.nutsnbolts.util.PropertyExpander;
import org.smallmind.nutsnbolts.util.PropertyExpanderException;
import org.springframework.beans.BeansException;
import org.springframework.util.StringValueResolver;

public class ProfilePropertyStringValueResolver implements StringValueResolver {

  private PropertyExpander propertyExpander;
  private HashMap<String, String> propertyMap;
  private boolean active = true;

  public ProfilePropertyStringValueResolver (String profile, boolean ignoreResourceNotFound, boolean ignoreUnresolvableProperties)
    throws BeansException {

    try {
      propertyMap = SettingsSAXParser.parse(profile);
    }
    catch (Exception exception) {
      if (!ignoreResourceNotFound) {
        throw new RuntimeBeansException("No profile(%s) found in the current user(%s) settings", profile, System.getProperty("user.name"));
      }
      else {
        active = false;
      }
    }

    try {
      if (active) {
        propertyExpander = new PropertyExpander(ignoreUnresolvableProperties);
      }
    }
    catch (PropertyExpanderException propertyExpanderException) {
      throw new RuntimeBeansException(propertyExpanderException);
    }
  }

  public boolean isActive () {

    return active;
  }

  public String resolveStringValue (String property)
    throws BeansException {

    try {
      return propertyExpander.expand(property, propertyMap);
    }
    catch (PropertyExpanderException propertyExpanderException) {
      throw new RuntimeBeansException(propertyExpanderException);
    }
  }
}
