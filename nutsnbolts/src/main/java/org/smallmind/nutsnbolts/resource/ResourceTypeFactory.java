package org.smallmind.nutsnbolts.resource;

import java.lang.reflect.Constructor;
import java.util.LinkedList;

public class ResourceTypeFactory implements ResourceFactory {

   private static final Class[] SIGNATURE = new Class[] {String.class};

   private static ResourceSchemes VALID_SCHEMES;

   static {

      String[] schemes;
      LinkedList<String> schemeList;

      schemeList = new LinkedList<String>();
      for (ResourceType resourceType : ResourceType.values()) {
         schemeList.add(resourceType.getResourceScheme());
      }

      schemes = new String[schemeList.size()];
      schemeList.toArray(schemes);
      VALID_SCHEMES = new ResourceSchemes(schemes);
   }

   public ResourceSchemes getValidSchemes () {

      return VALID_SCHEMES;
   }

   public Resource createResource (String scheme, String path)
      throws ResourceException {

      Constructor<? extends Resource> resourceConstructor;

      for (ResourceType resourceType : ResourceType.values()) {
         if (resourceType.getResourceScheme().equals(scheme)) {
            try {
               resourceConstructor = resourceType.getResourceClass().getConstructor(SIGNATURE);
               return resourceConstructor.newInstance(path);
            }
            catch (Exception exception) {
               throw new ResourceException(exception);
            }
         }
      }

      throw new ResourceException("This factory does not handle the references scheme(%s)", scheme);
   }
}
