/*
 * Copyright (c) 2007 through 2026 David Berkman
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
import liquibase.command.CommonArgumentNames;
import liquibase.command.core.DbDocCommandStep;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.UpdateSqlCommandStep;
import liquibase.command.core.helpers.DatabaseChangelogCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.LiquibaseDataType;
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

/**
 * Spring-friendly Liquibase runner that executes a configurable goal after properties are set.
 * Supports previewing SQL, applying change sets, generating documentation, or deriving change logs
 * while registering custom data types and resolving resources from the file system or classpath.
 */
public class SpringLiquibase implements InitializingBean {

  private final ClassLoader classloader;

  private DataSource dataSource;
  private ResourceAccessor resourceAccessor;
  private Goal goal;
  private OutputStream previewStream;
  private ChangeLog[] changeLogs;
  private LiquibaseDataType[] dataTypes;
  private String contexts;
  private String outputDir;

  /**
   * Constructs an instance using the current thread context class loader for resource resolution.
   */
  public SpringLiquibase () {

    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * @param classLoader class loader used when resolving classpath-based change logs
   */
  public SpringLiquibase (ClassLoader classLoader) {

    this.classloader = classLoader;
  }

  /**
   * @param dataSource JDBC data source that Liquibase will operate against
   */
  public void setDataSource (DataSource dataSource) {

    this.dataSource = dataSource;
  }

  /**
   * Selects where change log resources are read from.
   *
   * @param source indicates file system or classpath resolution
   * @throws FileNotFoundException if the chosen accessor cannot be created
   */
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

  /**
   * @param goal action Liquibase should perform once properties are initialized
   */
  public void setGoal (Goal goal) {

    this.goal = goal;
  }

  /**
   * @param previewStream destination for SQL preview output; defaults to {@link System#out} when null
   */
  public void setPreviewStream (OutputStream previewStream) {

    this.previewStream = previewStream;
  }

  /**
   * @param changeLogs ordered list of change logs to process
   */
  public void setChangeLogs (ChangeLog[] changeLogs) {

    this.changeLogs = changeLogs;
  }

  /**
   * @param dataTypes custom Liquibase data types to register before execution
   */
  public void setDataTypes (LiquibaseDataType[] dataTypes) {

    this.dataTypes = dataTypes;
  }

  /**
   * @param contexts comma-delimited Liquibase contexts used for filtering change sets
   */
  public void setContexts (String contexts) {

    this.contexts = contexts;
  }

  /**
   * @param outputDir directory for generated documentation or change logs; defaults to system temp directory when blank
   */
  public void setOutputDir (String outputDir) {

    this.outputDir = outputDir;
  }

  /**
   * Registers custom data types and executes the configured Liquibase goal against each change log.
   *
   * @throws Exception if Liquibase operations fail or when an unsupported goal is encountered
   */
  @Transactional
  public void afterPropertiesSet ()
    throws Exception {

    if (dataTypes != null) {
      for (LiquibaseDataType dataType : dataTypes) {
        DataTypeFactory.getInstance().unregister(dataType.getName());
        DataTypeFactory.getInstance().register(dataType);
      }
    }

    if (!goal.equals(Goal.NONE)) {

      HashSet<String> catalogSet = new HashSet<>();

      for (ChangeLog changeLog : changeLogs) {
        try (JdbcConnection connection = new JdbcConnection(dataSource.getConnection())) {

          Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);

          switch (goal) {
            case PREVIEW:
              Scope.child(Scope.Attr.resourceAccessor, resourceAccessor, () -> {

                CommandScope peview = new CommandScope(UpdateSqlCommandStep.COMMAND_NAME);

                peview.addArgumentValue(UpdateSqlCommandStep.CONTEXTS_ARG, contexts);
                peview.addArgumentValue(CommonArgumentNames.URL.getArgumentName(), "offline:" + database.getDisplayName());
                peview.addArgumentValue(UpdateSqlCommandStep.CHANGELOG_FILE_ARG, changeLog.getInput());

                peview.setOutput((previewStream == null) ? System.out : previewStream);

                peview.execute();
              });
              break;
            case UPDATE:
              Scope.child(Scope.Attr.resourceAccessor, resourceAccessor, () -> {

                CommandScope update = new CommandScope(UpdateCommandStep.COMMAND_NAME);

                update.addArgumentValue(UpdateCommandStep.CONTEXTS_ARG, contexts);
                update.addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database);
                update.addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, changeLog.getInput());

                update.execute();
              });
              break;
            case DOCUMENT:
              Scope.child(Scope.Attr.resourceAccessor, resourceAccessor, () -> {

                CommandScope document = new CommandScope(DbDocCommandStep.COMMAND_NAME);

                document.addArgumentValue(DatabaseChangelogCommandStep.CONTEXTS_ARG, contexts);
                document.addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database);
                document.addArgumentValue(CommonArgumentNames.CHANGELOG_FILE.getArgumentName(), changeLog.getInput());
                document.addArgumentValue(DbDocCommandStep.OUTPUT_DIRECTORY_ARG, ((outputDir == null) || outputDir.isEmpty()) ? System.getProperty("java.io.tmpdir") : outputDir);

                document.execute();
              });

              break;
            case GENERATE:

              String catalog;

              if (catalogSet.add(catalog = database.getDefaultCatalogName())) {

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
