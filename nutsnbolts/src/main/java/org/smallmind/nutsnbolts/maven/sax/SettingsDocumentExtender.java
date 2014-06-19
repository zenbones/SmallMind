/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.nutsnbolts.maven.sax;

import java.util.HashMap;
import org.smallmind.nutsnbolts.xml.sax.AbstractDocumentExtender;
import org.smallmind.nutsnbolts.xml.sax.BasicElementExtender;
import org.smallmind.nutsnbolts.xml.sax.DoNothingElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;
import org.smallmind.nutsnbolts.xml.sax.SAXExtender;
import org.xml.sax.Attributes;

public class SettingsDocumentExtender extends AbstractDocumentExtender {

  private String profile;
  private HashMap<String, Object> propertyMap;

  public SettingsDocumentExtender (String profile) {

    this.profile = profile;
  }

  public HashMap<String, Object> getPropertyMap () {

    return propertyMap;
  }

  public void setPropertyMap (HashMap<String, Object> propertyMap) {

    this.propertyMap = propertyMap;
  }

  public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts) {

    if (localName.equals("settings")) {
      return new SettingsElementExtender(profile);
    }
    else if (localName.equals("profiles")) {
      return new ProfilesElementExtender();
    }
    else if (localName.equals("profile")) {
      return new ProfileElementExtender();
    }
    else if ((parent instanceof ProfileElementExtender) && localName.equals("id")) {
      return new IdElementExtender();
    }
    else if ((parent instanceof ProfileElementExtender) && localName.equals("properties")) {
      return new PropertiesElementExtender();
    }
    else if (parent instanceof PropertiesElementExtender) {
      return new BasicElementExtender();
    }
    else {
      return new DoNothingElementExtender();
    }
  }
}
