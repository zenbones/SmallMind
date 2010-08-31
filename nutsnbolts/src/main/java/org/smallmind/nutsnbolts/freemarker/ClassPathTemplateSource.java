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
   }

   public ClassLoader getClassLoader () {

      return classLoader;
   }

   public String getName () {

      return name;
   }

   public synchronized InputStream getInputStream () {

      if (inputStream != null) {
         throw new IllegalStateException("Stream has been previously requested, but not closed");
      }

      return (inputStream = classLoader.getResourceAsStream(name));
   }

   public synchronized void close ()
      throws IOException {

      if (inputStream == null) {
         throw new IllegalStateException("Stream has not been requested");
      }

      inputStream.close();
      inputStream = null;
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
