/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.liquibase.spring;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import javax.sql.DataSource;
import liquibase.CatalogAndSchema;
import liquibase.Scope;
import liquibase.command.CommandScope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.orm.aop.Transactional;
import org.springframework.beans.factory.InitializingBean;

public class SpringLiquibase implements InitializingBean {

  private final ClassLoader classloader;

  private DataSource dataSource;
  private ResourceAccessor resourceAccessor;
  private Goal goal;
  private OutputStream previewStream;
  private ChangeLog[] changeLogs;
  private String contexts;
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

  public void setSource (Source source)
    throws FileNotFoundException {

    switch (source) {
      case FILE:
        resourceAccessor = new DirectoryResourceAccessor(Paths.get(System.getProperty("user.home")));
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

  public void setPreviewStream (OutputStream previewStream) {

    this.previewStream = previewStream;
  }

  public void setChangeLogs (ChangeLog[] changeLogs) {

    this.changeLogs = changeLogs;
  }

  public void setContexts (String contexts) {

    this.contexts = contexts;
  }

  public void setOutputDir (String outputDir) {

    this.outputDir = outputDir;
  }

  @Transactional
  public void afterPropertiesSet ()
    throws Exception {

    if (!goal.equals(Goal.NONE)) {

      HashSet<String> catalogSet = new HashSet<>();

      for (ChangeLog changeLog : changeLogs) {

        try (JdbcConnection connection = new JdbcConnection(dataSource.getConnection())) {

          Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);

          switch (goal) {
            case PREVIEW:
              Scope.child(Scope.Attr.resourceAccessor, resourceAccessor, () -> {

                CommandScope update = new CommandScope("update");

                update.addArgumentValue("contexts", contexts);
                update.addArgumentValue("database", database);
                update.addArgumentValue("changelogFile", changeLog.getInput());

                update.setOutput((previewStream == null) ? System.out : previewStream);

                update.execute();
              });
              break;
            case UPDATE:
              Scope.child(Scope.Attr.resourceAccessor, resourceAccessor, () -> {

                CommandScope update = new CommandScope("update");

                update.addArgumentValue("contexts", contexts);
                update.addArgumentValue("database", database);
                update.addArgumentValue("changelogFile", changeLog.getInput());

                update.execute();
              });
              break;
            case DOCUMENT:
              Scope.child(Scope.Attr.resourceAccessor, resourceAccessor, () -> {

                CommandScope update = new CommandScope("dbDoc");

                update.addArgumentValue("contexts", contexts);
                update.addArgumentValue("database", database);
                update.addArgumentValue("changelogFile", changeLog.getInput());
                update.addArgumentValue("outputDirectory", ((outputDir == null) || outputDir.isEmpty()) ? System.getProperty("java.io.tmpdir") : outputDir);

                update.execute();
              });

              break;
            case GENERATE:

              String catalog;

              if (catalogSet.add(catalog = (database).getDefaultCatalogName())) {

                SnapshotControl snapshotControl;
                CompareControl compareControl;
                DatabaseSnapshot originalDatabaseSnapshot;
                DiffResult diffResult;
                DiffToChangeLog changeLogWriter;

                snapshotControl = new SnapshotControl(database);
                compareControl = new CompareControl(new CompareControl.SchemaComparison[] {new CompareControl.SchemaComparison(new CatalogAndSchema(database.getDefaultCatalogName(), database.getDefaultSchemaName()), new CatalogAndSchema(database.getDefaultCatalogName(), database.getDefaultSchemaName()))}, Collections.emptySet());

                originalDatabaseSnapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(compareControl.getSchemas(CompareControl.DatabaseRole.REFERENCE), database, snapshotControl);
                diffResult = DiffGeneratorFactory.getInstance().compare(originalDatabaseSnapshot, SnapshotGeneratorFactory.getInstance().createSnapshot(compareControl.getSchemas(CompareControl.DatabaseRole.REFERENCE), null, snapshotControl), compareControl);

                DiffOutputControl diffOutputControl = new DiffOutputControl();
                diffOutputControl.setDataDir(null);

                changeLogWriter = new DiffToChangeLog(diffResult, diffOutputControl);

                changeLogWriter.setChangeSetAuthor("auto.generated");
                changeLogWriter.setChangeSetContext(contexts);

                changeLogWriter.print(new PrintStream(Files.newOutputStream(Paths.get(((outputDir == null) || outputDir.isEmpty()) ? System.getProperty("java.io.tmpdir") : outputDir, catalog + ".changelog"))));
              }
              break;
            default:
              throw new UnknownSwitchCaseException(goal.name());
          }
        }
      }
    }
  }
}
