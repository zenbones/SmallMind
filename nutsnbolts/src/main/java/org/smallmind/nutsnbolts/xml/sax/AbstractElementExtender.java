package org.smallmind.nutsnbolts.xml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public abstract class AbstractElementExtender implements ElementExtender {

   private DocumentExtender documentExtender;
   private SAXExtender parent;

   public void setDocumentExtender (DocumentExtender documentExtender) {

      this.documentExtender = documentExtender;
   }

   public void setParent (SAXExtender parent) {

      this.parent = parent;
   }

   public DocumentExtender getDocumentExtender () {

      return documentExtender;
   }

   public SAXExtender getParent () {

      return parent;
   }

   public void startElement (String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException {
   }

   public void endElement (String namespaceURI, String localName, String qName, StringBuilder contentBuilder)
      throws SAXException {
   }

   public void completedChildElement (ElementExtender elementExtender)
      throws SAXException {
   }
}
