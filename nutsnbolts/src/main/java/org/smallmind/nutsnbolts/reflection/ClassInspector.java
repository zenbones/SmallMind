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
