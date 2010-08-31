package org.smallmind.nutsnbolts.freemarker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import freemarker.cache.TemplateLoader;

public class ClassPathTemplateLoader implements TemplateLoader {

   ClassLoader classLoader;
   private Class<?> anchorClass;
   private boolean relative;

   public ClassPathTemplateLoader () {

      relative = false;
      classLoader = Thread.currentThread().getContextClassLoader();
   }

   public ClassPathTemplateLoader (Class<?> anchorClass) {

      this(anchorClass, true);
   }

   public ClassPathTemplateLoader (Class<?> anchorClass, boolean relative) {

      this.anchorClass = anchorClass;
      this.relative = relative;

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

      ClassPathTemplateSource source;

      if (!relative) {
         source = new ClassPathTemplateSource(classLoader, name);
      }
      else if (anchorClass != null) {

         StringBuilder pathBuilder = new StringBuilder(anchorClass.getPackage().getName().replace('.', '/'));

         pathBuilder.append('/').append(name);
         source = new ClassPathTemplateSource(classLoader, pathBuilder.toString());
      }
      else {
         source = new ClassPathTemplateSource(classLoader, name);
      }

      return (source.exists()) ? source : null;
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
