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
package org.smallmind.spark.singularity.boot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Bootstrap class declared as the jar's {@code Main-Class}. When executed it installs a {@link SingularityClassLoader}
 * as the context class loader and then reflectively dispatches to the real application main, whose fully qualified
 * name is carried in the manifest's {@code Singularity-Class} attribute.
 */
public class SingularityEntryPoint {

  /**
   * Bootstraps the Singularity runtime and relinquishes control to the user-supplied main class.
   *
   * <p>The jar hosting this class is opened via its {@link CodeSource}, the {@link SingularityClassLoader} is built
   * from its manifest and index, and the class named by the manifest's {@code Singularity-Class} attribute has its
   * {@code main(String[])} method invoked. A reference to this entry point itself is silently ignored so that the
   * bootstrap never recurses.
   *
   * @param args command-line arguments to forward unchanged to the application's main method
   * @throws IOException               if the enclosing jar cannot be opened or its manifest read
   * @throws ClassNotFoundException    if the class named by {@code Singularity-Class} cannot be resolved
   * @throws NoSuchMethodException     if that class does not expose a {@code public static void main(String[])}
   * @throws IllegalAccessException    if the main method cannot be invoked due to access restrictions
   * @throws InvocationTargetException if the invoked main method throws; the underlying cause is the application's exception
   */
  public static void main (String... args)
    throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

    ProtectionDomain protectionDomain = SingularityEntryPoint.class.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    Manifest manifest;
    String mainClass;

    try (JarInputStream jarInputStream = new JarInputStream(codeSource.getLocation().openStream())) {
      manifest = jarInputStream.getManifest();
      Thread.currentThread().setContextClassLoader(new SingularityClassLoader(null, manifest, codeSource.getLocation(), jarInputStream));
    }

    if ((mainClass = manifest.getMainAttributes().getValue(new Attributes.Name("Singularity-Class"))) != null) {
      if (!mainClass.equals(SingularityEntryPoint.class.getName())) {

        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(mainClass);
        Method main = clazz.getMethod("main", String[].class);

        main.invoke(null, new Object[] {args});
      }
    }
  }
}
