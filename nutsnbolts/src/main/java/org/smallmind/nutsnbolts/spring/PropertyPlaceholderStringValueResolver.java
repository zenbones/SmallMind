package org.smallmind.nutsnbolts.spring;

import java.util.Map;
import org.smallmind.nutsnbolts.util.PropertyExpander;
import org.smallmind.nutsnbolts.util.PropertyExpanderException;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.springframework.beans.BeansException;
import org.springframework.util.StringValueResolver;

public class PropertyPlaceholderStringValueResolver implements StringValueResolver {

   private PropertyExpander propertyExpander;
   private Map<String, String> propertyMap;

   public PropertyPlaceholderStringValueResolver (Map<String, String> propertyMap, boolean ignoreUnresolvableProperties, SystemPropertyMode systemPropertyMode, boolean searchSystemEnvironment)
      throws BeansException {

      try {
         propertyExpander = new PropertyExpander(ignoreUnresolvableProperties, systemPropertyMode, searchSystemEnvironment);
      }
      catch (PropertyExpanderException propertyExpanderException) {
         throw new RuntimeBeansException(propertyExpanderException);
      }

      this.propertyMap = propertyMap;
   }

   public String resolveStringValue (String property)
      throws BeansException {

      try {

         return propertyExpander.expand(property, propertyMap);
      }
      catch (PropertyExpanderException propertyExpanderException) {
         throw new RuntimeBeansException(propertyExpanderException);
      }
   }
}
