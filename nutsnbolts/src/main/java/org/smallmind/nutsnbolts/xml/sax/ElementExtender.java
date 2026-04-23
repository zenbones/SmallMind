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
 * Interface for objects that handle SAX events for a specific XML element within an {@link org.smallmind.nutsnbolts.xml.sax.ExtensibleSAXParser} parse.
 */
public interface ElementExtender extends SAXExtender {

  /**
   * Injects the {@link DocumentExtender} that owns the current parse.
   *
   * @param documentExtender the document extender managing this parse
   */
  void setDocumentExtender (DocumentExtender documentExtender);

  /**
   * Sets the parent extender on the element stack.
   *
   * @param parent the parent {@link SAXExtender} for the element being processed
   */
  void setParent (SAXExtender parent);

  /**
   * Called at the opening tag of the element.
   *
   * @param namespaceURI the element namespace URI
   * @param localName    the element local name
   * @param qName        the qualified element name
   * @param atts         the element attributes
   * @throws SAXException if processing fails
   */
  void startElement (String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException;

  /**
   * Called at the closing tag of the element with the complete accumulated character content.
   *
   * @param namespaceURI the element namespace URI
   * @param localName    the element local name
   * @param qName        the qualified element name
   * @param content      the aggregated character content of the element
   * @throws SAXException if processing fails
   */
  void endElement (String namespaceURI, String localName, String qName, StringBuilder content)
    throws SAXException;
}
