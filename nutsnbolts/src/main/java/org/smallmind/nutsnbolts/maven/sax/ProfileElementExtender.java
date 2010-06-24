package org.smallmind.nutsnbolts.maven.sax;

import java.util.HashMap;
import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;

public class ProfileElementExtender extends AbstractElementExtender {

   private HashMap<String, String> propertyMap;
   private String id;

   public String getId () {

      return id;
   }

   public void setId (String id) {

      this.id = id;
   }

   public HashMap<String, String> getPropertyMap () {

      return propertyMap;
   }

   public void setPropertyMap (HashMap<String, String> propertyMap) {

      this.propertyMap = propertyMap;
   }
}
