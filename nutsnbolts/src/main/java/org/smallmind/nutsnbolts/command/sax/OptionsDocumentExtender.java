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
package org.smallmind.nutsnbolts.command.sax;

import org.smallmind.nutsnbolts.command.CommandLineException;
import org.smallmind.nutsnbolts.command.template.Template;
import org.smallmind.nutsnbolts.util.StringUtility;
import org.smallmind.nutsnbolts.xml.sax.AbstractDocumentExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;
import org.smallmind.nutsnbolts.xml.sax.SAXExtender;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class OptionsDocumentExtender extends AbstractDocumentExtender {

  private Template template;

  public OptionsDocumentExtender (Template template) {

    this.template = template;
  }

  public Template getTemplate () {

    return template;
  }

  @Override
  public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts)
    throws Exception {

    return (ElementExtender)Class.forName(OptionsDocumentExtender.class.getPackage().getName() + "." + StringUtility.toCamelCase(qName, '-') + "ElementExtender").newInstance();
  }

  @Override
  public void completedChildElement (ElementExtender elementExtender)
    throws SAXException {

    if (elementExtender instanceof OptionsElementExtender) {
      try {
        template.addOptions(((OptionsElementExtender)elementExtender).getOptionList());
      } catch (CommandLineException commandLineException) {
        throw new SAXException(commandLineException);
      }
    }
  }
}
