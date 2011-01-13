/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.liquibase;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ResourceAccessor;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.orm.aop.Transactional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

public class SpringLiquibase implements InitializingBean, ResourceLoaderAware {

   private ResourceLoader resourceLoader;
   private DataSource dataSource;
   private Goal goal;
   private String changeLog;
   private String contexts;
   private String outputDir;

   public void setResourceLoader (ResourceLoader resourceLoader) {

      this.resourceLoader = resourceLoader;
   }

   public void setDataSource (DataSource dataSource) {

      this.dataSource = dataSource;
   }

   public void setGoal (Goal goal) {

      this.goal = goal;
   }

   public void setChangeLog (String changeLog) {

      this.changeLog = changeLog;
   }

   public void setContexts (String contexts) {

      this.contexts = contexts;
   }

   public void setOutputDir (String outputDir) {

      this.outputDir = outputDir;
   }

   @Transactional
   public void afterPropertiesSet ()
      throws SQLException, LiquibaseException {

      if (!goal.equals(Goal.NONE)) {

         Liquibase liquibase;

         liquibase = new Liquibase(changeLog, new ChangeLogResourceAccessor(), new JdbcConnection(dataSource.getConnection()));

         switch (goal) {
            case PREVIEW:
               liquibase.update(contexts, new PrintWriter(System.out));
               break;
            case UPDATE:
               liquibase.update(contexts);
               break;
            case DOCUMENT:
               liquibase.generateDocumentation(((outputDir == null) || (outputDir.length() == 0)) ? System.getProperty("java.io.tmpdir") : outputDir, contexts);
               break;
            default:
               throw new UnknownSwitchCaseException(goal.name());
         }
      }
   }

   private class ChangeLogResourceAccessor implements ResourceAccessor {

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
