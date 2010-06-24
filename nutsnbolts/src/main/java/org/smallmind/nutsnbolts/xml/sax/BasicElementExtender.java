package org.smallmind.nutsnbolts.xml.sax;

import org.xml.sax.Attributes;

public class BasicElementExtender extends AbstractElementExtender {

   private String localName;
   private StringBuilder contentBuilder;

   public String getLocalName () {

      return localName;
   }

   public String getContent () {

      return contentBuilder.toString();
   }

   @Override
   public void startElement (String namespaceURI, String localName, String qName, Attributes atts) {

      this.localName = localName;
   }

   @Override
   public void endElement (String namespaceURI, String localName, String qName, StringBuilder contentBuilder) {

      this.contentBuilder = contentBuilder;
   }
}
