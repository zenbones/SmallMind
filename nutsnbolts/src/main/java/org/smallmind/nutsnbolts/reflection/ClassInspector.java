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
import java.io.PrintWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * Development utility that prints byte code information for a class to standard output using
 * ASM's tracing and ASMifier facilities.
 */
public class ClassInspector {

  /**
   * Emits a human-readable byte code trace of the given class to {@link System#out}.
   *
   * @param parseClass the class whose byte code should be traced
   * @throws ByteCodeManipulationException if the class resource stream cannot be opened
   */
  public static void trace (Class parseClass) {

    ClassReader classReader;
    TraceClassVisitor classVisitor;

    try {
      classReader = new ClassReader(parseClass.getClassLoader().getResourceAsStream(parseClass.getCanonicalName().replace('.', '/') + ".class"));
    } catch (IOException ioException) {
      throw new ByteCodeManipulationException(ioException);
    }

    classVisitor = new TraceClassVisitor(new PrintWriter(System.out));
    classReader.accept(classVisitor, 0);
  }

  /**
   * Generates an ASMifier script that reproduces the byte code of the given class and writes it to {@link System#out}.
   *
   * @param parseClass the class whose byte code should be expressed as an ASMifier program
   * @throws ByteCodeManipulationException if the class resource stream cannot be opened
   */
  public static void asmify (Class parseClass) {

    ClassReader classReader;
    TraceClassVisitor classVisitor;

    try {
      classReader = new ClassReader(parseClass.getClassLoader().getResourceAsStream(parseClass.getCanonicalName().replace('.', '/') + ".class"));
    } catch (IOException ioException) {
      throw new ByteCodeManipulationException(ioException);
    }

    classVisitor = new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out));
    classReader.accept(classVisitor, 0);
  }
}
