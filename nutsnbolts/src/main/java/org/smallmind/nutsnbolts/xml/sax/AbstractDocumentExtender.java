package org.smallmind.nutsnbolts.xml.sax;

import org.xml.sax.SAXException;

public abstract class AbstractDocumentExtender implements DocumentExtender {

   public void startDocument ()
      throws SAXException {
   }

   public void endDocument ()
      throws SAXException {
   }

   public void completedChildElement (ElementExtender elementExtender)
      throws SAXException {
   }
}
