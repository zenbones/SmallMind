/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import org.objectweb.asm.util.ASMifierClassVisitor;
import org.objectweb.asm.util.TraceClassVisitor;

public class ClassInspector {

   public static void trace (Class parseClass) {

      ClassReader classReader;
      TraceClassVisitor classVisitor;

      classVisitor = new TraceClassVisitor(new PrintWriter(System.out));

      try {
         classReader = new ClassReader(parseClass.getClassLoader().getResourceAsStream(parseClass.getCanonicalName().replace('.', '/') + ".class"));
      }
      catch (IOException ioException) {
         throw new ByteCodeManipulationException(ioException);
      }

      classReader.accept(classVisitor, 0);
   }

   public static void asm (Class parseClass) {

      ClassReader classReader;
      ASMifierClassVisitor classVisitor;

      classVisitor = new ASMifierClassVisitor(new PrintWriter(System.out));

      try {
         classReader = new ClassReader(parseClass.getClassLoader().getResourceAsStream(parseClass.getCanonicalName().replace('.', '/') + ".class"));
      }
      catch (IOException ioException) {
         throw new ByteCodeManipulationException(ioException);
      }

      classReader.accept(classVisitor, 0);
   }
}
