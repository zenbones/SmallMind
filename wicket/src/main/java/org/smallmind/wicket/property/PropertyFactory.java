package org.smallmind.wicket.property;

import java.io.IOException;
import java.util.Properties;
import java.util.WeakHashMap;
import org.apache.wicket.protocol.http.WebApplication;

public class PropertyFactory {

   private static final WeakHashMap<String, Properties> PROPERTY_MAP = new WeakHashMap<String, Properties>();

   public static synchronized Properties getProperties (WebApplication webApplication, String resourcePath)
      throws PropertyException {

      Properties properties;

      if ((properties = PROPERTY_MAP.get(resourcePath)) == null) {
         properties = new Properties();

         try {
            properties.load(webApplication.getServletContext().getResourceAsStream(resourcePath));
         }
         catch (IOException ioException) {
            throw new PropertyException(ioException);
         }

         PROPERTY_MAP.put(resourcePath, properties);
      }

      return properties;
   }
}
