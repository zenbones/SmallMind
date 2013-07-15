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

public class ExtensibleSAXParser implements ContentHandler {

  private LinkedList<SAXExtender> extenderStack;
  private LinkedList<StringBuilder> contentStack;
  private DocumentExtender documentExtender;
  private boolean documentTerminated;

  public static void parse (DocumentExtender documentExtender, InputSource inputSource, EntityResolver entityResolver)
    throws IOException, SAXException, ParserConfigurationException {

    internalParse(documentExtender, inputSource, entityResolver, true);
  }

  public static void parse (DocumentExtender documentExtender, InputSource inputSource, EntityResolver entityResolver, boolean validating)
    throws IOException, SAXException, ParserConfigurationException {

    internalParse(documentExtender, inputSource, entityResolver, validating);
  }

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

  private ExtensibleSAXParser (DocumentExtender documentExtender) {

    this.documentExtender = documentExtender;

    extenderStack = new LinkedList<SAXExtender>();
    contentStack = new LinkedList<StringBuilder>();

    documentTerminated = false;
  }

  public boolean isDocumentTerminated () {

    return documentTerminated;
  }

  public void setDocumentLocator (Locator locator) {

  }

  public void startDocument ()
    throws SAXException {

    documentExtender.startDocument();
    extenderStack.add(documentExtender);
  }

  public void endDocument ()
    throws SAXException {

    extenderStack.clear();
    documentExtender.endDocument();

    documentTerminated = true;
  }

  public void startElement (String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException {

    ElementExtender elementExtender;

    try {
      if ((elementExtender = documentExtender.getElementExtender(extenderStack.getLast(), namespaceURI, localName, qName, atts)) == null) {
        throw new ExtensibleSAXParserException("No ElementExtender available to handle tag (%s)", qName);
      }
      else {
        elementExtender.setDocumentExtender(documentExtender);
        elementExtender.setParent(extenderStack.getLast());
        elementExtender.startElement(namespaceURI, localName, qName, atts);
        extenderStack.add(elementExtender);
        contentStack.add(new StringBuilder());
      }
    }
    catch (SAXException saxException) {
      throw saxException;
    }
    catch (Exception exception) {
      throw new SAXException(exception);
    }
  }

  public void endElement (String namespaceURI, String localName, String qName)
    throws SAXException {

    ElementExtender elementExtender;
    StringBuilder contentBuilder;

    contentBuilder = contentStack.removeLast();
    elementExtender = (ElementExtender)extenderStack.removeLast();
    elementExtender.endElement(namespaceURI, localName, qName, contentBuilder);

    extenderStack.getLast().completedChildElement(elementExtender);
  }

  public void characters (char[] ch, int start, int length)
    throws SAXException {

    (contentStack.getLast()).append(ch, start, length);
  }

  public void ignorableWhitespace (char[] ch, int start, int length)
    throws SAXException {

    (contentStack.getLast()).append(ch, start, length);
  }

  public void processingInstruction (String target, String data)
    throws SAXException {

  }

  public void skippedEntity (String name)
    throws SAXException {

  }

  public void startPrefixMapping (String prefix, String uri)
    throws SAXException {

  }

  public void endPrefixMapping (String prefix)
    throws SAXException {

  }

}
