package org.smallmind.nutsnbolts.xml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public interface DocumentExtender extends SAXExtender {

   public abstract void startDocument ()
      throws SAXException;

   public abstract void endDocument ()
      throws SAXException;

   public abstract ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts)
      throws Exception;
}
