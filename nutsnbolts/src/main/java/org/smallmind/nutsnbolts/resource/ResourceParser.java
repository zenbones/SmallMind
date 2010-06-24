package org.smallmind.nutsnbolts.resource;

import java.util.HashMap;

public class ResourceParser {

   private final HashMap<ResourceSchemes, ResourceFactory> factoryMap;

   public ResourceParser () {

      factoryMap = new HashMap<ResourceSchemes, ResourceFactory>();
   }

   public void addResourceFactory (ResourceFactory factory) {

      synchronized (factoryMap) {
         factoryMap.put(factory.getValidSchemes(), factory);
      }
   }

   public Resource parseResource (String resourceIdentifier)
      throws ResourceException {

      String scheme;
      int colonPos;

      if ((colonPos = resourceIdentifier.indexOf(':')) < 0) {
         throw new ResourceException("Could not locate resource scheme in identifier(%s)", resourceIdentifier);
      }

      scheme = resourceIdentifier.substring(0, colonPos);
      synchronized (factoryMap) {
         for (ResourceSchemes resourceSchemes : factoryMap.keySet()) {
            if (resourceSchemes.containsScheme(scheme)) {
               return factoryMap.get(resourceSchemes).createResource(scheme, resourceIdentifier.substring(colonPos + 1));
            }
         }
      }

      throw new ResourceException("Could not locate a ResourceFactory for handling scheme(%s)", scheme);
   }
}
