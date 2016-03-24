/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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

public class SingularityEntryPoint {

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
