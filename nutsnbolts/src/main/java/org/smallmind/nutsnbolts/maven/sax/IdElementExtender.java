package org.smallmind.nutsnbolts.maven.sax;

import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;

public class IdElementExtender extends AbstractElementExtender {

   @Override
   public void endElement (String namespaceURI, String localName, String qName, StringBuilder contentBuilder) {

      ((ProfileElementExtender)getParent()).setId(contentBuilder.toString());
   }
}
