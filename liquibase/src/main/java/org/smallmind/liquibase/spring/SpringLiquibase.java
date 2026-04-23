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
 * Spring {@link InitializingBean} that executes a configurable Liquibase operation during application
 * context refresh.
 *
 * <p>Configure one instance per logical database in your Spring context. After all properties are
 * injected, {@link #afterPropertiesSet()} performs two steps:</p>
 * <ol>
 *   <li>Register any custom {@link LiquibaseDataType} instances with Liquibase's
 *       {@link DataTypeFactory}, replacing any existing registration for the same name.</li>
 *   <li>Execute the configured {@link Goal} against each {@link ChangeLog} in order.</li>
 * </ol>
 *
 * <p>Supported goals:</p>
 * <ul>
 *   <li>{@link Goal#NONE} — register data types only; no database interaction.</li>
 *   <li>{@link Goal#PREVIEW} — write pending SQL to the configured output stream without
 *       modifying the database.</li>
 *   <li>{@link Goal#UPDATE} — apply all pending change sets to the database.</li>
 *   <li>{@link Goal#DOCUMENT} — write HTML schema documentation to the output directory.</li>
 *   <li>{@link Goal#GENERATE} — snapshot the live schema and write a Liquibase change log file
 *       to the output directory; one file per distinct catalog.</li>
 * </ul>
 *
 * <p>Resources (change log files) are resolved through the {@link ResourceAccessor} selected by
 * {@link #setSource(Source)}: either a {@code DirectoryResourceAccessor} rooted at the user's
 * home directory ({@link Source#FILE}) or a {@code ClassLoaderResourceAccessor}
 * ({@link Source#CLASSPATH}).</p>
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
   * Constructs an instance using the current thread's context class loader for resource resolution.
   *
   * <p>Equivalent to {@link #SpringLiquibase(ClassLoader)
   * SpringLiquibase(Thread.currentThread().getContextClassLoader())}.</p>
   */
  public SpringLiquibase () {

    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Constructs an instance using the supplied class loader for classpath resource resolution.
   *
   * @param classLoader class loader used when {@link #setSource(Source)} is called with
   *                    {@link Source#CLASSPATH}; must not be {@code null}
   */
  public SpringLiquibase (ClassLoader classLoader) {

    this.classloader = classLoader;
  }

  /**
   * Sets the JDBC data source that Liquibase will use to connect to the target database.
   *
   * @param dataSource a configured, non-null {@link DataSource}; a connection is obtained from it
   *                   for each change log processed by {@link #afterPropertiesSet()}
   */
  public void setDataSource (DataSource dataSource) {

    this.dataSource = dataSource;
  }

  /**
   * Selects where change log resources are located and configures the corresponding
   * Liquibase {@link ResourceAccessor}.
   *
   * <ul>
   *   <li>{@link Source#FILE} — creates a {@code DirectoryResourceAccessor} rooted at
   *       {@code System.getProperty("user.home")}.</li>
   *   <li>{@link Source#CLASSPATH} — creates a {@code ClassLoaderResourceAccessor} using
   *       the class loader supplied at construction time.</li>
   * </ul>
   *
   * @param source the desired resource resolution strategy; must not be {@code null}
   * @throws FileNotFoundException      if {@link Source#FILE} is chosen and the user's home
   *                                    directory cannot be used as a resource root
   * @throws UnknownSwitchCaseException if an unrecognised {@link Source} constant is passed
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
   * Sets the Liquibase action to perform during {@link #afterPropertiesSet()}.
   *
   * @param goal the desired operation; must not be {@code null}
   * @see Goal
   */
  public void setGoal (Goal goal) {

    this.goal = goal;
  }

  /**
   * Sets the stream that receives generated SQL when the goal is {@link Goal#PREVIEW}.
   *
   * <p>If this property is not set (or set to {@code null}), preview output is written to
   * {@link System#out}.</p>
   *
   * @param previewStream destination for SQL preview output; {@code null} falls back to
   *                      {@link System#out}
   */
  public void setPreviewStream (OutputStream previewStream) {

    this.previewStream = previewStream;
  }

  /**
   * Sets the ordered list of change logs to process.
   *
   * <p>Each change log is processed independently in array order. For {@link Goal#GENERATE},
   * only one output file is written per distinct database catalog regardless of how many
   * change logs share the same catalog.</p>
   *
   * @param changeLogs array of change logs to process; must not be {@code null} when the goal
   *                   is not {@link Goal#NONE}
   */
  public void setChangeLogs (ChangeLog[] changeLogs) {

    this.changeLogs = changeLogs;
  }

  /**
   * Sets custom Liquibase data types to register before any change logs are processed.
   *
   * <p>For each data type, any existing registration under the same name is removed before the
   * new type is registered, ensuring the custom implementation takes precedence over built-in
   * or previously registered types.</p>
   *
   * @param dataTypes array of custom data types to register; may be {@code null} to skip
   *                  type registration
   */
  public void setDataTypes (LiquibaseDataType[] dataTypes) {

    this.dataTypes = dataTypes;
  }

  /**
   * Sets the comma-delimited list of Liquibase contexts used to filter change sets.
   *
   * <p>Only change sets matching at least one of the supplied contexts (or change sets with no
   * context restriction) will be included. Pass {@code null} or an empty string to disable
   * context filtering.</p>
   *
   * @param contexts comma-delimited context names, or {@code null} to apply no context filter
   */
  public void setContexts (String contexts) {

    this.contexts = contexts;
  }

  /**
   * Sets the directory to which generated artifacts are written.
   *
   * <p>Used by {@link Goal#DOCUMENT} and {@link Goal#GENERATE}. When this property is
   * {@code null} or empty, both goals fall back to the system temporary directory
   * ({@code System.getProperty("java.io.tmpdir")}).</p>
   *
   * @param outputDir filesystem path of the output directory; {@code null} or empty causes
   *                  the system temporary directory to be used
   */
  public void setOutputDir (String outputDir) {

    this.outputDir = outputDir;
  }

  /**
   * Registers custom data types and executes the configured Liquibase goal against each change log.
   *
   * <p>Invoked automatically by the Spring container after all bean properties have been injected.
   * The method is annotated {@link Transactional} so that {@link Goal#UPDATE} operations
   * participate in an existing Spring-managed transaction when one is present.</p>
   *
   * <p>Processing order:</p>
   * <ol>
   *   <li>If {@code dataTypes} is non-null, each type is unregistered by name and then
   *       re-registered with the Liquibase {@link DataTypeFactory}.</li>
   *   <li>If the goal is {@link Goal#NONE}, processing stops here.</li>
   *   <li>For each {@link ChangeLog}, a fresh JDBC connection is obtained from the data source,
   *       wrapped in a Liquibase {@link JdbcConnection}, and the goal-specific Liquibase command
   *       is executed within a {@link Scope} that carries the configured {@link ResourceAccessor}.</li>
   * </ol>
   *
   * @throws Exception                  if Liquibase fails to execute a command, if a database
   *                                    connection cannot be obtained, or if an I/O error occurs
   *                                    while writing generated output
   * @throws UnknownSwitchCaseException if the configured {@link Goal} is not handled by the
   *                                    switch statement (indicates a programming error)
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
