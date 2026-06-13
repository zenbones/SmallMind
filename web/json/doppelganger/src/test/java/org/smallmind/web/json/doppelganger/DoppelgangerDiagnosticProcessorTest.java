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
import java.util.LinkedHashMap;
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
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Integration tests that drive the {@link DoppelgangerAnnotationProcessor} diagnostic, option, and directional
 * idiom branches that the shared {@link DoppelgangerCompilationFixture} cannot reach: the {@code prefix}
 * processor option, the top-level-class guard, polymorphic-subclass package validation, the unnecessary-pledge
 * warning, purpose-and-visibility-scoped imports and implementations, and the generic malformed-type rejection.
 */
@Test(groups = "integration")
public class DoppelgangerDiagnosticProcessorTest {

  private Path sourceOutput;
  private Path classOutput;

  @AfterMethod(alwaysRun = true)
  public void afterMethod ()
    throws Exception {

    for (Path root : new Path[] {sourceOutput, classOutput}) {
      if (root != null) {
        try (var paths = Files.walk(root)) {
          paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
      }
    }
    sourceOutput = null;
    classOutput = null;
  }

  // Compiles the given sources with an optional 'prefix' processor option and reports success, diagnostics,
  // and generated sources.

  private Result compile (Map<String, String> sources, String prefixOption)
    throws IOException {

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    sourceOutput = Files.createTempDirectory("doppelganger-diag-sources");
    classOutput = Files.createTempDirectory("doppelganger-diag-classes");

    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {

      List<StringSource> sourceList = new ArrayList<>();
      List<String> options = new ArrayList<>();

      fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(sourceOutput.toFile()));
      fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classOutput.toFile()));

      for (Map.Entry<String, String> sourceEntry : sources.entrySet()) {
        sourceList.add(new StringSource(sourceEntry.getKey(), sourceEntry.getValue()));
      }

      options.add("-classpath");
      options.add(System.getProperty("java.class.path"));
      if (prefixOption != null) {
        options.add("-Aprefix=" + prefixOption);
      }

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

      List<String> generated = new ArrayList<>();

      try (var paths = Files.walk(sourceOutput)) {
        paths.filter(Files::isRegularFile).forEach(path -> generated.add(sourceOutput.relativize(path).toString().replace('\\', '/')));
      }

      return new Result(success, errors, warnings, generated);
    }
  }

  // NameUtility + processor: the 'prefix' processor option is prepended to every generated view name.

  public void testPrefixOptionRenamesGeneratedViews ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Knob",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"id\", type = @Type(Long.class)))\n"
        + "public class Knob {\n"
        + "  private Long id;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "}\n");

    Result result = compile(sources, "Api");

    Assert.assertTrue(result.isSuccess(), "compilation failed: " + result.getErrors());
    Assert.assertTrue(result.getGenerated().contains("widget/ApiKnobInView.java"), result.getGenerated().toString());
    Assert.assertTrue(result.getGenerated().contains("widget/ApiKnobOutView.java"), result.getGenerated().toString());

    String outView = Files.readString(sourceOutput.resolve("widget/ApiKnobOutView.java"));
    Assert.assertTrue(outView.contains("class ApiKnobOutView"), "the prefix should appear in the class declaration");
  }

  // processor.generate: a @Doppelganger annotation on a nested (non-top-level) class is rejected.

  public void testNestedClassIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Outer",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "public class Outer {\n"
        + "  @Doppelganger(real = @Real(field = \"id\", type = @Type(Long.class)))\n"
        + "  public static class Inner {\n"
        + "    private Long id;\n"
        + "    public Long getId () { return id; }\n"
        + "    public void setId (Long id) { this.id = id; }\n"
        + "  }\n"
        + "}\n");

    Result result = compile(sources, null);

    Assert.assertFalse(result.isSuccess(), "a nested @Doppelganger class should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("must be a root implementation of type 'class'")), result.getErrors().toString());
  }

  // processor.writeView: a polymorphic subclass declared in a different package than the base is rejected.

  public void testPolymorphicSubclassInWrongPackageIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("zoo.Beast",
      "package zoo;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Polymorphic;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(polymorphic = @Polymorphic(subClasses = {other.Wolf.class}), real = @Real(field = \"name\", type = @Type(String.class)))\n"
        + "public abstract class Beast {\n"
        + "  private String name;\n"
        + "  public String getName () { return name; }\n"
        + "  public void setName (String name) { this.name = name; }\n"
        + "}\n");
    sources.put("other.Wolf",
      "package other;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"pack\", type = @Type(String.class)))\n"
        + "public class Wolf extends zoo.Beast {\n"
        + "  private String pack;\n"
        + "  public String getPack () { return pack; }\n"
        + "  public void setPack (String pack) { this.pack = pack; }\n"
        + "}\n");

    Result result = compile(sources, null);

    Assert.assertFalse(result.isSuccess(), "a polymorphic subclass in another package should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("must be in package")), result.getErrors().toString());
  }

  // processor.generate: a @Pledge for a purpose that real properties already fulfill in both directions
  // triggers the unnecessary-pledge warning naming both IN and OUT.

  public void testUnnecessaryPledgeEmitsWarning ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Overdone",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Pledge;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger(pledges = @Pledge(visibility = Visibility.BOTH, purposes = \"detail\"), real = @Real(field = \"id\", type = @Type(Long.class), idioms = @Idiom(purposes = \"detail\", visibility = Visibility.BOTH)))\n"
        + "public class Overdone {\n"
        + "  private Long id;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "}\n");

    Result result = compile(sources, null);

    Assert.assertTrue(result.isSuccess(), "compilation failed: " + result.getErrors());
    Assert.assertTrue(result.getWarnings().stream().anyMatch((message) -> message.contains("unnecessary @Pledge")), result.getWarnings().toString());
    Assert.assertTrue(result.getWarnings().stream().anyMatch((message) -> message.contains("IN[detail]") && message.contains("OUT[detail]")), "both directions should be named in the warning: " + result.getWarnings());
  }

  // DoppelgangerInformation: an @Import scoped to a single direction by visibility lands only in that direction's view.

  public void testVisibilityScopedImportLandsInOneDirection ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Shipment",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "import org.smallmind.web.json.doppelganger.Import;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger(imports = @Import(visibility = Visibility.OUT, value = \"java.util.UUID\"), real = @Real(field = \"id\", type = @Type(Long.class)))\n"
        + "public class Shipment {\n"
        + "  private Long id;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "}\n");

    Result result = compile(sources, null);

    Assert.assertTrue(result.isSuccess(), "compilation failed: " + result.getErrors());

    String outView = Files.readString(sourceOutput.resolve("widget/ShipmentOutView.java"));
    Assert.assertTrue(outView.contains("import java.util.UUID;"), "an OUT-scoped import should land in the out view");

    String inView = Files.readString(sourceOutput.resolve("widget/ShipmentInView.java"));
    Assert.assertFalse(inView.contains("import java.util.UUID;"), "an OUT-scoped import should not leak into the in view");
  }

  // DoppelgangerInformation: an @Implementation scoped to a purpose lands only on that purpose's view, exercising
  // the non-empty-purpose branch of the implementation loop.

  public void testPurposeScopedImplementation ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Badge",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Implementation;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger(implementations = @Implementation(purposes = \"detail\", visibility = Visibility.OUT, value = java.io.Serializable.class), real = {\n"
        + "  @Real(field = \"id\", type = @Type(Long.class)),\n"
        + "  @Real(field = \"name\", type = @Type(String.class), idioms = @Idiom(purposes = \"detail\", visibility = Visibility.OUT))\n"
        + "})\n"
        + "public class Badge {\n"
        + "  private Long id;\n"
        + "  private String name;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "  public String getName () { return name; }\n"
        + "  public void setName (String name) { this.name = name; }\n"
        + "}\n");

    Result result = compile(sources, null);

    Assert.assertTrue(result.isSuccess(), "compilation failed: " + result.getErrors());

    String detailOut = Files.readString(sourceOutput.resolve("widget/BadgeDetailOutView.java"));
    Assert.assertTrue(detailOut.contains("implements java.io.Serializable"), "the purpose-scoped implementation should land on the detail out view");

    String defaultOut = Files.readString(sourceOutput.resolve("widget/BadgeOutView.java"));
    Assert.assertFalse(defaultOut.contains("implements java.io.Serializable"), "the purpose-scoped implementation should not land on the default view");
  }

  // DoppelgangerInformation.extractType: a non-array @Type that names a primitive base value (which cannot be
  // resolved as a declared type) drives the generic 'Illegal type definition' catch rather than the array-specific
  // message.
  // NOTE: extractType funnels every non-DefinitionException failure into the generic "Illegal type definition"
  // message, dropping the underlying cause text; this pins that current behavior.

  public void testPrimitiveBaseTypeIsRejectedAsIllegal ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Measure",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"size\", type = @Type(value = int.class, parameters = String.class)))\n"
        + "public class Measure {\n"
        + "  private int size;\n"
        + "  public int getSize () { return size; }\n"
        + "  public void setSize (int size) { this.size = size; }\n"
        + "}\n");

    Result result = compile(sources, null);

    Assert.assertFalse(result.isSuccess(), "a primitive base type with parameters should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("Illegal type definition")), result.getErrors().toString());
  }

  private static final class Result {

    private final boolean success;
    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> generated;

    private Result (boolean success, List<String> errors, List<String> warnings, List<String> generated) {

      this.success = success;
      this.errors = errors;
      this.warnings = warnings;
      this.generated = generated;
    }

    private boolean isSuccess () {

      return success;
    }

    private List<String> getErrors () {

      return errors;
    }

    private List<String> getWarnings () {

      return warnings;
    }

    private List<String> getGenerated () {

      return generated;
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
