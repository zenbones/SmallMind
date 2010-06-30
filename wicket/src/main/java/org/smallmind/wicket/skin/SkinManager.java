package org.smallmind.wicket.skin;

import java.util.Map;
import java.util.Properties;
import org.apache.wicket.protocol.http.WebApplication;
import org.smallmind.wicket.property.PropertyException;
import org.smallmind.wicket.property.PropertyFactory;

public class SkinManager {

   private Map<Class, String> propertyMap;

   public SkinManager () {
   }

   public SkinManager (Map<Class, String> propertyMap) {

      this.propertyMap = propertyMap;
   }

   public void setPropertyMap (Map<Class, String> propertyMap) {

      this.propertyMap = propertyMap;
   }

   public synchronized Properties getProperties (WebApplication webApplication, Class componentClass) {

      String resourcePath;

      if ((resourcePath = propertyMap.get(componentClass)) == null) {
         throw new SkinManagementException("Unknown component class(%s)", componentClass.getName());
      }

      try {
         return PropertyFactory.getProperties(webApplication, resourcePath);
      }
      catch (PropertyException propertyException) {
         throw new SkinManagementException(propertyException);
      }
   }
}
