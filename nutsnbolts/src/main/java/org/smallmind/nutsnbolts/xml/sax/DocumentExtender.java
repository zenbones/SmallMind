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
package org.smallmind.nutsnbolts.xml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * SAX document-level event sink that also acts as a factory for {@link ElementExtender} instances used to handle individual elements.
 */
public interface DocumentExtender extends SAXExtender {

  /**
   * Called when the parser encounters the start of the document.
   *
   * @throws SAXException if an error occurs while processing the document start
   */
  void startDocument ()
    throws SAXException;

  /**
   * Called when the parser encounters the end of the document.
   *
   * @throws SAXException if an error occurs while processing the document end
   */
  void endDocument ()
    throws SAXException;

  /**
   * Returns an {@link ElementExtender} responsible for handling the element described by the supplied arguments.
   *
   * @param parent       the currently active parent extender on the element stack
   * @param namespaceURI the namespace URI of the element
   * @param localName    the local name of the element
   * @param qName        the qualified name of the element
   * @param atts         the attributes of the element
   * @return a non-null {@link ElementExtender} for the element
   * @throws Exception if the extender cannot be created or configured
   */
  ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts)
    throws Exception;
}
