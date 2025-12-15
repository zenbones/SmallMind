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
 * Convenience base class for {@link ElementExtender} implementations providing storage for parent/document references.
 */
public abstract class AbstractElementExtender implements ElementExtender {

  private DocumentExtender documentExtender;
  private SAXExtender parent;

  /**
   * @return enclosing document extender
   */
  public DocumentExtender getDocumentExtender () {

    return documentExtender;
  }

  /**
   * Assigns the enclosing document extender.
   *
   * @param documentExtender document extender managing the parse
   */
  public void setDocumentExtender (DocumentExtender documentExtender) {

    this.documentExtender = documentExtender;
  }

  /**
   * @return parent extender in the element stack
   */
  public SAXExtender getParent () {

    return parent;
  }

  /**
   * Sets the parent extender.
   *
   * @param parent parent extender for the current element
   */
  public void setParent (SAXExtender parent) {

    this.parent = parent;
  }

  /**
   * {@inheritDoc}
   */
  public void startElement (String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException {

  }

  /**
   * {@inheritDoc}
   */
  public void endElement (String namespaceURI, String localName, String qName, StringBuilder contentBuilder)
    throws SAXException {

  }

  /**
   * {@inheritDoc}
   */
  public void completedChildElement (ElementExtender elementExtender)
    throws SAXException {

  }
}
