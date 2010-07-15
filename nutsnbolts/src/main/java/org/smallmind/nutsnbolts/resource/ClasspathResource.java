package org.smallmind.nutsnbolts.resource;

import java.io.InputStream;

public class ClasspathResource extends AbstractResource {

   public ClasspathResource (String path) {

      super(path);
   }

   public String getScheme () {

      return "classpath";
   }

   public InputStream getInputStream () {

      return ClassLoader.getSystemResourceAsStream(getPath());
   }
}
