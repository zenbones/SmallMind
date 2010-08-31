package org.smallmind.nutsnbolts.freemarker;

import java.io.IOException;
import java.io.InputStream;

public class ClassPathTemplateSource {

   private InputStream inputStream;
   private ClassLoader classLoader;
   private String name;

   public ClassPathTemplateSource (ClassLoader classLoader, String name) {

      this.classLoader = classLoader;
      this.name = name;

      inputStream = classLoader.getResourceAsStream(name);
   }

   public boolean exists () {

      return inputStream != null;
   }

   public ClassLoader getClassLoader () {

      return classLoader;
   }

   public String getName () {

      return name;
   }

   public synchronized InputStream getInputStream () {

      return inputStream;
   }

   public synchronized void close ()
      throws IOException {

      if (inputStream != null) {
         inputStream.close();
      }
   }

   @Override
   public int hashCode () {

      return classLoader.hashCode() ^ name.hashCode();
   }

   @Override
   public boolean equals (Object obj) {

      return (obj instanceof ClassPathTemplateSource) && ((ClassPathTemplateSource)obj).getClassLoader().equals(classLoader) && ((ClassPathTemplateSource)obj).getName().equals(name);
   }
}
