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
package org.smallmind.spark.singularity.maven;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Shared helpers for driving {@link GenerateSingularityMojo} from tests: the Mojo exposes no setters (its parameters
 * are normally injected by Maven), so fields are populated reflectively, and compiled fixture classes are pulled off
 * the test classpath to assemble a stand-in project and dependency jars.
 */
final class MojoTestSupport {

  private MojoTestSupport () {

  }

  static void setField (Object target, String name, Object value)
    throws Exception {

    Field field = target.getClass().getDeclaredField(name);

    field.setAccessible(true);
    field.set(target, value);
  }

  static String resourcePath (String binaryName) {

    return binaryName.replace('.', '/') + ".class";
  }

  static byte[] classBytes (String binaryName)
    throws Exception {

    try (InputStream inputStream = MojoTestSupport.class.getResourceAsStream("/" + resourcePath(binaryName))) {
      if (inputStream == null) {
        throw new IllegalStateException("fixture not present on the test classpath: " + binaryName);
      }

      return inputStream.readAllBytes();
    }
  }

  static void writeClassInto (Path classesDirectory, String binaryName)
    throws Exception {

    Path destination = classesDirectory.resolve(resourcePath(binaryName));

    Files.createDirectories(destination.getParent());
    Files.write(destination, classBytes(binaryName));
  }

  static void buildJar (Path jarPath, Map<String, byte[]> entries)
    throws Exception {

    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
        jarOutputStream.write(entry.getValue());
        jarOutputStream.closeEntry();
      }
    }
  }

  // The boot dependency resolves to a jar when pulled from the local repository, but to a classes directory when the
  // boot module is part of the same reactor build; GenerateSingularityMojo can only read it as a jar. When handed a
  // directory, stage it into a jar using STORED entries so that JarInputStream.getSize() (which the Mojo relies on)
  // is populated.
  static Path bootClassesAsJar (Path bootLocation, Path stagedJar)
    throws Exception {

    if (Files.isRegularFile(bootLocation)) {

      return bootLocation;
    }

    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(stagedJar))) {
      try (Stream<Path> walk = Files.walk(bootLocation)) {

        List<Path> files = walk.filter(Files::isRegularFile).toList();

        for (Path file : files) {

          byte[] data = Files.readAllBytes(file);
          JarEntry jarEntry = new JarEntry(bootLocation.relativize(file).toString().replace(File.separatorChar, '/'));
          CRC32 crc32 = new CRC32();

          crc32.update(data);
          jarEntry.setMethod(JarEntry.STORED);
          jarEntry.setSize(data.length);
          jarEntry.setCompressedSize(data.length);
          jarEntry.setCrc(crc32.getValue());

          jarOutputStream.putNextEntry(jarEntry);
          jarOutputStream.write(data);
          jarOutputStream.closeEntry();
        }
      }
    }

    return stagedJar;
  }

  static void deleteTree (Path root) {

    if ((root != null) && Files.exists(root)) {
      try (Stream<Path> walk = Files.walk(root)) {
        walk.sorted(Comparator.reverseOrder()).forEach((path) -> {
          try {
            Files.deleteIfExists(path);
          } catch (Exception exception) {
            // best effort: a child process may still hold the bundle open briefly on Windows
          }
        });
      } catch (Exception exception) {
        // best effort
      }
    }
  }
}
