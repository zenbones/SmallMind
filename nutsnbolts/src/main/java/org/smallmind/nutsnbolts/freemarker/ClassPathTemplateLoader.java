package org.smallmind.nutsnbolts.freemarker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import freemarker.cache.TemplateLoader;

public class ClassPathTemplateLoader implements TemplateLoader {

   ClassLoader classLoader;
   private Class<?> anchorClass;

   public ClassPathTemplateLoader () {

      classLoader = Thread.currentThread().getContextClassLoader();
   }

   public ClassPathTemplateLoader (Class<?> anchorClass) {

      this.anchorClass = anchorClass;

      classLoader = anchorClass.getClassLoader();
   }

   public Class<?> getAnchorClass () {

      return anchorClass;
   }

   public ClassLoader getClassLoader () {

      return classLoader;
   }

   public Object findTemplateSource (String name)
      throws IOException {

      if (name.startsWith("/")) {

         return new ClassPathTemplateSource(classLoader, name.substring(1));
      }
      else if (anchorClass != null) {

         StringBuilder pathBuilder = new StringBuilder(anchorClass.getPackage().getName().replace('.', '/'));

         pathBuilder.append('/').append(name);

         return new ClassPathTemplateSource(classLoader, pathBuilder.toString());
      }
      else {

         return new ClassPathTemplateSource(classLoader, name);
      }
   }

   public long getLastModified (Object templateSource) {

      return -1;
   }

   public Reader getReader (Object templateSource, String encoding)
      throws IOException {

      return new InputStreamReader(((ClassPathTemplateSource)templateSource).getInputStream(), encoding);
   }

   public void closeTemplateSource (Object templateSource)
      throws IOException {

      ((ClassPathTemplateSource)templateSource).close();
   }
}
