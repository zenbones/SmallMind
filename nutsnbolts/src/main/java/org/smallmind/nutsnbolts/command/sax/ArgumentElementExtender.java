package org.smallmind.nutsnbolts.command.sax;

import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ArgumentElementExtender extends AbstractElementExtender {

   public String value;

   public String getValue () {

      return value;
   }

   public void startElement (String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException {

      value = atts.getValue("value");
   }
}
