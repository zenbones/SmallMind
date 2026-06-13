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
package org.smallmind.web.json.doppelganger;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Reusable harness that drives the {@link DoppelgangerAnnotationProcessor} through an in-process
 * {@code javac} ({@link ToolProvider#getSystemJavaCompiler}) over a set of in-memory source strings,
 * capturing the generated view sources, compilation success, and diagnostics.
 */
public class DoppelgangerCompilationFixture {

  private final Path sourceOutput;
  private final Path classOutput;

  /**
   * Creates a fixture backed by fresh temporary source-output and class-output directories.
   *
   * @throws IOException if the temporary directories cannot be created
   */
  public DoppelgangerCompilationFixture ()
    throws IOException {

    sourceOutput = Files.createTempDirectory("doppelganger-gen-sources");
    classOutput = Files.createTempDirectory("doppelganger-gen-classes");
  }

  /**
   * Compiles the given named sources with the Doppelganger processor attached.
   *
   * @param sources map of fully qualified class name to source text
   * @return the result describing success, errors, warnings, and generated source files
   * @throws IOException if file-manager configuration or generated-file scanning fails
   */
  public Result compile (Map<String, String> sources)
    throws IOException {

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {

      List<StringSource> sourceList = new ArrayList<>();

      fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(sourceOutput.toFile()));
      fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classOutput.toFile()));

      for (Map.Entry<String, String> sourceEntry : sources.entrySet()) {
        sourceList.add(new StringSource(sourceEntry.getKey(), sourceEntry.getValue()));
      }

      List<String> options = List.of("-classpath", System.getProperty("java.class.path"));
      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, sourceList);

      task.setProcessors(List.of(new DoppelgangerAnnotationProcessor()));

      boolean success = task.call();

      List<String> errors = new ArrayList<>();
      List<String> warnings = new ArrayList<>();

      for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
          errors.add(diagnostic.getMessage(null));
        } else if (diagnostic.getKind() == Diagnostic.Kind.WARNING) {
          warnings.add(diagnostic.getMessage(null));
        }
      }

      HashMap<String, String> generated = new HashMap<>();

      try (var paths = Files.walk(sourceOutput)) {
        for (Path path : (Iterable<Path>)paths.filter(Files::isRegularFile)::iterator) {
          generated.put(sourceOutput.relativize(path).toString().replace('\\', '/'), Files.readString(path));
        }
      }

      return new Result(success, errors, warnings, generated);
    }
  }

  /**
   * Recursively deletes the temporary directories backing this fixture.
   *
   * @throws IOException if the directory tree cannot be walked
   */
  public void cleanup ()
    throws IOException {

    for (Path root : new Path[] {sourceOutput, classOutput}) {
      if (root != null) {
        try (var paths = Files.walk(root)) {
          paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
      }
    }
  }

  /**
   * Immutable holder for the outcome of a single compilation run.
   */
  public static final class Result {

    private final boolean success;
    private final List<String> errors;
    private final List<String> warnings;
    private final Map<String, String> generated;

    private Result (boolean success, List<String> errors, List<String> warnings, Map<String, String> generated) {

      this.success = success;
      this.errors = errors;
      this.warnings = warnings;
      this.generated = generated;
    }

    public boolean isSuccess () {

      return success;
    }

    public List<String> getErrors () {

      return errors;
    }

    public List<String> getWarnings () {

      return warnings;
    }

    public Map<String, String> getGenerated () {

      return generated;
    }

    public boolean hasGenerated (String relativePath) {

      return generated.containsKey(relativePath);
    }

    public String getSource (String relativePath) {

      return generated.get(relativePath);
    }
  }

  private static final class StringSource extends SimpleJavaFileObject {

    private final String code;

    private StringSource (String className, String code) {

      super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);

      this.code = code;
    }

    @Override
    public CharSequence getCharContent (boolean ignoreEncodingErrors)
      throws IOException {

      return code;
    }
  }
}
