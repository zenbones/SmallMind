/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

public class ProxyGenerator {

  private static final HashMap<String, String> OBJECT_METHOD_MAP = new HashMap<>();
  private static final ConcurrentHashMap<Class<?>, Class<?>> INTERFACE_MAP = new ConcurrentHashMap<>();
  private static final HashMap<ClassLoader, ProxyClassLoader> LOADER_MAP = new HashMap<>();
  private static final String INVOCATION_HANDLER = "L" + InvocationHandler.class.getName().replace('.', '/') + ";";

  static {
    OBJECT_METHOD_MAP.put("hashCode", "()I");
    OBJECT_METHOD_MAP.put("equals", "(Ljava/lang/Object;)Z");
    OBJECT_METHOD_MAP.put("toString", "()Ljava/lang/String;");
  }

  public static <T> T createProxy (Class<T> toBeProxiedClass, InvocationHandler handler) {

    return createProxy(toBeProxiedClass, handler, null);
  }

  public static <T> T createProxy (Class<T> toBeProxiedClass, InvocationHandler handler, AnnotationFilter annotationFilter) {

    Class<?> extractedClass;

    if ((extractedClass = INTERFACE_MAP.get(toBeProxiedClass)) == null) {
      synchronized (INTERFACE_MAP) {
        if ((extractedClass = INTERFACE_MAP.get(toBeProxiedClass)) == null) {

          int toBeProxiedClassModifiers = toBeProxiedClass.getModifiers();

          if (!Modifier.isPublic(toBeProxiedClassModifiers)) {
            throw new ByteCodeManipulationException("The proxy class(%s) must be 'public'", toBeProxiedClass.getName());
          }
          if (Modifier.isStatic(toBeProxiedClassModifiers)) {
            throw new ByteCodeManipulationException("The proxy class(%s) must not be 'static'", toBeProxiedClass.getName());
          }
          if ((!toBeProxiedClass.isInterface()) && Modifier.isAbstract(toBeProxiedClassModifiers)) {
            throw new ByteCodeManipulationException("A concrete proxy class(%s) must not be 'abstract'", toBeProxiedClass.getName());
          } else {

            Class currentClass;
            ClassReader classReader;
            ClassWriter classWriter;
            CheckClassAdapter checkClassAdapter;
            ProxyClassVisitor proxyClassVisitor;
            ClassLoader toBeProxiedClassLoader;
            ProxyClassLoader proxyClassLoader;
            HashSet<MethodTracker> methodTrackerSet;
            boolean initialized = false;

            classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            checkClassAdapter = new CheckClassAdapter(classWriter, true);

            currentClass = toBeProxiedClass;
            methodTrackerSet = new HashSet<>();
            do {
              if (currentClass.equals(Object.class)) {
                currentClass = ObjectImpersonator.class;
              }

              try {
                classReader = new ClassReader(currentClass.getClassLoader().getResourceAsStream(currentClass.getCanonicalName().replace('.', '/') + ".class"));
              } catch (IOException ioException) {
                throw new ByteCodeManipulationException(ioException);
              }

              proxyClassVisitor = new ProxyClassVisitor(checkClassAdapter, toBeProxiedClass, currentClass, annotationFilter, methodTrackerSet, initialized);
              classReader.accept(proxyClassVisitor, 0);
              initialized = true;
            } while ((currentClass = currentClass.equals(ObjectImpersonator.class) ? null : currentClass.getSuperclass()) != null);

            checkClassAdapter.visitEnd();

            synchronized (LOADER_MAP) {
              if ((proxyClassLoader = LOADER_MAP.get(toBeProxiedClassLoader = toBeProxiedClass.getClassLoader())) == null) {
                LOADER_MAP.put(toBeProxiedClassLoader, proxyClassLoader = new ProxyClassLoader(toBeProxiedClassLoader));
              }
            }

            INTERFACE_MAP.put(toBeProxiedClass, extractedClass = proxyClassLoader.extractInterface(toBeProxiedClass.getName() + "$Proxy$_ExtractedSubclass", classWriter.toByteArray()));
          }
        }
      }
    }

    try {
      return toBeProxiedClass.cast(extractedClass.getConstructor(InvocationHandler.class).newInstance(handler));
    } catch (Exception exception) {
      throw new ByteCodeManipulationException(exception);
    }
  }

  private static class MethodTracker {

    private String name;
    private String description;

    private MethodTracker (String name, String description) {

      this.name = name;
      this.description = description;
    }

    public String getName () {

      return name;
    }

    public String getDescription () {

      return description;
    }

    @Override
    public int hashCode () {

      return name.hashCode() ^ ((description == null) ? 0 : description.hashCode());
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof MethodTracker) && ((MethodTracker)obj).getName().equals(name) && ((description == null) ? ((MethodTracker)obj).getDescription() == null : description.equals(((MethodTracker)obj).getDescription()));
    }
  }

  private static class ProxyClassLoader extends ClassLoader {

    public ProxyClassLoader (ClassLoader parent) {

      super(parent);
    }

    public Class extractInterface (String name, byte[] b) {

      return defineClass(name, b, 0, b.length);
    }
  }

  private static class ProxyClassVisitor extends ClassVisitor {

    private ClassVisitor nextClassVisitor;
    private Class toBeProxiedClass;
    private Class currentClass;
    private AnnotationFilter annotationFilter;
    private HashSet<MethodTracker> methodTrackerSet;
    private boolean constructed = false;
    private boolean initialized;

    public ProxyClassVisitor (ClassVisitor nextClassVisitor, Class toBeProxiedClass, Class currentClass, AnnotationFilter annotationFilter, HashSet<MethodTracker> methodTrackerSet, boolean initialized) {

      super(Opcodes.ASM5);

      this.nextClassVisitor = nextClassVisitor;
      this.toBeProxiedClass = toBeProxiedClass;
      this.currentClass = currentClass;
      this.annotationFilter = annotationFilter;
      this.methodTrackerSet = methodTrackerSet;
      this.initialized = initialized;
    }

    @Override
    public void visit (int version, int access, String name, String signature, String superName, String[] interfaces) {

      if (!initialized) {
        if (toBeProxiedClass.isInterface()) {
          nextClassVisitor.visit(version, Opcodes.ACC_PUBLIC, name + "$Proxy$_ExtractedSubclass", null, "java/lang/Object", new String[] {toBeProxiedClass.getName().replace('.', '/')});
        } else {
          nextClassVisitor.visit(version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name + "$Proxy$_ExtractedSubclass", null, name, null);
        }

        nextClassVisitor.visitField(Opcodes.ACC_PRIVATE, "$proxy$_handler", INVOCATION_HANDLER, null, null).visitEnd();
      }
    }

    private void createConstructor (String signature, String[] exceptions) {

      if (!(initialized || constructed)) {

        MethodVisitor initVisitor;

        initVisitor = nextClassVisitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + INVOCATION_HANDLER + ")V", signature, exceptions);

        initVisitor.visitCode();

        Label l0 = new Label();
        initVisitor.visitLabel(l0);
        initVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        initVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, (toBeProxiedClass.isInterface()) ? "java/lang/Object" : toBeProxiedClass.getName().replace('.', '/'), "<init>", "()V", false);
        Label l1 = new Label();
        initVisitor.visitLabel(l1);
        initVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        initVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        initVisitor.visitFieldInsn(Opcodes.PUTFIELD, toBeProxiedClass.getName().replace('.', '/') + "$Proxy$_ExtractedSubclass", "$proxy$_handler", INVOCATION_HANDLER);
        Label l2 = new Label();
        initVisitor.visitLabel(l2);
        initVisitor.visitInsn(Opcodes.RETURN);
        Label l3 = new Label();
        initVisitor.visitLabel(l3);
        initVisitor.visitLocalVariable("this", "L" + toBeProxiedClass.getName().replace('.', '/') + "$Proxy$_ExtractedSubclass;", null, l0, l3, 0);
        initVisitor.visitLocalVariable("$proxy$_handler", INVOCATION_HANDLER, null, l0, l3, 1);
        initVisitor.visitMaxs(2, 2);
        initVisitor.visitEnd();

        constructed = true;
      }
    }

    @Override
    public MethodVisitor visitMethod (int access, String name, String desc, String signature, String[] exceptions) {

      MethodTracker methodTracker;

      if (!methodTrackerSet.contains(methodTracker = new MethodTracker(name, desc))) {
        if (toBeProxiedClass.isInterface() || ((access & Opcodes.ACC_ABSTRACT) == 0)) {
          if ("<init>".equals(name)) {
            if ("()V".equals(desc)) {
              methodTrackerSet.add(methodTracker);
              createConstructor(signature, exceptions);
            }
          } else {

            String objectMethodDesc;

            if ((!currentClass.equals(ObjectImpersonator.class)) || (((objectMethodDesc = OBJECT_METHOD_MAP.get(name)) != null) && objectMethodDesc.equals(desc))) {
              if (((access & Opcodes.ACC_PUBLIC) != 0) && ((access & Opcodes.ACC_STATIC) == 0) && ((access & Opcodes.ACC_FINAL) == 0) && ((access & Opcodes.ACC_SYNTHETIC) == 0)) {

                MethodVisitor proxyVisitor;

                methodTrackerSet.add(methodTracker);
                proxyVisitor = nextClassVisitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, name, desc, null, exceptions);

                proxyVisitor.visitCode();
                Label l0 = new Label();
                Label l1 = new Label();

                Label[] exceptionLabels;

                exceptionLabels = new Label[(exceptions == null) ? 1 : exceptions.length + 1];

                for (int index = 0; index < ((exceptions == null) ? 0 : exceptions.length); index++) {
                  exceptionLabels[index] = new Label();
                  proxyVisitor.visitTryCatchBlock(l0, l1, exceptionLabels[index], exceptions[index]);
                }

                exceptionLabels[(exceptions == null) ? 0 : exceptions.length] = new Label();
                proxyVisitor.visitTryCatchBlock(l0, l1, exceptionLabels[(exceptions == null) ? 0 : exceptions.length], "java/lang/Throwable");

                proxyVisitor.visitLabel(l0);
                proxyVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                proxyVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                proxyVisitor.visitFieldInsn(Opcodes.GETFIELD, toBeProxiedClass.getName().replace('.', '/') + "$Proxy$_ExtractedSubclass", "$proxy$_handler", INVOCATION_HANDLER);

                proxyVisitor.visitInsn(toBeProxiedClass.isInterface() ? Opcodes.ICONST_0 : Opcodes.ICONST_1);
                proxyVisitor.visitLdcInsn(UUID.randomUUID().toString());
                proxyVisitor.visitLdcInsn(name);
                proxyVisitor.visitLdcInsn(desc.substring(desc.indexOf(')') + 1));

                String[] parameters;
                LinkedList<String> parameterList;

                parameterList = new LinkedList<>();
                for (String parameter : new ParameterIterable(desc.substring(1, desc.indexOf(')')))) {
                  parameterList.add(parameter);
                }
                parameters = new String[parameterList.size()];
                parameterList.toArray(parameters);

                insertNumber(proxyVisitor, parameterList.size());
                proxyVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
                for (int index = 0; index < parameters.length; index++) {
                  proxyVisitor.visitInsn(Opcodes.DUP);
                  insertNumber(proxyVisitor, index);
                  proxyVisitor.visitLdcInsn(parameters[index]);
                  proxyVisitor.visitInsn(Opcodes.AASTORE);
                }

                int[] parameterRegisters;
                int variableIndex = 1;

                insertNumber(proxyVisitor, parameterList.size());
                proxyVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

                parameterRegisters = new int[parameters.length];
                for (int index = 0; index < parameters.length; index++) {
                  proxyVisitor.visitInsn(Opcodes.DUP);
                  insertNumber(proxyVisitor, index);

                  if (parameters[index].length() == 1) {
                    switch (parameters[index].charAt(0)) {
                      case 'Z':
                        proxyVisitor.visitVarInsn(Opcodes.ILOAD, parameterRegisters[index] = variableIndex++);
                        proxyVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                        break;
                      case 'B':
                        proxyVisitor.visitVarInsn(Opcodes.ILOAD, parameterRegisters[index] = variableIndex++);
                        proxyVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                        break;
                      case 'C':
                        proxyVisitor.visitVarInsn(Opcodes.ILOAD, parameterRegisters[index] = variableIndex++);
                        proxyVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                        break;
                      case 'S':
                        proxyVisitor.visitVarInsn(Opcodes.ILOAD, parameterRegisters[index] = variableIndex++);
                        proxyVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                        break;
                      case 'I':
                        proxyVisitor.visitVarInsn(Opcodes.ILOAD, parameterRegisters[index] = variableIndex++);
                        proxyVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                        break;
                      case 'J':
                        proxyVisitor.visitVarInsn(Opcodes.LLOAD, parameterRegisters[index] = variableIndex);
                        variableIndex += 2;
                        proxyVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                        break;
                      case 'F':
                        proxyVisitor.visitVarInsn(Opcodes.FLOAD, parameterRegisters[index] = variableIndex++);
                        proxyVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                        break;
                      case 'D':
                        proxyVisitor.visitVarInsn(Opcodes.DLOAD, parameterRegisters[index] = variableIndex);
                        variableIndex += 2;
                        proxyVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                        break;
                      default:
                        throw new ByteCodeManipulationException("Unknown primitive type(%s)", parameters[index]);
                    }
                  } else {
                    proxyVisitor.visitVarInsn(Opcodes.ALOAD, parameterRegisters[index] = variableIndex++);
                  }

                  proxyVisitor.visitInsn(Opcodes.AASTORE);
                }

                proxyVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, ProxyUtility.class.getName().replace('.', '/'), "invoke", "(Ljava/lang/Object;" + INVOCATION_HANDLER + "ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false);

                String returnType;

                if ((returnType = desc.substring(desc.indexOf(')') + 1)).length() == 1) {
                  switch (returnType.charAt(0)) {
                    case 'V':
                      proxyVisitor.visitInsn(Opcodes.POP);
                      break;
                    case 'Z':
                      proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                      proxyVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                      break;
                    case 'B':
                      proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
                      proxyVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                      break;
                    case 'C':
                      proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
                      proxyVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                      break;
                    case 'S':
                      proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
                      proxyVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                      break;
                    case 'I':
                      proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                      proxyVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                      break;
                    case 'J':
                      proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
                      proxyVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                      break;
                    case 'F':
                      proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
                      proxyVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                      break;
                    case 'D':
                      proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
                      proxyVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                      break;
                    default:
                      throw new ByteCodeManipulationException("Unknown return type(%s)", returnType);
                  }
                } else if (returnType.startsWith("L")) {
                  proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, returnType.substring(1, returnType.length() - 1));
                } else {
                  proxyVisitor.visitTypeInsn(Opcodes.CHECKCAST, returnType);
                }

                proxyVisitor.visitLabel(l1);

                Label lEnd = null;

                if ((returnType = desc.substring(desc.indexOf(')') + 1)).length() == 1) {
                  switch (returnType.charAt(0)) {
                    case 'V':
                      lEnd = new Label();
                      proxyVisitor.visitJumpInsn(Opcodes.GOTO, lEnd);
                      break;
                    case 'Z':
                      proxyVisitor.visitInsn(Opcodes.IRETURN);
                      break;
                    case 'B':
                      proxyVisitor.visitInsn(Opcodes.IRETURN);
                      break;
                    case 'C':
                      proxyVisitor.visitInsn(Opcodes.IRETURN);
                      break;
                    case 'S':
                      proxyVisitor.visitInsn(Opcodes.IRETURN);
                      break;
                    case 'I':
                      proxyVisitor.visitInsn(Opcodes.IRETURN);
                      break;
                    case 'J':
                      proxyVisitor.visitInsn(Opcodes.LRETURN);
                      break;
                    case 'F':
                      proxyVisitor.visitInsn(Opcodes.FRETURN);
                      break;
                    case 'D':
                      proxyVisitor.visitInsn(Opcodes.DRETURN);
                      break;
                    default:
                      throw new ByteCodeManipulationException("Unknown return type(%s)", returnType);
                  }
                } else {
                  proxyVisitor.visitInsn(Opcodes.ARETURN);
                }

                Label[] extraLabels;

                extraLabels = new Label[(exceptions == null) ? 1 : exceptions.length + 1];
                for (int index = 0; index <= ((exceptions == null) ? 0 : exceptions.length); index++) {
                  proxyVisitor.visitLabel(exceptionLabels[index]);
                  proxyVisitor.visitVarInsn(Opcodes.ASTORE, variableIndex);
                  proxyVisitor.visitLabel(extraLabels[index] = new Label());

                  if (index == ((exceptions == null) ? 0 : exceptions.length)) {
                    proxyVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/reflect/UndeclaredThrowableException");
                    proxyVisitor.visitInsn(Opcodes.DUP);
                    proxyVisitor.visitVarInsn(Opcodes.ALOAD, variableIndex);
                    proxyVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/reflect/UndeclaredThrowableException", "<init>", "(Ljava/lang/Throwable;)V", false);
                    proxyVisitor.visitInsn(Opcodes.ATHROW);
                  } else {
                    proxyVisitor.visitVarInsn(Opcodes.ALOAD, variableIndex);
                    proxyVisitor.visitInsn(Opcodes.ATHROW);
                  }
                }

                if (lEnd != null) {
                  proxyVisitor.visitLabel(lEnd);
                  proxyVisitor.visitInsn(Opcodes.RETURN);
                }

                Label lLocal;

                proxyVisitor.visitLabel(lLocal = new Label());
                proxyVisitor.visitLocalVariable("this", "L" + toBeProxiedClass.getName().replace('.', '/') + "$Proxy$_ExtractedSubclass;", null, l0, lLocal, 0);
                for (int index = 0; index < parameters.length; index++) {
                  proxyVisitor.visitLocalVariable("$proxy$_var" + index, parameters[index], null, l0, lLocal, parameterRegisters[index]);
                }
                for (int index = 0; index < ((exceptions == null) ? 0 : exceptions.length); index++) {
                  proxyVisitor.visitLocalVariable("$proxy$_exc" + index, "L" + exceptions[index] + ";", null, extraLabels[index], exceptionLabels[index + 1], variableIndex);
                }
                proxyVisitor.visitLocalVariable("$proxy$_exc" + ((exceptions == null) ? 0 : exceptions.length), "Ljava/lang/Throwable;", null, extraLabels[(exceptions == null) ? 0 : exceptions.length], lLocal, variableIndex);

                proxyVisitor.visitMaxs(12, variableIndex + 2);

                return new ProxyMethodVisitor(proxyVisitor, annotationFilter);
              }
            }
          }
        }
      }

      return null;
    }

    @Override
    public void visitEnd () {

      createConstructor(null, null);
    }

    private void insertNumber (MethodVisitor methodVisitor, int number) {

      switch (number) {
        case 0:
          methodVisitor.visitInsn(Opcodes.ICONST_0);
          break;
        case 1:
          methodVisitor.visitInsn(Opcodes.ICONST_1);
          break;
        case 2:
          methodVisitor.visitInsn(Opcodes.ICONST_2);
          break;
        case 3:
          methodVisitor.visitInsn(Opcodes.ICONST_3);
          break;
        case 4:
          methodVisitor.visitInsn(Opcodes.ICONST_4);
          break;
        case 5:
          methodVisitor.visitInsn(Opcodes.ICONST_5);
          break;
        default:
          if (number <= Byte.MAX_VALUE) {
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, number);
          } else {
            methodVisitor.visitIntInsn(Opcodes.SIPUSH, number);
          }
          break;
      }
    }
  }

  private static class ProxyMethodVisitor extends MethodVisitor {

    private MethodVisitor nextMethodVisitor;
    private AnnotationFilter annotationFilter;

    public ProxyMethodVisitor (MethodVisitor nextMethodVisitor, AnnotationFilter annotationFilter) {

      super(Opcodes.ASM5);

      this.nextMethodVisitor = nextMethodVisitor;
      this.annotationFilter = annotationFilter;
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault () {

      return nextMethodVisitor.visitAnnotationDefault();
    }

    @Override
    public AnnotationVisitor visitAnnotation (String desc, boolean visible) {

      if ((annotationFilter == null) || annotationFilter.isAllowed(desc)) {

        return nextMethodVisitor.visitAnnotation(desc, visible);
      }

      return null;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation (int parameter, String desc, boolean visible) {

      if ((annotationFilter == null) || annotationFilter.isAllowed(desc)) {

        return nextMethodVisitor.visitParameterAnnotation(parameter, desc, visible);
      }

      return null;
    }

    @Override
    public void visitEnd () {

      nextMethodVisitor.visitEnd();
    }
  }
}