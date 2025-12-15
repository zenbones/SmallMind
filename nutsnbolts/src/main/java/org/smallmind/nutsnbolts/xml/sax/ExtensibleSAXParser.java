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
 * Pluggable SAX {@link ContentHandler} that delegates element processing to {@link ElementExtender} instances produced by a {@link DocumentExtender}.
 * Provides static helper methods to parse an {@link InputSource} with optional validation.
 */
public class ExtensibleSAXParser implements ContentHandler {

  private final LinkedList<SAXExtender> extenderStack;
  private final LinkedList<StringBuilder> contentStack;
  private final DocumentExtender documentExtender;
  private boolean documentTerminated;

  /**
   * Creates a parser bound to the supplied document extender.
   *
   * @param documentExtender factory/handler for document-level events
   */
  private ExtensibleSAXParser (DocumentExtender documentExtender) {

    this.documentExtender = documentExtender;

    extenderStack = new LinkedList<>();
    contentStack = new LinkedList<>();

    documentTerminated = false;
  }

  /**
   * Parses the input source using validation and the provided resolvers.
   *
   * @param documentExtender document extender to drive parsing
   * @param inputSource      XML input
   * @param entityResolver   resolver for external entities
   * @throws IOException                  on I/O failure
   * @throws SAXException                 on SAX parsing errors
   * @throws ParserConfigurationException if the parser cannot be created
   */
  public static void parse (DocumentExtender documentExtender, InputSource inputSource, EntityResolver entityResolver)
    throws IOException, SAXException, ParserConfigurationException {

    internalParse(documentExtender, inputSource, entityResolver, true);
  }

  /**
   * Parses the input source with optional validation.
   *
   * @param documentExtender document extender to drive parsing
   * @param inputSource      XML input
   * @param entityResolver   resolver for external entities
   * @param validating       whether to perform schema/DTD validation
   * @throws IOException                  on I/O failure
   * @throws SAXException                 on SAX parsing errors
   * @throws ParserConfigurationException if the parser cannot be created
   */
  public static void parse (DocumentExtender documentExtender, InputSource inputSource, EntityResolver entityResolver, boolean validating)
    throws IOException, SAXException, ParserConfigurationException {

    internalParse(documentExtender, inputSource, entityResolver, validating);
  }

  /**
   * Internal helper that wires the content handler and kicks off parsing.
   *
   * @param documentExtender document extender to drive parsing
   * @param inputSource      XML input
   * @param entityResolver   resolver for external entities
   * @param validating       whether validation is enabled
   * @throws IOException                  on I/O failure
   * @throws SAXException                 on SAX parsing errors
   * @throws ParserConfigurationException if the parser cannot be created
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
   * @return {@code true} once {@link #endDocument()} has been invoked
   */
  public boolean isDocumentTerminated () {

    return documentTerminated;
  }

  /**
   * {@inheritDoc}
   */
  public void setDocumentLocator (Locator locator) {

  }

  /**
   * {@inheritDoc}
   */
  public void startDocument ()
    throws SAXException {

    documentExtender.startDocument();
    extenderStack.add(documentExtender);
  }

  /**
   * {@inheritDoc}
   */
  public void endDocument ()
    throws SAXException {

    extenderStack.clear();
    documentExtender.endDocument();

    documentTerminated = true;
  }

  /**
   * {@inheritDoc}
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
   * {@inheritDoc}
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
   * {@inheritDoc}
   */
  public void characters (char[] ch, int start, int length)
    throws SAXException {

    (contentStack.getLast()).append(ch, start, length);
  }

  /**
   * {@inheritDoc}
   */
  public void ignorableWhitespace (char[] ch, int start, int length)
    throws SAXException {

    (contentStack.getLast()).append(ch, start, length);
  }

  /**
   * {@inheritDoc}
   */
  public void processingInstruction (String target, String data)
    throws SAXException {

  }

  /**
   * {@inheritDoc}
   */
  public void skippedEntity (String name)
    throws SAXException {

  }

  /**
   * {@inheritDoc}
   */
  public void startPrefixMapping (String prefix, String uri)
    throws SAXException {

  }

  /**
   * {@inheritDoc}
   */
  public void endPrefixMapping (String prefix)
    throws SAXException {

  }
}
