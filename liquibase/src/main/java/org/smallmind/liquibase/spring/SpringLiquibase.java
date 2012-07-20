/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.liquibase.spring;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.Diff;
import liquibase.diff.DiffResult;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.orm.aop.Transactional;
import org.springframework.beans.factory.InitializingBean;

public class SpringLiquibase implements InitializingBean {

  private final ClassLoader classloader;

  private DataSource dataSource;
  private ResourceAccessor resourceAccessor;
  private Goal goal;
  private Writer previewWriter;
  private String changeLog;
  private String contexts;
  private String outputLog;
  private String outputDir;

  public SpringLiquibase () {

    this(Thread.currentThread().getContextClassLoader());
  }

  public SpringLiquibase (ClassLoader classLoader) {

    this.classloader = classLoader;
  }

  public void setDataSource (DataSource dataSource) {

    this.dataSource = dataSource;
  }

  public void setSource (Source source) {

    switch (source) {
      case FILE:
        resourceAccessor = new FileSystemResourceAccessor();
        break;
      case CLASSPATH:
        resourceAccessor = new ClassLoaderResourceAccessor(classloader);
        break;
      default:
        throw new UnknownSwitchCaseException(source.name());
    }
  }

  public void setGoal (Goal goal) {

    this.goal = goal;
  }

  public void setPreviewWriter (Writer previewWriter) {

    this.previewWriter = previewWriter;
  }

  public void setChangeLog (String changeLog) {

    this.changeLog = changeLog;
  }

  public void setContexts (String contexts) {

    this.contexts = contexts;
  }

  public void setOutputLog (String outputLog) {

    this.outputLog = outputLog;
  }

  public void setOutputDir (String outputDir) {

    this.outputDir = outputDir;
  }

  @Transactional
  public void afterPropertiesSet ()
    throws IOException, ParserConfigurationException, SQLException, LiquibaseException {

    if (!goal.equals(Goal.NONE)) {

      Liquibase liquibase;
      Connection connection;

      connection = dataSource.getConnection();
      try {
        liquibase = new Liquibase(changeLog, resourceAccessor, new JdbcConnection(connection));

        switch (goal) {
          case PREVIEW:
            liquibase.update(contexts, (previewWriter == null) ? new PrintWriter(System.out) : previewWriter);
            break;
          case UPDATE:
            liquibase.update(contexts);
            break;
          case DOCUMENT:
            liquibase.generateDocumentation(((outputDir == null) || (outputDir.length() == 0)) ? System.getProperty("java.io.tmpdir") : outputDir, contexts);
            break;
          case GENERATE:

            Diff diff;
            DiffResult diffResult;
            Database database;

            diff = new Diff(database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection())), database.getDefaultSchemaName());
            diffResult = diff.compare();
            diffResult.setChangeSetAuthor("auto.generated");
            diffResult.setChangeSetContext(contexts);
            diffResult.setDataDir(outputDir);

            diffResult.printChangeLog(new PrintStream(new File((((outputDir == null) || (outputDir.length() == 0)) ? System.getProperty("java.io.tmpdir") : outputDir) + System.getProperty("file.separator") + outputLog)), database);
            break;
          default:
            throw new UnknownSwitchCaseException(goal.name());
        }
      }
      finally {
        connection.close();
      }
    }
  }
}
