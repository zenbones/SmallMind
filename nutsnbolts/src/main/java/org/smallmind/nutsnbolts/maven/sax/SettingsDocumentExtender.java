package org.smallmind.nutsnbolts.maven.sax;

import java.util.HashMap;
import org.smallmind.nutsnbolts.xml.sax.AbstractDocumentExtender;
import org.smallmind.nutsnbolts.xml.sax.BasicElementExtender;
import org.smallmind.nutsnbolts.xml.sax.DoNothingElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;
import org.smallmind.nutsnbolts.xml.sax.SAXExtender;
import org.xml.sax.Attributes;

public class SettingsDocumentExtender extends AbstractDocumentExtender {

   private String profile;
   private HashMap<String, String> propertyMap;

   public SettingsDocumentExtender (String profile) {

      this.profile = profile;
   }

   public HashMap<String, String> getPropertyMap () {

      return propertyMap;
   }

   public void setPropertyMap (HashMap<String, String> propertyMap) {

      this.propertyMap = propertyMap;
   }

   public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts) {

      if (localName.equals("settings")) {
         return new SettingsElementExtender(profile);
      }
      else if (localName.equals("profiles")) {
         return new ProfilesElementExtender();
      }
      else if (localName.equals("profile")) {
         return new ProfileElementExtender();
      }
      else if ((parent instanceof ProfileElementExtender) && localName.equals("id")) {
         return new IdElementExtender();
      }
      else if ((parent instanceof ProfileElementExtender) && localName.equals("properties")) {
         return new PropertiesElementExtender();
      }
      else if (parent instanceof PropertiesElementExtender) {
         return new BasicElementExtender();
      }
      else {
         return new DoNothingElementExtender();
      }
   }
}
