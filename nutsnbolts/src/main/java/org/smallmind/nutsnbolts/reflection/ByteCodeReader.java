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
import java.io.InputStream;
import org.objectweb.asm.ClassReader;

/**
 * Reads the class file of a loaded class so that ASM can parse it. The class file is located through the
 * class's {@link Module}, which asks the class's own loader for it without delegating to a parent, so a class
 * of the same name visible through a parent loader is never read in its place.
 */
final class ByteCodeReader {

  private ByteCodeReader () {

  }

  /**
   * Creates an ASM {@link ClassReader} over the class file of the given class.
   *
   * @param clazz the class whose byte code should be read
   * @return a reader positioned at the start of the class file
   * @throws ByteCodeManipulationException if the class loader does not provide the class file as a resource, as
   *                                       with classes defined from bytes held only in memory, if the class file
   *                                       found describes a different class, or if it cannot be read or parsed
   */
  static ClassReader createClassReader (Class<?> clazz) {

    ClassReader classReader;
    String internalName = clazz.getName().replace('.', '/');

    try (InputStream classStream = clazz.getModule().getResourceAsStream(internalName + ".class")) {
      if (classStream == null) {
        throw new ByteCodeManipulationException("Unable to locate the byte code of class(%s), because its class loader does not provide it as a resource", clazz.getName());
      }

      classReader = new ClassReader(classStream);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ByteCodeManipulationException(exception, "Unable to read the byte code of class(%s)", clazz.getName());
    }

    if (!internalName.equals(classReader.getClassName())) {
      throw new ByteCodeManipulationException("The byte code located for class(%s) describes the class(%s)", clazz.getName(), classReader.getClassName().replace('/', '.'));
    }

    return classReader;
  }
}
