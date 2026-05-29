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
package org.smallmind.nutsnbolts.apt;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "integration")
public class AptUtilityTest {

  public void testProcessorReadsAnnotationValueViaAptUtility ()
    throws Exception {

    String source = """
      package sample;

      import org.smallmind.nutsnbolts.apt.TestMark;

      @TestMark(name = "from-source")
      public class Annotated {
      }
      """;

    CapturingProcessor processor = new CapturingProcessor();

    runCompilation(new InMemoryJavaFileObject("sample.Annotated", source), processor);

    Assert.assertEquals(processor.capturedNames.size(), 1);
    Assert.assertEquals(processor.capturedNames.get(0), "from-source");
  }

  public void testProcessorReadsDefaultAnnotationValueViaAptUtility ()
    throws Exception {

    String source = """
      package sample;

      import org.smallmind.nutsnbolts.apt.TestMark;

      @TestMark
      public class AnnotatedDefault {
      }
      """;

    CapturingProcessor processor = new CapturingProcessor();

    runCompilation(new InMemoryJavaFileObject("sample.AnnotatedDefault", source), processor);

    Assert.assertEquals(processor.capturedDefaultedNames.size(), 1);
    Assert.assertEquals(processor.capturedDefaultedNames.get(0), "default-name");
  }

  private static void runCompilation (JavaFileObject sourceFile, AbstractProcessor processor)
    throws IOException {

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    Path outputDir = Files.createTempDirectory("apt-out");

    try (var fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {

      fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(outputDir));

      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, List.of(sourceFile));

      task.setProcessors(List.of(processor));
      Assert.assertTrue(task.call(), "Compilation failed");
    }
  }

  @SupportedAnnotationTypes("org.smallmind.nutsnbolts.apt.TestMark")
  @SupportedSourceVersion(SourceVersion.RELEASE_25)
  private static class CapturingProcessor extends AbstractProcessor {

    private final java.util.List<String> capturedNames = new java.util.ArrayList<>();
    private final java.util.List<String> capturedDefaultedNames = new java.util.ArrayList<>();

    @Override
    public boolean process (Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

      if (annotations.isEmpty()) {

        return false;
      }

      TypeElement markAnnotationElement = processingEnv.getElementUtils().getTypeElement("org.smallmind.nutsnbolts.apt.TestMark");

      for (Element element : roundEnv.getElementsAnnotatedWith(markAnnotationElement)) {

        AnnotationMirror mirror = AptUtility.extractAnnotationMirror(processingEnv, element, markAnnotationElement.asType());

        Assert.assertNotNull(mirror);

        String explicitName = AptUtility.extractAnnotationValue(mirror, "name", String.class, null);

        if (explicitName != null) {
          capturedNames.add(explicitName);
        }

        String defaultedName = AptUtility.extractAnnotationValueWithDefault(processingEnv, mirror, "name", String.class);

        capturedDefaultedNames.add(defaultedName);
      }

      return true;
    }
  }

  private static class InMemoryJavaFileObject extends SimpleJavaFileObject {

    private final String contents;

    InMemoryJavaFileObject (String name, String contents) {

      super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
      this.contents = contents;
    }

    @Override
    public CharSequence getCharContent (boolean ignoreEncodingErrors) {

      return contents;
    }
  }
}
