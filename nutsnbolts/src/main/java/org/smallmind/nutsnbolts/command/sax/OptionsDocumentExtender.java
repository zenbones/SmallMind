/*
 * Copyright (c) 2007 through 2026 David Berkman
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

/**
 * Document-level SAX extender that drives parsing of an options XML descriptor and populates a {@link Template} with the declared options.
 */
public class OptionsDocumentExtender extends AbstractDocumentExtender {

  private final Template template;

  /**
   * Constructs an extender that will populate the supplied template during parsing.
   *
   * @param template template to populate from the parsed document
   */
  public OptionsDocumentExtender (Template template) {

    this.template = template;
  }

  /**
   * Returns the template being populated during parsing.
   *
   * @return {@link Template} that receives options as elements are processed
   */
  public Template getTemplate () {

    return template;
  }

  /**
   * Instantiates the appropriate element extender for the current XML element by mapping the element's qualified name to a class in this package.
   *
   * @param parent       parent SAX extender in the processing hierarchy
   * @param namespaceURI namespace URI of the starting element
   * @param localName    local name of the starting element
   * @param qName        qualified name used to derive the extender class name
   * @param atts         attributes of the starting element
   * @return newly constructed {@link ElementExtender} for the element
   * @throws Exception if the derived extender class cannot be found or instantiated
   */
  @Override
  public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts)
    throws Exception {

    return (ElementExtender)Class.forName(OptionsDocumentExtender.class.getPackage().getName() + "." + StringUtility.toCamelCase(qName, '-') + "ElementExtender").getConstructor().newInstance();
  }

  /**
   * Transfers the options collected by a completed {@link OptionsElementExtender} into the template.
   *
   * @param elementExtender completed child extender; only processed when it is an {@link OptionsElementExtender}
   * @throws SAXException wrapping a {@link CommandLineException} if the options fail template validation
   */
  @Override
  public void completedChildElement (ElementExtender elementExtender)
    throws SAXException {

    if (elementExtender instanceof OptionsElementExtender) {
      try {
        template.addOptions(((OptionsElementExtender)elementExtender).getOptionList(), false);
      } catch (CommandLineException commandLineException) {
        throw new SAXException(commandLineException);
      }
    }
  }
}
