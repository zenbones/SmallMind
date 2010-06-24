package org.smallmind.nutsnbolts.xml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public interface ElementExtender extends SAXExtender {

   public abstract void setDocumentExtender (DocumentExtender documentExtender);

   public abstract void setParent (SAXExtender parent);

   public abstract void startElement (String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException;

   public abstract void endElement (String namespaceURI, String localName, String qName, StringBuilder content)
      throws SAXException;

}
