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
package org.smallmind.nutsnbolts.reflection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.tools.ToolProvider;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.smallmind.nutsnbolts.reflection.sample.AnnotatedBag;
import org.smallmind.nutsnbolts.reflection.sample.Calculator;
import org.smallmind.nutsnbolts.reflection.sample.ClosedShape;
import org.smallmind.nutsnbolts.reflection.sample.Counter;
import org.smallmind.nutsnbolts.reflection.sample.DescribedBag;
import org.smallmind.nutsnbolts.reflection.sample.Failing;
import org.smallmind.nutsnbolts.reflection.sample.FinalNamed;
import org.smallmind.nutsnbolts.reflection.sample.Greeter;
import org.smallmind.nutsnbolts.reflection.sample.OpenShape;
import org.smallmind.nutsnbolts.reflection.sample.PairList;
import org.smallmind.nutsnbolts.reflection.sample.PojoBag;
import org.smallmind.nutsnbolts.reflection.sample.PoliteGreeter;
import org.smallmind.nutsnbolts.reflection.sample.SealedShape;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ProxyGeneratorTest {

  public void testInterfaceProxyDispatchesEveryCallThroughHandler () {

    RecordingHandler recordingHandler = new RecordingHandler("answer");
    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, recordingHandler, true);

    Assert.assertEquals(proxy.greet("world"), "answer");
    Assert.assertEquals(recordingHandler.calls.size(), 1);
    Assert.assertEquals(recordingHandler.calls.get(0).methodName, "greet");
    Assert.assertEquals(recordingHandler.calls.get(0).args[0], "world");
  }

  public void testInterfaceProxyPropagatesPrimitiveReturn () {

    RecordingHandler recordingHandler = new RecordingHandler(42);
    Counter proxy = ProxyGenerator.createProxy(Counter.class, recordingHandler, true);

    Assert.assertEquals(proxy.count(), 42);
  }

  public void testInterfaceProxyForwardsPrimitiveArguments () {

    RecordingHandler recordingHandler = new RecordingHandler(99L);
    Calculator proxy = ProxyGenerator.createProxy(Calculator.class, recordingHandler, true);

    Assert.assertEquals(proxy.add(7, 3), 99L);
    Assert.assertEquals(recordingHandler.calls.get(0).args[0], 7L);
    Assert.assertEquals(recordingHandler.calls.get(0).args[1], 3L);
  }

  public void testInterfaceProxyWithNullHandlerReturnsDefaults () {

    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, null, true);

    Assert.assertNull(proxy.greet("anything"));
  }

  public void testClassProxyRoutesObjectMethodsThroughHandler () {

    RecordingHandler recordingHandler = new RecordingHandler(null) {

      @Override
      public Object invoke (Object proxy, java.lang.reflect.Method method, Object[] args) {

        if ("toString".equals(method.getName())) {
          return "stringified";
        } else if ("hashCode".equals(method.getName())) {
          return 7;
        } else if ("equals".equals(method.getName())) {
          return Boolean.TRUE;
        }

        return null;
      }
    };

    PojoBag proxy = ProxyGenerator.createProxy(PojoBag.class, recordingHandler, true);

    Assert.assertEquals(proxy.toString(), "stringified");
    Assert.assertEquals(proxy.hashCode(), 7);
    Assert.assertTrue(proxy.equals("anything"));
  }

  public void testHandlerRuntimeExceptionPropagatesUnchanged () {

    IllegalStateException forcedException = new IllegalStateException("forced");
    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, (proxyObject, method, args) -> {
      throw forcedException;
    }, true);

    try {
      proxy.greet("x");
      Assert.fail("Expected handler exception to surface");
    } catch (IllegalStateException illegalStateException) {
      Assert.assertSame(illegalStateException, forcedException);
    }
  }

  public void testHandlerUndeclaredCheckedExceptionSurfacesAsUndeclaredThrowable () {

    IOException forcedException = new IOException("forced");
    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, (proxyObject, method, args) -> {
      throw forcedException;
    }, false);

    try {
      proxy.greet("x");
      Assert.fail("Expected handler exception to surface");
    } catch (UndeclaredThrowableException undeclaredThrowableException) {
      Assert.assertSame(undeclaredThrowableException.getCause(), forcedException);
    }
  }

  public void testHandlerDeclaredCheckedExceptionPropagatesUnchanged () {

    IOException forcedException = new IOException("forced");
    Failing proxy = ProxyGenerator.createProxy(Failing.class, (proxyObject, method, args) -> {
      throw forcedException;
    }, true);

    try {
      proxy.check();
      Assert.fail("Expected handler exception to surface");
    } catch (IOException ioException) {
      Assert.assertSame(ioException, forcedException);
    }
  }

  public void testOffloadingHandlerRethrowsTargetExceptions () {

    Failing proxy = ProxyGenerator.createProxy(Failing.class, new OffloadingInvocationHandler(new Failing()), false);

    try {
      proxy.fail();
      Assert.fail("Expected target exception to surface");
    } catch (IllegalStateException illegalStateException) {
      Assert.assertEquals(illegalStateException.getMessage(), "target");
    }

    try {
      proxy.check();
      Assert.fail("Expected target exception to surface");
    } catch (IOException ioException) {
      Assert.assertEquals(ioException.getMessage(), "target");
    }
  }

  public void testInterfaceProxyImplementsSuperinterfaceMethods () {

    for (boolean defineInProxiedModule : new boolean[] {true, false}) {

      RecordingHandler recordingHandler = new RecordingHandler("answer");
      PoliteGreeter proxy = ProxyGenerator.createProxy(PoliteGreeter.class, recordingHandler, defineInProxiedModule);

      Assert.assertEquals(proxy.greet("world"), "answer");
      Assert.assertEquals(proxy.thank("world"), "answer");
      Assert.assertEquals(recordingHandler.calls.get(0).methodName, "greet");
      Assert.assertEquals(recordingHandler.calls.get(1).methodName, "thank");
    }
  }

  public void testInterfaceProxyRoutesObjectMethodsThroughHandler () {

    Greeter implementation = name -> "hello " + name;
    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, new OffloadingInvocationHandler(implementation), false);

    Assert.assertEquals(proxy.toString(), implementation.toString());
    Assert.assertEquals(proxy.hashCode(), implementation.hashCode());
    Assert.assertTrue(proxy.equals(implementation));
  }

  public void testClassProxyRoutesInheritedDefaultMethods () {

    DescribedBag proxy = ProxyGenerator.createProxy(DescribedBag.class, new RecordingHandler("routed"), true);

    Assert.assertEquals(proxy.describe(), "routed");
  }

  public void testFinalOverrideIsNotProxied () {

    FinalNamed proxy = ProxyGenerator.createProxy(FinalNamed.class, new RecordingHandler("routed"), true);

    Assert.assertEquals(proxy.name(), "final");
  }

  public void testNonSealedInterfaceWithSealedSuperinterfaceIsProxied () {

    for (boolean defineInProxiedModule : new boolean[] {true, false}) {

      RecordingHandler recordingHandler = new RecordingHandler("routed");
      OpenShape proxy = ProxyGenerator.createProxy(OpenShape.class, recordingHandler, defineInProxiedModule);

      Assert.assertEquals(proxy.label(), "routed");
      Assert.assertEquals(proxy.outline(), "routed");
      Assert.assertEquals(recordingHandler.calls.get(0).methodName, "label");
      Assert.assertEquals(recordingHandler.calls.get(1).methodName, "outline");
    }
  }

  public void testJdkInterfaceProxiesInProxyClassLoader () {

    Supplier<?> proxy = ProxyGenerator.createProxy(Supplier.class, new RecordingHandler("supplied"), false);

    Assert.assertEquals(proxy.get(), "supplied");
    Assert.assertNull(proxy.getClass().getClassLoader().getParent());
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class, expectedExceptionsMessageRegExp = ".*must open the package\\(java\\.util\\.function\\) to the module\\(unnamed\\)")
  public void testJdkInterfaceIsRejectedInProxiedModule () {

    ProxyGenerator.createProxy(Supplier.class, new RecordingHandler("supplied"), true);
  }

  public void testJdkInterfaceProxiesAreCachedInThisModule ()
    throws Exception {

    Field ancestorProxyClassMapField = ProxyGenerator.class.getDeclaredField("ANCESTOR_PROXY_CLASS_MAP");
    Map<?, ?> ancestorProxyClassMap;
    Supplier<?> first = ProxyGenerator.createProxy(Supplier.class, new RecordingHandler("first"), false);
    Supplier<?> second = ProxyGenerator.createProxy(Supplier.class, new RecordingHandler("second"), false);

    ancestorProxyClassMapField.setAccessible(true);
    ancestorProxyClassMap = (Map<?, ?>)ancestorProxyClassMapField.get(null);

    Assert.assertSame(first.getClass(), second.getClass());
    Assert.assertTrue(ancestorProxyClassMap.containsKey(Supplier.class));

    ProxyGenerator.createProxy(Greeter.class, new RecordingHandler("greeted"), false);
    Assert.assertFalse(ancestorProxyClassMap.containsKey(Greeter.class));
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class, expectedExceptionsMessageRegExp = ".*must not be hidden")
  public void testHiddenClassIsRejected ()
    throws IllegalAccessException {

    Class<?> hiddenClass = MethodHandles.lookup().defineHiddenClass(createPublicClassBytes("org/smallmind/nutsnbolts/reflection/HiddenBean"), true).lookupClass();

    ProxyGenerator.createProxy(hiddenClass, new RecordingHandler("x"), true);
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class, expectedExceptionsMessageRegExp = "Unable to locate the byte code of class\\(org\\.smallmind\\.nutsnbolts\\.reflection\\.MemoryOnlyBean\\).*")
  public void testClassWithoutByteCodeResourceIsRejected ()
    throws IllegalAccessException {

    Class<?> memoryOnlyClass = MethodHandles.lookup().defineClass(createPublicClassBytes("org/smallmind/nutsnbolts/reflection/MemoryOnlyBean"));

    ProxyGenerator.createProxy(memoryOnlyClass, new RecordingHandler("x"), true);
  }

  public void testNonExportedPackageIsRejectedInProxyClassLoader ()
    throws Exception {

    Path sourceDirectory = Files.createTempDirectory("proxy-generator-source");
    Path classDirectory = Files.createTempDirectory("proxy-generator-modules");
    Configuration configuration;
    ModuleLayer moduleLayer;
    Class<?> closedClass;

    writeModuleSource(sourceDirectory, "loaded.closed", "module loaded.closed { }", "Closed", "package loaded.closed; public interface Closed { String name (); }");

    Assert.assertEquals(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classDirectory.toString(), "--module-source-path", sourceDirectory.toString(), "--module", "loaded.closed"), 0);

    configuration = ModuleLayer.boot().configuration().resolve(ModuleFinder.of(classDirectory), ModuleFinder.of(), Set.of("loaded.closed"));
    moduleLayer = ModuleLayer.boot().defineModulesWithOneLoader(configuration, ProxyGeneratorTest.class.getClassLoader());
    closedClass = moduleLayer.findLoader("loaded.closed").loadClass("loaded.closed.Closed");

    try {
      ProxyGenerator.createProxy(closedClass, new RecordingHandler(null), false);
      Assert.fail("Expected the non-exported package to be rejected");
    } catch (ByteCodeManipulationException byteCodeManipulationException) {
      Assert.assertEquals(byteCodeManipulationException.getMessage(), "The module(loaded.closed) must export the package(loaded.closed) of the proxy class(loaded.closed.Closed) unconditionally");
    }
  }

  public void testSignatureTypeInvisibleToProxiedClassLoaderIsRejected ()
    throws Exception {

    Path classDirectory = Files.createTempDirectory("proxy-generator-split");
    Path packageDirectory = Files.createDirectories(classDirectory.resolve("loaded").resolve("split"));
    Path tokenSource = Files.writeString(packageDirectory.resolve("Token.java"), "package loaded.split; public class Token { }");
    Path takerSource = Files.writeString(packageDirectory.resolve("Taker.java"), "package loaded.split; public interface Taker { void take (Token token); }");
    Path concreteSource = Files.writeString(packageDirectory.resolve("Concrete.java"), "package loaded.split; public class Concrete implements Taker { public void take (Token token) { } }");

    Assert.assertEquals(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classDirectory.toString(), tokenSource.toString(), takerSource.toString(), concreteSource.toString()), 0);

    try (URLClassLoader interfaceLoader = new URLClassLoader(new URL[] {classDirectory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {

      Class<?> concreteClass = new ConcreteOnlyClassLoader(classDirectory, interfaceLoader).loadClass("loaded.split.Concrete");

      try {
        ProxyGenerator.createProxy(concreteClass, new RecordingHandler(null), false);
        Assert.fail("Expected the type only the interface's loader can see to be rejected");
      } catch (ByteCodeManipulationException byteCodeManipulationException) {
        Assert.assertEquals(byteCodeManipulationException.getMessage(), "Unable to load the type(loaded.split.Token) in the signature of method(take) through the class loader of the proxy class(loaded.split.Concrete)");
      }
    }
  }

  public void testGenerationIsCachedPerAnnotationFilter ()
    throws NoSuchMethodException {

    AnnotatedBag filtered = ProxyGenerator.createProxy(AnnotatedBag.class, null, new AnnotationFilter(PassType.EXCLUDE, Deprecated.class), true);
    AnnotatedBag unfiltered = ProxyGenerator.createProxy(AnnotatedBag.class, null, null, true);

    Assert.assertFalse(filtered.getClass().getMethod("value").isAnnotationPresent(Deprecated.class));
    Assert.assertTrue(unfiltered.getClass().getMethod("value").isAnnotationPresent(Deprecated.class));
    Assert.assertSame(ProxyGenerator.createProxy(AnnotatedBag.class, null, new AnnotationFilter(PassType.EXCLUDE, Deprecated.class), true).getClass(), filtered.getClass());
  }

  public void testGenerationIsCachedAcrossInvocations () {

    Greeter first = ProxyGenerator.createProxy(Greeter.class, new RecordingHandler("a"), true);
    Greeter second = ProxyGenerator.createProxy(Greeter.class, new RecordingHandler("b"), true);

    Assert.assertSame(first.getClass(), second.getClass());
    Assert.assertNotSame(first, second);
  }

  public void testProxiedModuleDefinitionSharesClassLoaderOfProxiedClass () {

    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, new RecordingHandler("a"), true);

    Assert.assertSame(proxy.getClass().getClassLoader(), Greeter.class.getClassLoader());
    Assert.assertSame(proxy.getClass().getModule(), Greeter.class.getModule());
  }

  public void testProxyClassLoaderDefinitionDispatchesThroughHandler () {

    RecordingHandler recordingHandler = new RecordingHandler("answer");
    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, recordingHandler, false);

    Assert.assertEquals(proxy.greet("world"), "answer");
    Assert.assertEquals(recordingHandler.calls.get(0).methodName, "greet");
    Assert.assertNotSame(proxy.getClass().getClassLoader(), Greeter.class.getClassLoader());
    Assert.assertSame(proxy.getClass().getClassLoader().getParent(), Greeter.class.getClassLoader());
  }

  public void testDefinitionModesAreCachedSeparately () {

    Greeter inProxiedModule = ProxyGenerator.createProxy(Greeter.class, new RecordingHandler("a"), true);
    Greeter inProxyClassLoader = ProxyGenerator.createProxy(Greeter.class, new RecordingHandler("b"), false);

    Assert.assertNotSame(inProxiedModule.getClass(), inProxyClassLoader.getClass());
    Assert.assertSame(ProxyGenerator.createProxy(Greeter.class, new RecordingHandler("c"), false).getClass(), inProxyClassLoader.getClass());
  }

  public void testInterfaceProxyOffloadsToImplementationInEitherDefinitionMode () {

    Greeter implementation = name -> "hello " + name;

    Assert.assertEquals(ProxyGenerator.createProxy(Greeter.class, new OffloadingInvocationHandler(implementation), true).greet("world"), "hello world");
    Assert.assertEquals(ProxyGenerator.createProxy(Greeter.class, new OffloadingInvocationHandler(implementation), false).greet("world"), "hello world");
  }

  public void testClassProxyReadsInheritedJdkByteCode () {

    PairList proxy = ProxyGenerator.createProxy(PairList.class, new OffloadingInvocationHandler(new PairList()), true);

    Assert.assertEquals(proxy.size(), 2);
    Assert.assertEquals(proxy.get(1), "second");
    Assert.assertTrue(proxy.contains("first"));
  }

  public void testParameterTypesResolveThroughProxyClassLoader ()
    throws Exception {

    Path classDirectory = Files.createTempDirectory("proxy-generator");
    Path packageDirectory = Files.createDirectories(classDirectory.resolve("loaded"));
    Path markerSource = Files.writeString(packageDirectory.resolve("Marker.java"), "package loaded; public class Marker { public String toString () { return \"marker\"; } }");
    Path shouterSource = Files.writeString(packageDirectory.resolve("Shouter.java"), "package loaded; public interface Shouter { String shout (Marker marker); }");

    Assert.assertEquals(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classDirectory.toString(), markerSource.toString(), shouterSource.toString()), 0);

    try (URLClassLoader classLoader = new URLClassLoader(new URL[] {classDirectory.toUri().toURL()}, ProxyGeneratorTest.class.getClassLoader())) {

      Class<?> shouterClass = classLoader.loadClass("loaded.Shouter");
      Class<?> markerClass = classLoader.loadClass("loaded.Marker");

      for (boolean defineInProxiedModule : new boolean[] {true, false}) {

        Object proxy = ProxyGenerator.createProxy(shouterClass, (proxyObject, method, args) -> "shouted " + args[0], defineInProxiedModule);

        Assert.assertEquals(shouterClass.getMethod("shout", markerClass).invoke(proxy, markerClass.getConstructor().newInstance()), "shouted marker");
      }
    }
  }

  public void testNamedModuleNeedNotReadThisModule ()
    throws Exception {

    Path sourceDirectory = Files.createTempDirectory("proxy-generator-source");
    Path classDirectory = Files.createTempDirectory("proxy-generator-module");
    Path packageDirectory = Files.createDirectories(sourceDirectory.resolve("loaded").resolve("named"));
    Path moduleSource = Files.writeString(sourceDirectory.resolve("module-info.java"), "open module loaded.named { }");
    Path phraseSource = Files.writeString(packageDirectory.resolve("Phrase.java"), "package loaded.named; public class Phrase { public String toString () { return \"phrase\"; } }");
    Path speakerSource = Files.writeString(packageDirectory.resolve("Speaker.java"), "package loaded.named; public interface Speaker { String speak (Phrase phrase); }");
    Configuration configuration;
    ModuleLayer moduleLayer;
    Class<?> speakerClass;
    Class<?> phraseClass;

    Assert.assertEquals(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classDirectory.toString(), moduleSource.toString(), phraseSource.toString(), speakerSource.toString()), 0);

    configuration = ModuleLayer.boot().configuration().resolve(ModuleFinder.of(classDirectory), ModuleFinder.of(), Set.of("loaded.named"));
    moduleLayer = ModuleLayer.boot().defineModulesWithOneLoader(configuration, ProxyGeneratorTest.class.getClassLoader());
    speakerClass = moduleLayer.findLoader("loaded.named").loadClass("loaded.named.Speaker");
    phraseClass = moduleLayer.findLoader("loaded.named").loadClass("loaded.named.Phrase");

    Assert.assertTrue(speakerClass.getModule().isNamed());
    Assert.assertFalse(speakerClass.getModule().canRead(ProxyGenerator.class.getModule()));

    for (boolean defineInProxiedModule : new boolean[] {true, false}) {

      Object proxy = ProxyGenerator.createProxy(speakerClass, (proxyObject, method, args) -> "spoke " + args[0], defineInProxiedModule);

      Assert.assertEquals(speakerClass.getMethod("speak", phraseClass).invoke(proxy, phraseClass.getConstructor().newInstance()), "spoke phrase");
    }
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class)
  public void testNonPublicClassIsRejected () {

    ProxyGenerator.createProxy(PackagePrivateClass.class, new RecordingHandler("x"), true);
  }

  public void testStaticNestedTypesAreProxied () {

    for (boolean defineInProxiedModule : new boolean[] {true, false}) {
      Assert.assertEquals(ProxyGenerator.createProxy(PublicStaticNested.class, new RecordingHandler("routed"), defineInProxiedModule).name(), "routed");
      Assert.assertEquals(ProxyGenerator.createProxy(NestedSpeaker.class, new RecordingHandler("routed"), defineInProxiedModule).speak(), "routed");
    }
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class, expectedExceptionsMessageRegExp = ".*must be 'static'")
  public void testInnerClassIsRejected () {

    ProxyGenerator.createProxy(PublicInner.class, new RecordingHandler("x"), true);
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class, expectedExceptionsMessageRegExp = ".*must have a no-arg constructor")
  public void testClassWithoutNoArgConstructorIsRejected () {

    ProxyGenerator.createProxy(WithoutNoArgConstructor.class, new RecordingHandler("x"), true);
  }

  public void testPackagePrivateNoArgConstructorIsProxiedInProxiedModule () {

    Assert.assertEquals(ProxyGenerator.createProxy(PackagePrivateNoArgConstructor.class, new RecordingHandler("routed"), true).name(), "routed");
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class, expectedExceptionsMessageRegExp = ".*must be 'public' or 'protected' unless the proxy is defined in the proxied module")
  public void testPackagePrivateNoArgConstructorIsRejectedInProxyClassLoader () {

    ProxyGenerator.createProxy(PackagePrivateNoArgConstructor.class, new RecordingHandler("x"), false);
  }

  public void testInaccessibleSignatureTypesAreRejectedBeforeDefinition ()
    throws Exception {

    Path sourceDirectory = Files.createTempDirectory("proxy-generator-source");
    Path classDirectory = Files.createTempDirectory("proxy-generator-modules");
    Configuration configuration;
    ModuleLayer moduleLayer;
    Class<?> subClass;

    writeModuleSource(sourceDirectory, "loaded.word", "module loaded.word { exports loaded.word to loaded.base; }", "Word", "package loaded.word; public class Word { }");
    writeModuleSource(sourceDirectory, "loaded.base", "module loaded.base { requires loaded.word; exports loaded.base; }", "Base", "package loaded.base; public class Base { public loaded.word.Word word () { return null; } }");
    writeModuleSource(sourceDirectory, "loaded.user", "open module loaded.user { requires loaded.base; exports loaded.user; }", "Sub", "package loaded.user; public class Sub extends loaded.base.Base { }");

    Assert.assertEquals(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classDirectory.toString(), "--module-source-path", sourceDirectory.toString(), "--module", "loaded.word,loaded.base,loaded.user"), 0);

    configuration = ModuleLayer.boot().configuration().resolve(ModuleFinder.of(classDirectory), ModuleFinder.of(), Set.of("loaded.user"));
    moduleLayer = ModuleLayer.boot().defineModulesWithOneLoader(configuration, ProxyGeneratorTest.class.getClassLoader());
    subClass = moduleLayer.findLoader("loaded.user").loadClass("loaded.user.Sub");

    try {
      ProxyGenerator.createProxy(subClass, new RecordingHandler(null), true);
      Assert.fail("Expected the unreadable return type to be rejected");
    } catch (ByteCodeManipulationException byteCodeManipulationException) {
      Assert.assertTrue(byteCodeManipulationException.getMessage().contains("must read the module(loaded.word) of the type(loaded.word.Word)"), byteCodeManipulationException.getMessage());
    }

    try {
      ProxyGenerator.createProxy(subClass, new RecordingHandler(null), false);
      Assert.fail("Expected the qualified export of the return type to be rejected");
    } catch (ByteCodeManipulationException byteCodeManipulationException) {
      Assert.assertTrue(byteCodeManipulationException.getMessage().contains("must export the package(loaded.word) unconditionally"), byteCodeManipulationException.getMessage());
    }
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class, expectedExceptionsMessageRegExp = ".*must not be 'sealed'")
  public void testSealedInterfaceIsRejected () {

    ProxyGenerator.createProxy(SealedShape.class, new RecordingHandler("x"), true);
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class, expectedExceptionsMessageRegExp = ".*must not be 'final'")
  public void testFinalClassIsRejected () {

    ProxyGenerator.createProxy(ClosedShape.class, new RecordingHandler("x"), true);
  }

  static class PackagePrivateClass {

  }

  private static void writeModuleSource (Path sourceDirectory, String moduleName, String moduleDeclaration, String className, String classSource)
    throws IOException {

    Path moduleDirectory = Files.createDirectories(sourceDirectory.resolve(moduleName));

    Files.writeString(moduleDirectory.resolve("module-info.java"), moduleDeclaration);
    Files.writeString(Files.createDirectories(moduleDirectory.resolve(moduleName.replace('.', '/'))).resolve(className + ".java"), classSource);
  }

  private static byte[] createPublicClassBytes (String internalName) {

    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    MethodVisitor constructorVisitor;

    classWriter.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null, "java/lang/Object", null);
    constructorVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    constructorVisitor.visitCode();
    constructorVisitor.visitVarInsn(Opcodes.ALOAD, 0);
    constructorVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    constructorVisitor.visitInsn(Opcodes.RETURN);
    constructorVisitor.visitMaxs(1, 1);
    constructorVisitor.visitEnd();
    classWriter.visitEnd();

    return classWriter.toByteArray();
  }

  /**
   * Defines {@code loaded.split.Concrete} itself, and delegates only {@code loaded.split.Taker} to the loader of
   * the interface, so that {@code loaded.split.Token}, which appears in the signature of the method that
   * {@code Concrete} inherits from {@code Taker}, is visible to the interface's loader but not to this one.
   */
  private static class ConcreteOnlyClassLoader extends ClassLoader {

    private final Path classDirectory;
    private final ClassLoader interfaceLoader;

    private ConcreteOnlyClassLoader (Path classDirectory, ClassLoader interfaceLoader) {

      super(ClassLoader.getPlatformClassLoader());

      this.classDirectory = classDirectory;
      this.interfaceLoader = interfaceLoader;
    }

    @Override
    protected Class<?> loadClass (String name, boolean resolve)
      throws ClassNotFoundException {

      synchronized (getClassLoadingLock(name)) {

        Class<?> loadedClass;

        if ((loadedClass = findLoadedClass(name)) == null) {
          if ("loaded.split.Taker".equals(name)) {
            loadedClass = interfaceLoader.loadClass(name);
          } else if ("loaded.split.Concrete".equals(name)) {
            try {

              byte[] classBytes = Files.readAllBytes(classDirectory.resolve("loaded").resolve("split").resolve("Concrete.class"));

              loadedClass = defineClass(name, classBytes, 0, classBytes.length);
            } catch (IOException ioException) {
              throw new ClassNotFoundException(name, ioException);
            }
          } else {
            loadedClass = super.loadClass(name, false);
          }
        }

        if (resolve) {
          resolveClass(loadedClass);
        }

        return loadedClass;
      }
    }

    @Override
    protected URL findResource (String name) {

      if ("loaded/split/Concrete.class".equals(name)) {
        try {
          return classDirectory.resolve(name).toUri().toURL();
        } catch (IOException ioException) {
          throw new UncheckedIOException(ioException);
        }
      }

      return null;
    }
  }

  public interface NestedSpeaker {

    String speak ();
  }

  public static class PublicStaticNested {

    public String name () {

      return "nested";
    }
  }

  public static class WithoutNoArgConstructor {

    public WithoutNoArgConstructor (String value) {

    }
  }

  public static class PackagePrivateNoArgConstructor {

    PackagePrivateNoArgConstructor () {

    }

    public String name () {

      return "constructed";
    }
  }

  public class PublicInner {

  }

  private static class RecordingHandler implements InvocationHandler {

    private final List<Invocation> calls = new ArrayList<>();
    private final Object returnValue;

    RecordingHandler (Object returnValue) {

      this.returnValue = returnValue;
    }

    @Override
    public Object invoke (Object proxy, java.lang.reflect.Method method, Object[] args)
      throws Throwable {

      calls.add(new Invocation(method.getName(), args == null ? new Object[0] : Arrays.copyOf(args, args.length)));

      return returnValue;
    }
  }

  private static class Invocation {

    private final String methodName;
    private final Object[] args;

    Invocation (String methodName, Object[] args) {

      this.methodName = methodName;
      this.args = args;
    }
  }
}
