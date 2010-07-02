package org.smallmind.persistence.liquibase;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import javax.sql.DataSource;
import liquibase.FileOpener;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import org.smallmind.persistence.orm.aop.Transactional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

public class SpringLiquibase implements InitializingBean, ResourceLoaderAware {

   private ResourceLoader resourceLoader;

   private DataSource dataSource;

   private String changeLog;

   private String contexts;

   private boolean preview = true;

   private boolean execute = false;

   public void setResourceLoader (ResourceLoader resourceLoader) {
      this.resourceLoader = resourceLoader;
   }

   public void setDataSource (DataSource dataSource) {
      this.dataSource = dataSource;
   }

   public void setChangeLog (String changeLog) {
      this.changeLog = changeLog;
   }

   public void setContexts (String contexts) {
      this.contexts = contexts;
   }

   public void setPreview (boolean preview) {
      this.preview = preview;
   }


   public void setExecute (boolean execute) {
      this.execute = execute;
   }

   @Transactional
   public void afterPropertiesSet ()
      throws SQLException, LiquibaseException {
      if (execute) {
         Liquibase liquibase;
         liquibase = new Liquibase(changeLog, new ChangeLogFileOpener(), dataSource.getConnection());
         if (preview) {
            liquibase.update(contexts, new PrintWriter(System.out));
         }
         else {
            liquibase.update(contexts);
         }
      }
   }

   private class ChangeLogFileOpener implements FileOpener {

      public InputStream getResourceAsStream (String resource)
         throws IOException {
         return resourceLoader.getResource(resource).getInputStream();
      }

      public Enumeration<URL> getResources (String resource)
         throws IOException {
         return new ChangeLogEnumeration(resourceLoader.getResource(resource).getURL());
      }

      public ClassLoader toClassLoader () {
         return resourceLoader.getClassLoader();
      }
   }

   private class ChangeLogEnumeration implements Enumeration<URL> {

      private URL url;

      private boolean taken = false;

      public ChangeLogEnumeration (URL url) {
         this.url = url;
      }

      public synchronized boolean hasMoreElements () {
         return taken;
      }

      public synchronized URL nextElement () {
         if (taken) {
            throw new NoSuchElementException();
         }
         taken = true;
         return url;
      }
   }
}
