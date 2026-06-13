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
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration test that runs the {@link DoppelgangerAnnotationProcessor} through an in-process
 * {@code javac} ({@link ToolProvider#getSystemJavaCompiler}) over an annotated source, and asserts the
 * expected directional/purpose view sources are generated and compile cleanly.
 */
@Test(groups = "integration")
public class DoppelgangerProcessorTest {

  private static final String SOURCE =
    "package widget;\n"
      + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
      + "import org.smallmind.web.json.doppelganger.Real;\n"
      + "import org.smallmind.web.json.doppelganger.Type;\n"
      + "import org.smallmind.web.json.doppelganger.Idiom;\n"
      + "import org.smallmind.web.json.doppelganger.Visibility;\n"
      + "@Doppelganger(real = {\n"
      + "  @Real(field = \"id\", type = @Type(Long.class)),\n"
      + "  @Real(field = \"name\", type = @Type(String.class), idioms = @Idiom(purposes = \"create\", visibility = Visibility.IN))\n"
      + "})\n"
      + "public class Widget {\n"
      + "  private Long id;\n"
      + "  private String name;\n"
      + "  public Long getId () { return id; }\n"
      + "  public void setId (Long id) { this.id = id; }\n"
      + "  public String getName () { return name; }\n"
      + "  public void setName (String name) { this.name = name; }\n"
      + "}\n";

  private Path sourceOutput;
  private Path classOutput;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    sourceOutput = Files.createTempDirectory("doppelganger-gen-sources");
    classOutput = Files.createTempDirectory("doppelganger-gen-classes");
  }

  @AfterClass(alwaysRun = true)
  public void afterClass ()
    throws Exception {

    for (Path root : new Path[] {sourceOutput, classOutput}) {
      if (root != null) {
        try (var paths = Files.walk(root)) {
          paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
      }
    }
  }

  public void testGeneratesDirectionalViews ()
    throws Exception {

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {

      fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(sourceOutput.toFile()));
      fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classOutput.toFile()));

      List<String> options = List.of("-classpath", System.getProperty("java.class.path"));
      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, List.of(new StringSource("widget.Widget", SOURCE)));

      task.setProcessors(List.of(new DoppelgangerAnnotationProcessor()));

      boolean success = task.call();

      List<String> errors = new ArrayList<>();
      for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
          errors.add(diagnostic.getMessage(null));
        }
      }

      List<String> generated = new ArrayList<>();
      try (var paths = Files.walk(sourceOutput)) {
        paths.filter(Files::isRegularFile).forEach(path -> generated.add(sourceOutput.relativize(path).toString().replace('\\', '/')));
      }

      Assert.assertTrue(success, "compilation failed: " + errors);
      Assert.assertTrue(generated.contains("widget/WidgetInView.java"), "WidgetInView.java not generated; generated=" + generated);
      Assert.assertTrue(generated.contains("widget/WidgetOutView.java"), "WidgetOutView.java not generated; generated=" + generated);
      Assert.assertTrue(generated.contains("widget/WidgetCreateInView.java"), "WidgetCreateInView.java not generated; generated=" + generated);

      String widgetCreateInView = Files.readString(sourceOutput.resolve("widget/WidgetCreateInView.java"));
      Assert.assertTrue(widgetCreateInView.contains("name"), "create-in view should carry the inbound 'name' property");
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
