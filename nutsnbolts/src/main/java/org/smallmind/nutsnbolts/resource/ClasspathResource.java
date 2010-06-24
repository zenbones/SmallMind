package org.smallmind.nutsnbolts.resource;

import java.io.InputStream;

public class ClasspathResource implements Resource {

   private String path;
   private String id;

   public ClasspathResource (String path) {

      this.path = path;

      id = "classpath:" + path;
   }

   public String getId () {

      return id;
   }

   public InputStream getInputStream ()
      throws ResourceException {

      return ClassLoader.getSystemResourceAsStream(path);
   }
}
