package org.smallmind.nutsnbolts.resource;

public enum ResourceType {

   FILE("file", FileResource.class), CLASSPATH("classpath", ClasspathResource.class);

   private String resourceScheme;
   private Class<? extends Resource> resourceClass;

   private ResourceType (String resourceScheme, Class<? extends Resource> resourceClass) {

      this.resourceScheme = resourceScheme;
      this.resourceClass = resourceClass;
   }

   public String getResourceScheme () {

      return resourceScheme;
   }

   public Class<? extends Resource> getResourceClass () {

      return resourceClass;
   }
}
