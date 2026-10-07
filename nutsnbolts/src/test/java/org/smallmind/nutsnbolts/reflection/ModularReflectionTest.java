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

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.tools.ToolProvider;
import org.objectweb.asm.ClassReader;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Runs the reflection utilities from a named {@code org.smallmind.nutsnbolts} module, which the rest of the test
 * suite cannot do because it runs on the classpath. Each test compiles small modules, and resolves them in a new
 * {@link ModuleLayer} together with a second copy of {@code org.smallmind.nutsnbolts}, read from the compiled
 * main classes, and {@code org.objectweb.asm}. The layer's loader delegates to the platform class loader, so
 * nothing in the layer is shared with the classpath copy except the JDK. Calls into the layer go through
 * reflection, and the test class itself, in the unnamed module, plays the part of an unrelated caller.
 */
@Test(groups = "unit")
public class ModularReflectionTest {

  public void testProxyIsDefinedInModuleThatOpensItsPackageOnlyToThisModule ()
    throws Exception {

    Path sourceDirectory = Files.createTempDirectory("modular-reflection-source");
    ModuleLayer moduleLayer;
    Class<?> speakerClass;
    Object proxy;

    writeModuleSource(sourceDirectory, "loaded.qualified", "module loaded.qualified { exports loaded.qualified; opens loaded.qualified to org.smallmind.nutsnbolts; }", "Speaker", "package loaded.qualified; public interface Speaker { String speak (); }");
    moduleLayer = createModuleLayer(sourceDirectory, "loaded.qualified");
    speakerClass = moduleLayer.findLoader("loaded.qualified").loadClass("loaded.qualified.Speaker");

    proxy = createProxy(moduleLayer, speakerClass, (proxyObject, method, args) -> "spoken", true);

    Assert.assertTrue(proxy.getClass().getModule().isNamed());
    Assert.assertSame(proxy.getClass().getModule(), speakerClass.getModule());
    Assert.assertEquals(speakerClass.getMethod("speak").invoke(proxy), "spoken");
  }

  public void testProxyInModuleThatDoesNotOpenItsPackageIsRejected ()
    throws Exception {

    Path sourceDirectory = Files.createTempDirectory("modular-reflection-source");
    ModuleLayer moduleLayer;
    Class<?> speakerClass;

    writeModuleSource(sourceDirectory, "loaded.unopened", "module loaded.unopened { exports loaded.unopened; }", "Speaker", "package loaded.unopened; public interface Speaker { String speak (); }");
    moduleLayer = createModuleLayer(sourceDirectory, "loaded.unopened");
    speakerClass = moduleLayer.findLoader("loaded.unopened").loadClass("loaded.unopened.Speaker");

    try {
      createProxy(moduleLayer, speakerClass, (proxyObject, method, args) -> "spoken", true);
      Assert.fail("Expected the unopened package to be rejected");
    } catch (InvocationTargetException invocationTargetException) {
      Assert.assertEquals(invocationTargetException.getCause().getClass().getName(), ByteCodeManipulationException.class.getName());
      Assert.assertEquals(invocationTargetException.getCause().getMessage(), "The module(loaded.unopened) of the proxy class(loaded.unopened.Speaker) must open the package(loaded.unopened) to the module(org.smallmind.nutsnbolts)");
    }
  }

  public void testOpeningToThisModuleDoesNotGrantFieldAccessToOtherCallers ()
    throws Exception {

    Path sourceDirectory = Files.createTempDirectory("modular-reflection-source");
    ModuleLayer moduleLayer;
    Class<?> secretClass;

    writeModuleSource(sourceDirectory, "loaded.secret", "module loaded.secret { exports loaded.secret; opens loaded.secret to org.smallmind.nutsnbolts; }", "Secret", "package loaded.secret; public class Secret { private String password = \"hunter2\"; }");
    moduleLayer = createModuleLayer(sourceDirectory, "loaded.secret");
    secretClass = moduleLayer.findLoader("loaded.secret").loadClass("loaded.secret.Secret");

    try {
      moduleLayer.findLoader("org.smallmind.nutsnbolts").loadClass(FieldUtility.class.getName()).getMethod("getFieldAccessors", MethodHandles.Lookup.class, Class.class).invoke(null, MethodHandles.lookup(), secretClass);
      Assert.fail("Expected a caller without access to the package to be refused");
    } catch (InvocationTargetException invocationTargetException) {
      Assert.assertTrue(invocationTargetException.getCause() instanceof IllegalAccessException);
      Assert.assertTrue(invocationTargetException.getCause().getMessage().contains("does not open loaded.secret to unnamed module"), invocationTargetException.getCause().getMessage());
    }
  }

  public void testFrameworkModuleReachesFieldsOpenedToItWithoutReadingTheirModule ()
    throws Exception {

    Path sourceDirectory = Files.createTempDirectory("modular-reflection-source");
    ModuleLayer moduleLayer;
    Class<?> readerClass;
    Class<?> modelClass;

    writeModuleSource(sourceDirectory, "loaded.framework", "module loaded.framework { requires org.smallmind.nutsnbolts; exports loaded.framework; }", "Reader", "package loaded.framework; import java.lang.invoke.MethodHandles; import org.smallmind.nutsnbolts.reflection.FieldUtility; public class Reader { public static Object read (Object target, String name) throws Exception { return FieldUtility.getFieldAccessor(MethodHandles.lookup(), target.getClass(), name).get(target); } }");
    writeModuleSource(sourceDirectory, "loaded.model", "module loaded.model { exports loaded.model; opens loaded.model to loaded.framework; }", "Model", "package loaded.model; public class Model { private String value = \"modeled\"; }");
    moduleLayer = createModuleLayer(sourceDirectory, "loaded.framework", "loaded.model");
    readerClass = moduleLayer.findLoader("loaded.framework").loadClass("loaded.framework.Reader");
    modelClass = moduleLayer.findLoader("loaded.model").loadClass("loaded.model.Model");

    Assert.assertFalse(readerClass.getModule().canRead(modelClass.getModule()));
    Assert.assertEquals(readerClass.getMethod("read", Object.class, String.class).invoke(null, modelClass.getConstructor().newInstance(), "value"), "modeled");
    Assert.assertTrue(readerClass.getModule().canRead(modelClass.getModule()));

    // The accessors now cached for loaded.framework must not be handed to a caller in another module
    try {
      moduleLayer.findLoader("org.smallmind.nutsnbolts").loadClass(FieldUtility.class.getName()).getMethod("getFieldAccessors", MethodHandles.Lookup.class, Class.class).invoke(null, MethodHandles.lookup(), modelClass);
      Assert.fail("Expected a caller in another module to be refused");
    } catch (InvocationTargetException invocationTargetException) {
      Assert.assertTrue(invocationTargetException.getCause() instanceof IllegalAccessException);
      Assert.assertTrue(invocationTargetException.getCause().getMessage().contains("does not open loaded.model to unnamed module"), invocationTargetException.getCause().getMessage());
    }
  }

  private static Object createProxy (ModuleLayer moduleLayer, Class<?> toBeProxiedClass, InvocationHandler handler, boolean defineInProxiedModule)
    throws Exception {

    Method createProxyMethod = moduleLayer.findLoader("org.smallmind.nutsnbolts").loadClass(ProxyGenerator.class.getName()).getMethod("createProxy", Class.class, InvocationHandler.class, boolean.class);

    return createProxyMethod.invoke(null, toBeProxiedClass, handler, defineInProxiedModule);
  }

  private static ModuleLayer createModuleLayer (Path sourceDirectory, String... moduleNames)
    throws Exception {

    Path nutsnboltsDirectory = Path.of(ProxyGenerator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path asmJar = Path.of(ClassReader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path classDirectory = Files.createTempDirectory("modular-reflection-modules");
    Set<String> rootModuleNames = new HashSet<>(Set.of(moduleNames));
    Configuration configuration;

    if (!Files.isRegularFile(nutsnboltsDirectory.resolve("module-info.class"))) {
      throw new SkipException("The main classes of org.smallmind.nutsnbolts are not an exploded module at " + nutsnboltsDirectory);
    }

    Assert.assertEquals(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classDirectory.toString(), "--module-path", nutsnboltsDirectory.toString(), "--module-source-path", sourceDirectory.toString(), "--module", String.join(",", moduleNames)), 0);

    rootModuleNames.add("org.smallmind.nutsnbolts");
    rootModuleNames.add("org.objectweb.asm");
    configuration = ModuleLayer.boot().configuration().resolve(ModuleFinder.of(classDirectory, nutsnboltsDirectory, asmJar), ModuleFinder.of(), rootModuleNames);

    return ModuleLayer.boot().defineModulesWithOneLoader(configuration, ClassLoader.getPlatformClassLoader());
  }

  private static void writeModuleSource (Path sourceDirectory, String moduleName, String moduleDeclaration, String className, String classSource)
    throws IOException {

    Path moduleDirectory = Files.createDirectories(sourceDirectory.resolve(moduleName));

    Files.writeString(moduleDirectory.resolve("module-info.java"), moduleDeclaration);
    Files.writeString(Files.createDirectories(moduleDirectory.resolve(moduleName.replace('.', File.separatorChar))).resolve(className + ".java"), classSource);
  }
}
