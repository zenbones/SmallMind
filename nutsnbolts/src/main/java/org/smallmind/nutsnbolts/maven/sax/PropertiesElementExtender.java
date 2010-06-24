package org.smallmind.nutsnbolts.maven.sax;

import java.util.HashMap;
import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;
import org.smallmind.nutsnbolts.xml.sax.BasicElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;

public class PropertiesElementExtender extends AbstractElementExtender {

   private HashMap<String, String> propertyMap;

   public PropertiesElementExtender () {

      propertyMap = new HashMap<String, String>();
   }

   @Override
   public void endElement (String namespaceURI, String localName, String qName, StringBuilder contentBuilder) {

      ((ProfileElementExtender)getParent()).setPropertyMap(propertyMap);
   }

   @Override
   public void completedChildElement (ElementExtender elementExtender) {

      propertyMap.put(((BasicElementExtender)elementExtender).getLocalName(), ((BasicElementExtender)elementExtender).getContent());
   }
}
