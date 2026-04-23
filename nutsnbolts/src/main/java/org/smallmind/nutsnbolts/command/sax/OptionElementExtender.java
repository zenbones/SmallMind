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

import java.util.LinkedList;
import org.smallmind.nutsnbolts.command.template.Option;
import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * SAX element extender for the {@code <option>} element that constructs an {@link Option} from XML attributes and wires up child options and argument definitions.
 */
public class OptionElementExtender extends AbstractElementExtender {

  private Option option;

  /**
   * Returns the {@link Option} constructed from the parsed element.
   *
   * @return assembled option, or {@code null} before the element has started
   */
  public Option getOption () {

    return option;
  }

  /**
   * Creates the {@link Option} from the element's {@code name}, {@code flag}, and {@code required} attributes.
   *
   * @param namespaceURI namespace URI of the starting element
   * @param localName    local name of the starting element
   * @param qName        qualified name of the starting element
   * @param atts         element attributes containing {@code name}, {@code flag}, and {@code required}
   */
  @Override
  public void startElement (String namespaceURI, String localName, String qName, Attributes atts) {

    String flag;

    option = new Option(atts.getValue("name"), (((flag = atts.getValue("flag")) == null) || flag.isEmpty()) ? null : flag.charAt(0), Boolean.parseBoolean(atts.getValue("required")));
  }

  /**
   * Wires child option lists and argument definitions into the constructed option, establishing parent-child relationships.
   *
   * @param elementExtender completed child extender; handled when it is an {@link OptionsElementExtender} or {@link ArgumentsElementExtender}
   * @throws SAXException if an error occurs while linking child options to their parent
   */
  @Override
  public void completedChildElement (ElementExtender elementExtender)
    throws SAXException {

    if (elementExtender instanceof OptionsElementExtender) {

      LinkedList<Option> optionList;

      for (Option childOption : optionList = ((OptionsElementExtender)elementExtender).getOptionList()) {
        childOption.setParent(option);
      }

      option.setChildren(optionList);
    }
    if (elementExtender instanceof ArgumentsElementExtender) {
      option.setArgument(((ArgumentsElementExtender)elementExtender).getArgument());
    }
  }
}
