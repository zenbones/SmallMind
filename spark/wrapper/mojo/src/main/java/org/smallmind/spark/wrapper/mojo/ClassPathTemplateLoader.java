package org.smallmind.spark.wrapper.mojo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import freemarker.cache.TemplateLoader;

public class ClassPathTemplateLoader implements TemplateLoader {

   Class<?> anchorClass;

   public ClassPathTemplateLoader (Class<?> anchorClass) {

      this.anchorClass = anchorClass;
   }

   public Object findTemplateSource (String name)
      throws IOException {

      return anchorClass.getClassLoader().getResourceAsStream(name);
   }

   public long getLastModified (Object templateSOurce) {

      return -1;
   }

   public Reader getReader (Object templateSource, String encoding)
      throws IOException {

      return new InputStreamReader((InputStream)templateSource, encoding);
   }

   public void closeTemplateSource (Object templateSource)
      throws IOException {

      ((InputStream)templateSource).close();
   }
}
