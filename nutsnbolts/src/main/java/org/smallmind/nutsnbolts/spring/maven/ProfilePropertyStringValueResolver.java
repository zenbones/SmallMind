package org.smallmind.nutsnbolts.spring.maven;

import java.util.HashMap;
import org.smallmind.nutsnbolts.maven.sax.SettingsSAXParser;
import org.smallmind.nutsnbolts.spring.RuntimeBeansException;
import org.smallmind.nutsnbolts.util.PropertyExpander;
import org.smallmind.nutsnbolts.util.PropertyExpanderException;
import org.springframework.beans.BeansException;
import org.springframework.util.StringValueResolver;

public class ProfilePropertyStringValueResolver implements StringValueResolver {

   private PropertyExpander propertyExpander;
   private HashMap<String, String> propertyMap;
   private boolean active = true;

   public ProfilePropertyStringValueResolver (String profile, boolean ignoreResourceNotFound, boolean ignoreUnresolvableProperties)
      throws BeansException {

      try {
         propertyMap = SettingsSAXParser.parse(profile);
      }
      catch (Exception exception) {
         if (!ignoreResourceNotFound) {
            throw new RuntimeBeansException("No profile(%s) found in the current user(%s) settings", profile, System.getProperty("user.name"));
         }
         else {
            active = false;
         }
      }

      try {
         if (active) {
            propertyExpander = new PropertyExpander(ignoreUnresolvableProperties);
         }
      }
      catch (PropertyExpanderException propertyExpanderException) {
         throw new RuntimeBeansException(propertyExpanderException);
      }
   }

   public boolean isActive () {

      return active;
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
