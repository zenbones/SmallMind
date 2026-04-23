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

import java.io.IOException;
import java.util.LinkedList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.smallmind.nutsnbolts.xml.ExtensibleSAXParserException;
import org.smallmind.nutsnbolts.xml.XMLErrorHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * SAX {@link ContentHandler} that dispatches element events to pluggable {@link ElementExtender} instances supplied by a {@link DocumentExtender}.
 * Static {@code parse} helpers configure and execute a full SAX parse with optional schema validation.
 */
public class ExtensibleSAXParser implements ContentHandler {

  private final LinkedList<SAXExtender> extenderStack;
  private final LinkedList<StringBuilder> contentStack;
  private final DocumentExtender documentExtender;
  private boolean documentTerminated;

  /**
   * Creates a parser bound to the given document extender.
   *
   * @param documentExtender the document extender that drives document- and element-level event handling
   */
  private ExtensibleSAXParser (DocumentExtender documentExtender) {

    this.documentExtender = documentExtender;

    extenderStack = new LinkedList<>();
    contentStack = new LinkedList<>();

    documentTerminated = false;
  }

  /**
   * Parses the supplied XML input with schema validation enabled, delegating events to the given document extender.
   *
   * @param documentExtender the document extender that handles document- and element-level events
   * @param inputSource      the XML input to parse
   * @param entityResolver   the entity resolver used to locate external entities
   * @throws IOException                  if an I/O error occurs while reading the input
   * @throws SAXException                 if a SAX parsing error occurs
   * @throws ParserConfigurationException if the SAX parser cannot be configured
   */
  public static void parse (DocumentExtender documentExtender, InputSource inputSource, EntityResolver entityResolver)
    throws IOException, SAXException, ParserConfigurationException {

    internalParse(documentExtender, inputSource, entityResolver, true);
  }

  /**
   * Parses the supplied XML input with optional schema validation, delegating events to the given document extender.
   *
   * @param documentExtender the document extender that handles document- and element-level events
   * @param inputSource      the XML input to parse
   * @param entityResolver   the entity resolver used to locate external entities
   * @param validating       {@code true} to enable schema/DTD validation
   * @throws IOException                  if an I/O error occurs while reading the input
   * @throws SAXException                 if a SAX parsing error occurs
   * @throws ParserConfigurationException if the SAX parser cannot be configured
   */
  public static void parse (DocumentExtender documentExtender, InputSource inputSource, EntityResolver entityResolver, boolean validating)
    throws IOException, SAXException, ParserConfigurationException {

    internalParse(documentExtender, inputSource, entityResolver, validating);
  }

  /**
   * Builds, configures, and runs a SAX parser for the supplied input.
   *
   * @param documentExtender the document extender that handles document- and element-level events
   * @param inputSource      the XML input to parse
   * @param entityResolver   the entity resolver used to locate external entities
   * @param validating       {@code true} to enable schema/DTD validation
   * @throws IOException                  if an I/O error occurs while reading the input
   * @throws SAXException                 if a SAX parsing error occurs
   * @throws ParserConfigurationException if the SAX parser cannot be configured
   */
  private static void internalParse (DocumentExtender documentExtender, InputSource inputSource, EntityResolver entityResolver, boolean validating)
    throws IOException, SAXException, ParserConfigurationException {

    SAXParserFactory parserFactory;
    SAXParser parser;
    ExtensibleSAXParser extensibleSAXParser;

    parserFactory = SAXParserFactory.newInstance();
    parserFactory.setValidating(validating);
    parserFactory.setNamespaceAware(true);
    parser = parserFactory.newSAXParser();

    extensibleSAXParser = new ExtensibleSAXParser(documentExtender);

    parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
    parser.getXMLReader().setContentHandler(extensibleSAXParser);
    parser.getXMLReader().setEntityResolver(entityResolver);
    parser.getXMLReader().setErrorHandler(XMLErrorHandler.getInstance());
    parser.getXMLReader().parse(inputSource);

    if (!extensibleSAXParser.isDocumentTerminated()) {
      extensibleSAXParser.endDocument();
    }
  }

  /**
   * Returns whether the document end event has been received.
   *
   * @return {@code true} once {@link #endDocument()} has been called
   */
  public boolean isDocumentTerminated () {

    return documentTerminated;
  }

  /**
   * No-op; the document locator is not used by this implementation.
   *
   * @param locator the document locator provided by the parser (ignored)
   */
  public void setDocumentLocator (Locator locator) {

  }

  /**
   * Forwards the document-start event to the document extender and pushes it onto the extender stack.
   *
   * @throws SAXException if the document extender signals an error
   */
  public void startDocument ()
    throws SAXException {

    documentExtender.startDocument();
    extenderStack.add(documentExtender);
  }

  /**
   * Clears the extender stack, forwards the document-end event to the document extender, and marks the document as terminated.
   *
   * @throws SAXException if the document extender signals an error
   */
  public void endDocument ()
    throws SAXException {

    extenderStack.clear();
    documentExtender.endDocument();

    documentTerminated = true;
  }

  /**
   * Obtains an {@link ElementExtender} from the document extender, wires it into the stack, and forwards the element-start event.
   *
   * @param namespaceURI the namespace URI of the element
   * @param localName    the local name of the element
   * @param qName        the qualified name of the element
   * @param atts         the attributes of the element
   * @throws SAXException if no extender is available or an extender signals an error
   */
  public void startElement (String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException {

    ElementExtender elementExtender;

    try {
      if ((elementExtender = documentExtender.getElementExtender(extenderStack.getLast(), namespaceURI, localName, qName, atts)) == null) {
        throw new ExtensibleSAXParserException("No ElementExtender available to handle tag (%s)", qName);
      } else {
        elementExtender.setDocumentExtender(documentExtender);
        elementExtender.setParent(extenderStack.getLast());
        elementExtender.startElement(namespaceURI, localName, qName, atts);
        extenderStack.add(elementExtender);
        contentStack.add(new StringBuilder());
      }
    } catch (SAXException saxException) {
      throw saxException;
    } catch (Exception exception) {
      throw new SAXException(exception);
    }
  }

  /**
   * Pops the current extender and content buffer, forwards the element-end event to the extender, and notifies the parent extender.
   *
   * @param namespaceURI the namespace URI of the element
   * @param localName    the local name of the element
   * @param qName        the qualified name of the element
   * @throws SAXException if an extender signals an error
   */
  public void endElement (String namespaceURI, String localName, String qName)
    throws SAXException {

    ElementExtender elementExtender;
    StringBuilder contentBuilder;

    contentBuilder = contentStack.removeLast();
    elementExtender = (ElementExtender)extenderStack.removeLast();
    elementExtender.endElement(namespaceURI, localName, qName, contentBuilder);

    extenderStack.getLast().completedChildElement(elementExtender);
  }

  /**
   * Appends the supplied characters to the content buffer of the current element.
   *
   * @param ch     the characters reported by the parser
   * @param start  the start index within {@code ch}
   * @param length the number of characters to use from {@code ch}
   * @throws SAXException never thrown by this implementation
   */
  public void characters (char[] ch, int start, int length)
    throws SAXException {

    (contentStack.getLast()).append(ch, start, length);
  }

  /**
   * Appends ignorable whitespace to the content buffer of the current element.
   *
   * @param ch     the whitespace characters reported by the parser
   * @param start  the start index within {@code ch}
   * @param length the number of characters to use from {@code ch}
   * @throws SAXException never thrown by this implementation
   */
  public void ignorableWhitespace (char[] ch, int start, int length)
    throws SAXException {

    (contentStack.getLast()).append(ch, start, length);
  }

  /**
   * No-op; processing instructions are not handled by this implementation.
   *
   * @param target the processing instruction target (ignored)
   * @param data   the processing instruction data (ignored)
   * @throws SAXException never thrown by this implementation
   */
  public void processingInstruction (String target, String data)
    throws SAXException {

  }

  /**
   * No-op; skipped entities are not handled by this implementation.
   *
   * @param name the name of the skipped entity (ignored)
   * @throws SAXException never thrown by this implementation
   */
  public void skippedEntity (String name)
    throws SAXException {

  }

  /**
   * No-op; namespace prefix mappings are not tracked by this implementation.
   *
   * @param prefix the namespace prefix being mapped (ignored)
   * @param uri    the namespace URI associated with the prefix (ignored)
   * @throws SAXException never thrown by this implementation
   */
  public void startPrefixMapping (String prefix, String uri)
    throws SAXException {

  }

  /**
   * No-op; namespace prefix mapping ends are not tracked by this implementation.
   *
   * @param prefix the namespace prefix whose mapping has ended (ignored)
   * @throws SAXException never thrown by this implementation
   */
  public void endPrefixMapping (String prefix)
    throws SAXException {

  }
}
