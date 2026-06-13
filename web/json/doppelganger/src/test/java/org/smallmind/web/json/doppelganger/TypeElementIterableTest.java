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
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Focused unit test for {@link TypeElementIterable}. A throw-away annotation processor is run through an
 * in-process {@code javac} so that the iterable can be exercised against real, live {@link TypeMirror}s while
 * the {@link ProcessingEnvironment} is still active. The processor resolves each named probe into the qualified
 * names collected by a {@link TypeElementIterable}, asserting its array-descent, type-argument-descent, and
 * non-class-element branches without generating any view sources.
 */
@Test(groups = "integration")
public class TypeElementIterableTest {

  // Runs the probe processor over a fixture source so the iterable can be exercised live inside a round, then
  // hands back the qualified names the iterable collected for each requested probe key.

  private Map<String, List<String>> runProbes (String className, String source, List<String> probeKeys)
    throws IOException {

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    ProbeProcessor probeProcessor = new ProbeProcessor(probeKeys);

    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {

      List<String> options = List.of("-classpath", System.getProperty("java.class.path"));
      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, List.of(new StringSource(className, source)));

      task.setProcessors(List.of(probeProcessor));
      task.call();
    }

    Assert.assertTrue(probeProcessor.wasInvoked(), "the probe processor never ran");

    return probeProcessor.getCollectedNames();
  }

  // A declared generic type descends into its class type argument. The container itself (java.util.List) is an
  // interface, not an ElementKind.CLASS, so it is skipped while the class type argument is collected. An
  // ArrayList<String> (a concrete class) is therefore used to also exercise the class-collected branch.

  public void testDeclaredTypeWithArgumentsCollectsClassContainerAndArgument ()
    throws Exception {

    Map<String, List<String>> collected = runProbes("probe.Holder",
      "package probe;\n"
        + "import java.util.ArrayList;\n"
        + "import java.util.List;\n"
        + "public class Holder {\n"
        + "  public ArrayList<String> getItems () { return null; }\n"
        + "  public List<String> getView () { return null; }\n"
        + "}\n", List.of("return:getItems", "return:getView"));

    List<String> classContainerNames = collected.get("return:getItems");

    Assert.assertTrue(classContainerNames.contains("java.util.ArrayList"), "a concrete class container should be collected: " + classContainerNames);
    Assert.assertTrue(classContainerNames.contains("java.lang.String"), "the class type argument should be collected: " + classContainerNames);

    List<String> interfaceContainerNames = collected.get("return:getView");

    Assert.assertFalse(interfaceContainerNames.contains("java.util.List"), "an interface container should be skipped: " + interfaceContainerNames);
    Assert.assertTrue(interfaceContainerNames.contains("java.lang.String"), "the class type argument should still be collected: " + interfaceContainerNames);
  }

  // An array-of-class field descends through the array component type to the underlying class.

  public void testArrayTypeDescendsToComponentClass ()
    throws Exception {

    Map<String, List<String>> collected = runProbes("probe.Bag",
      "package probe;\n"
        + "public class Bag {\n"
        + "  public String[] getTags () { return null; }\n"
        + "}\n", List.of("return:getTags"));

    Assert.assertEquals(collected.get("return:getTags"), List.of("java.lang.String"), "array descent should yield only the component class");
  }

  // A primitive return type matches neither the array nor the declared branch and collects nothing.

  public void testPrimitiveTypeCollectsNothing ()
    throws Exception {

    Map<String, List<String>> collected = runProbes("probe.Counter",
      "package probe;\n"
        + "public class Counter {\n"
        + "  public int getCount () { return 0; }\n"
        + "}\n", List.of("return:getCount"));

    Assert.assertTrue(collected.get("return:getCount").isEmpty(), "a primitive type should collect no class elements: " + collected.get("return:getCount"));
  }

  // A declared interface return type is not an ElementKind.CLASS, so the interface itself is skipped while
  // its class type argument is still collected.

  public void testDeclaredInterfaceElementIsSkipped ()
    throws Exception {

    Map<String, List<String>> collected = runProbes("probe.Service",
      "package probe;\n"
        + "import java.util.Comparator;\n"
        + "public class Service {\n"
        + "  public Comparator<String> getComparator () { return null; }\n"
        + "}\n", List.of("return:getComparator"));

    List<String> names = collected.get("return:getComparator");

    Assert.assertFalse(names.contains("java.util.Comparator"), "an interface element should be skipped: " + names);
    Assert.assertTrue(names.contains("java.lang.String"), "the class type argument should still be collected: " + names);
  }

  // NOTE: TypeElementIterable.pushTypeElement is unreachable production code -- the constructor only invokes
  // pushTypeMirror and nothing else references pushTypeElement. It is exercised here via reflection (driven inside
  // a live processing round) to pin its current behavior: the class element itself is collected directly, but a
  // type parameter is descended into via its own asType(), which is a TYPEVAR and therefore matches neither the
  // array nor the declared branch of pushTypeMirror -- so the type parameter bound is NOT collected.

  public void testPushTypeElementReflectivelyCollectsClassButNotTypeParameterBound ()
    throws Exception {

    Map<String, List<String>> collected = runProbes("probe.Box",
      "package probe;\n"
        + "public class Box<T extends Number> {\n"
        + "}\n", List.of("pushElement:probe.Box"));

    List<String> names = collected.get("pushElement:probe.Box");

    Assert.assertEquals(names, List.of("probe.Box"), "pushTypeElement should collect only the class itself, not the TYPEVAR type-parameter bound");
  }

  // Probe processor that, while the environment is live, builds a TypeElementIterable for each requested probe
  // key and records the qualified names it yields. Two probe forms are supported:
  //   return:<methodName>   -- iterate the return type mirror of a declared method (the production code path)
  //   pushElement:<fqcn>    -- reflectively invoke the otherwise-unreachable pushTypeElement on the named class

  @SupportedAnnotationTypes("*")
  private static final class ProbeProcessor extends AbstractProcessor {

    private final Map<String, List<String>> collectedNames = new LinkedHashMap<>();
    private final List<String> probeKeys;
    private boolean invoked;

    private ProbeProcessor (List<String> probeKeys) {

      this.probeKeys = probeKeys;
    }

    @Override
    public SourceVersion getSupportedSourceVersion () {

      return SourceVersion.latestSupported();
    }

    @Override
    public boolean process (Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

      if ((!roundEnv.processingOver()) && (!roundEnv.getRootElements().isEmpty())) {
        invoked = true;

        for (String probeKey : probeKeys) {
          if (probeKey.startsWith("return:")) {
            collectedNames.put(probeKey, collectReturnTypeNames(roundEnv, probeKey.substring("return:".length())));
          } else if (probeKey.startsWith("pushElement:")) {
            collectedNames.put(probeKey, collectPushTypeElementNames(probeKey.substring("pushElement:".length())));
          }
        }
      }

      return false;
    }

    private List<String> collectReturnTypeNames (RoundEnvironment roundEnv, String methodName) {

      TypeMirror returnTypeMirror = null;

      for (Element rootElement : roundEnv.getRootElements()) {
        if (ElementKind.CLASS.equals(rootElement.getKind())) {
          for (Element enclosedElement : rootElement.getEnclosedElements()) {
            if (ElementKind.METHOD.equals(enclosedElement.getKind()) && enclosedElement.getSimpleName().toString().equals(methodName)) {
              returnTypeMirror = ((ExecutableElement)enclosedElement).getReturnType();
            }
          }
        }
      }

      Assert.assertNotNull(returnTypeMirror, "no method named(" + methodName + ") in the probe source");

      return namesOf(new TypeElementIterable(processingEnv, returnTypeMirror));
    }

    private List<String> collectPushTypeElementNames (String qualifiedName) {

      try {

        TypeElement targetElement = processingEnv.getElementUtils().getTypeElement(qualifiedName);
        // Seed the iterable with a primitive type mirror so the constructor's pushTypeMirror collects nothing,
        // leaving only the names contributed by the subsequent reflective pushTypeElement call.
        TypeElementIterable typeElementIterable = new TypeElementIterable(processingEnv, processingEnv.getTypeUtils().getPrimitiveType(TypeKind.INT));
        Method pushTypeElement = TypeElementIterable.class.getDeclaredMethod("pushTypeElement", ProcessingEnvironment.class, TypeElement.class);

        pushTypeElement.setAccessible(true);
        pushTypeElement.invoke(typeElementIterable, processingEnv, targetElement);

        return namesOf(typeElementIterable);
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }

    private List<String> namesOf (TypeElementIterable typeElementIterable) {

      List<String> names = new ArrayList<>();

      for (TypeElement typeElement : typeElementIterable) {
        names.add(typeElement.getQualifiedName().toString());
      }

      return names;
    }

    private boolean wasInvoked () {

      return invoked;
    }

    private Map<String, List<String>> getCollectedNames () {

      return collectedNames;
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
