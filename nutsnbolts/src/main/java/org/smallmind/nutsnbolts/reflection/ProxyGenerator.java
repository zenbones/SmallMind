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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Uses ASM to dynamically generate a subclass or interface implementation that routes every public
 * non-final method call through a supplied {@link InvocationHandler}, with optional annotation filtering.
 * The routed methods are those the proxied type declares or inherits, including the methods of its
 * superinterfaces, the default methods of the interfaces a proxied class implements, and {@code hashCode},
 * {@code equals} and {@code toString}. The handler receives the {@link java.lang.reflect.Method} that
 * {@link Class#getMethod(String, Class[])} returns for the proxied type, or for {@link Object} in the case of
 * those three methods on an interface proxy. Unchecked exceptions thrown by the handler, and checked exceptions
 * the method declares, propagate unchanged; any other checked exception is wrapped in an
 * {@link java.lang.reflect.UndeclaredThrowableException}. A generated class refers only to {@code java.base},
 * the proxied type, and the types in the proxied method signatures. Generation needs only the
 * {@code org.objectweb.asm} module.
 * <p>
 * Each generated method carries copies of the method and parameter annotations of one declaration of the method
 * it proxies, less any that the annotation filter excludes. That declaration is the first one found by searching
 * the proxied class, then its superclasses, then its interfaces, so an overriding method's annotations replace
 * those of the method it overrides. Declarations are read from the byte code of the types that hold them, except
 * in types defined by the bootstrap or platform class loader, which belong to the JDK. Those are read through
 * reflection, so a JDK newer than the ASM release can still be proxied, and their annotations are not copied: a
 * generated method whose declaration comes from a JDK type carries no annotations, whatever the filter.
 * Annotations on the proxied type itself are never copied.
 * <p>
 * Where the generated class is defined is chosen per call by the {@code defineInProxiedModule} flag:
 * <ul>
 *   <li>{@code true} defines it in the class loader, runtime package, and module of the proxied class.
 *   When the proxied class is in a named module, that module must open the proxied class's package to
 *   {@code org.smallmind.nutsnbolts}. For every type the proxied methods return or declare as thrown that lies
 *   outside the proxied class's runtime package, the proxied class's module must also read the type's module, and
 *   the type's package must be exported to it.</li>
 *   <li>{@code false} defines it in the unnamed module of a separate class loader whose parent is the
 *   proxied class's loader, in a package of its own under {@code org.smallmind.nutsnbolts.reflection.proxy}.
 *   When the proxied class is in a named module, that module must export (or open) the proxied class's
 *   package, and the packages of every type the proxied methods return or declare as thrown, unconditionally.</li>
 * </ul>
 * In either mode, every type in the proxied method signatures must be loadable through the proxied class's
 * class loader. These requirements are checked before the generated class is defined.
 * <p>
 * Generated classes are cached per proxied class and annotation filter, separately for each mode. The cache does
 * not keep the proxied class, or its class loader, reachable. Nor does it keep this module's class loader
 * reachable through a proxied class that outlives it, such as a JDK interface proxied from a discarded
 * {@link ModuleLayer}, except when a {@code false} proxy is generated for a class whose loader is neither an
 * ancestor nor a descendant of this module's loader; that class then keeps this module's loader reachable.
 */
public class ProxyGenerator {

  private static final HashMap<String, String> OBJECT_METHOD_MAP = new HashMap<>();
  private static final ClassValue<ConcurrentHashMap<String, Class<?>>> PROXY_CLASS_MAP_VALUE = new ClassValue<>() {

    @Override
    protected ConcurrentHashMap<String, Class<?>> computeValue (Class<?> type) {

      return new ConcurrentHashMap<>();
    }
  };
  private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Class<?>>> ANCESTOR_PROXY_CLASS_MAP = new ConcurrentHashMap<>();
  private static final AtomicInteger PROXY_CLASS_COUNTER = new AtomicInteger();
  private static final String INVOCATION_HANDLER = "L" + InvocationHandler.class.getName().replace('.', '/') + ";";
  private static final String METHOD = "Ljava/lang/reflect/Method;";
  private static final String PROXY_CLASS_LOADER_PACKAGE = "org/smallmind/nutsnbolts/reflection/proxy/";

  static {
    OBJECT_METHOD_MAP.put("hashCode", "()I");
    OBJECT_METHOD_MAP.put("equals", "(Ljava/lang/Object;)Z");
    OBJECT_METHOD_MAP.put("toString", "()Ljava/lang/String;");
  }

  /**
   * Generates and instantiates a proxy for the given type that routes all calls to the supplied handler. The
   * generated methods carry every annotation that the class documentation describes as copied; in particular,
   * methods declared by JDK types carry none.
   *
   * @param toBeProxiedClass      the public, non-sealed, non-hidden class or interface to proxy; a class must also be
   *                              neither an inner (non-static nested) class, abstract, nor final, and must
   *                              have a non-private no-arg constructor, which must be public or protected
   *                              unless {@code defineInProxiedModule} is {@code true}. To stand in for a
   *                              sealed type, proxy a non-sealed type it permits, whose proxy is assignable
   *                              to the sealed type
   * @param handler               the {@link InvocationHandler} that will receive every method invocation;
   *                              {@code null} makes every method return {@code null}, {@code false}, or zero
   * @param defineInProxiedModule {@code true} to define the proxy class in the class loader, package, and
   *                              module of {@code toBeProxiedClass}; {@code false} to define it in the
   *                              unnamed module of a separate class loader
   * @param <T>                   the type of the proxy
   * @return a new proxy instance that is assignable to {@code toBeProxiedClass}
   * @throws ByteCodeManipulationException if the class is ineligible, {@code defineInProxiedModule} is
   *                                       {@code false} and its module does not export its package
   *                                       unconditionally, {@code defineInProxiedModule} is {@code true} and its
   *                                       module does not open its package to {@code org.smallmind.nutsnbolts},
   *                                       the byte code of a type in its hierarchy cannot be located or read, a
   *                                       type in the proxied method signatures cannot be loaded through its
   *                                       class loader, a type the proxied methods return or declare as thrown
   *                                       would not be accessible to the generated class, byte code generation
   *                                       fails, or the generated class cannot be defined, initialized, or
   *                                       instantiated
   */
  public static <T> T createProxy (Class<T> toBeProxiedClass, InvocationHandler handler, boolean defineInProxiedModule) {

    return createProxy(toBeProxiedClass, handler, null, defineInProxiedModule);
  }

  /**
   * Generates and instantiates a proxy for the given type, routing all calls to the supplied handler,
   * and filtering the method and parameter annotations copied onto the generated methods. Only annotations that the
   * class documentation describes as copied are subject to the filter; in particular, methods declared by JDK types
   * carry none, whatever the filter allows.
   *
   * @param toBeProxiedClass      the public, non-sealed, non-hidden class or interface to proxy; a class must also be
   *                              neither an inner (non-static nested) class, abstract, nor final, and must
   *                              have a non-private no-arg constructor, which must be public or protected
   *                              unless {@code defineInProxiedModule} is {@code true}. To stand in for a
   *                              sealed type, proxy a non-sealed type it permits, whose proxy is assignable
   *                              to the sealed type
   * @param handler               the {@link InvocationHandler} that will receive every method invocation;
   *                              {@code null} makes every method return {@code null}, {@code false}, or zero
   * @param annotationFilter      an optional filter that decides which of the copied method and parameter
   *                              annotations the generated methods keep; {@code null} keeps all of them.
   *                              Annotations on methods declared by JDK types are never copied, so no filter
   *                              can keep them
   * @param defineInProxiedModule {@code true} to define the proxy class in the class loader, package, and
   *                              module of {@code toBeProxiedClass}; {@code false} to define it in the
   *                              unnamed module of a separate class loader
   * @param <T>                   the type of the proxy
   * @return a new proxy instance that is assignable to {@code toBeProxiedClass}
   * @throws ByteCodeManipulationException if the class is ineligible, {@code defineInProxiedModule} is
   *                                       {@code false} and its module does not export its package
   *                                       unconditionally, {@code defineInProxiedModule} is {@code true} and its
   *                                       module does not open its package to {@code org.smallmind.nutsnbolts},
   *                                       the byte code of a type in its hierarchy cannot be located or read, a
   *                                       type in the proxied method signatures cannot be loaded through its
   *                                       class loader, a type the proxied methods return or declare as thrown
   *                                       would not be accessible to the generated class, byte code generation
   *                                       fails, or the generated class cannot be defined, initialized, or
   *                                       instantiated
   */
  public static <T> T createProxy (Class<T> toBeProxiedClass, InvocationHandler handler, AnnotationFilter annotationFilter, boolean defineInProxiedModule) {

    ConcurrentHashMap<String, Class<?>> proxyClassMap = getProxyClassMap(toBeProxiedClass, defineInProxiedModule);
    String proxyKey = ((defineInProxiedModule) ? "module:" : "loader:") + annotationFilter;
    Class<?> extractedClass;

    if ((extractedClass = proxyClassMap.get(proxyKey)) == null) {
      synchronized (proxyClassMap) {
        if ((extractedClass = proxyClassMap.get(proxyKey)) == null) {

          ClassWriter classWriter;
          HashSet<MethodTracker> methodTrackerSet;
          LinkedList<MethodReference> methodReferenceList;
          String proxyInternalName;
          boolean initialized = false;

          checkEligibility(toBeProxiedClass, defineInProxiedModule);

          classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

          methodTrackerSet = new HashSet<>();
          methodReferenceList = new LinkedList<>();
          proxyInternalName = ((defineInProxiedModule) ? "" : PROXY_CLASS_LOADER_PACKAGE) + toBeProxiedClass.getName().replace('.', '/') + "$Proxy$_ExtractedSubclass" + PROXY_CLASS_COUNTER.incrementAndGet();

          for (Class<?> currentClass : assembleHierarchy(toBeProxiedClass)) {

            ProxyClassVisitor proxyClassVisitor = new ProxyClassVisitor(classWriter, toBeProxiedClass, currentClass, proxyInternalName, annotationFilter, methodTrackerSet, methodReferenceList, initialized);

            if (isDefinedByJdkLoader(currentClass)) {
              visitReflectively(proxyClassVisitor, currentClass);
            } else {
              ByteCodeReader.createClassReader(currentClass).accept(proxyClassVisitor, 0);
            }

            initialized = true;
          }

          createMethodInitializer(classWriter, proxyInternalName, methodReferenceList);
          classWriter.visitEnd();

          for (MethodReference methodReference : methodReferenceList) {
            for (String parameter : methodReference.parameters()) {
              loadSignatureClass(toBeProxiedClass, methodReference, parameter);
            }

            checkSignatureAccess(toBeProxiedClass, methodReference, methodReference.returnType(), defineInProxiedModule);
            if (methodReference.exceptions() != null) {
              for (String exception : methodReference.exceptions()) {
                checkSignatureAccess(toBeProxiedClass, methodReference, "L" + exception + ";", defineInProxiedModule);
              }
            }
          }

          if (defineInProxiedModule) {
            extractedClass = defineProxyClassInProxiedModule(toBeProxiedClass, classWriter.toByteArray());
          } else {
            extractedClass = defineProxyClassInProxyClassLoader(toBeProxiedClass, proxyInternalName.replace('/', '.'), classWriter.toByteArray());
          }

          proxyClassMap.put(proxyKey, extractedClass);
        }
      }
    }

    try {
      return toBeProxiedClass.cast(extractedClass.getConstructor(InvocationHandler.class).newInstance(handler));
    } catch (Exception exception) {
      throw new ByteCodeManipulationException(exception);
    }
  }

  /**
   * Returns the map that caches the proxy classes generated for a proxied type in the given mode. A proxy
   * defined in the proxied module refers to nothing in this module, so its classes are cached against the
   * proxied type itself. A proxy defined in a {@link ProxyClassLoader} keeps this module's class loader
   * reachable, so when the proxied type's loader is a strict ancestor of this module's loader, and might outlive
   * it, its classes are cached in this module instead, where they cannot keep this module's loader reachable and
   * do not keep the longer-lived proxied type reachable for any longer than it would be anyway.
   *
   * @param toBeProxiedClass      the class or interface being proxied
   * @param defineInProxiedModule {@code true} when the proxy is defined in the proxied module
   * @return the cache of generated classes, keyed by mode and annotation filter
   */
  private static ConcurrentHashMap<String, Class<?>> getProxyClassMap (Class<?> toBeProxiedClass, boolean defineInProxiedModule) {

    if ((!defineInProxiedModule) && ClassLoaderAncestry.isStrictAncestor(toBeProxiedClass.getClassLoader(), ProxyGenerator.class.getClassLoader())) {

      ConcurrentHashMap<String, Class<?>> proxyClassMap;

      if ((proxyClassMap = ANCESTOR_PROXY_CLASS_MAP.get(toBeProxiedClass)) == null) {

        ConcurrentHashMap<String, Class<?>> priorProxyClassMap;

        if ((priorProxyClassMap = ANCESTOR_PROXY_CLASS_MAP.putIfAbsent(toBeProxiedClass, proxyClassMap = new ConcurrentHashMap<>())) != null) {
          proxyClassMap = priorProxyClassMap;
        }
      }

      return proxyClassMap;
    }

    return PROXY_CLASS_MAP_VALUE.get(toBeProxiedClass);
  }

  /**
   * Determines whether a type belongs to the JDK, meaning it was defined by the bootstrap or platform class
   * loader.
   *
   * @param type the type to test
   * @return {@code true} if the bootstrap or platform class loader defined {@code type}
   */
  private static boolean isDefinedByJdkLoader (Class<?> type) {

    ClassLoader classLoader;

    return ((classLoader = type.getClassLoader()) == null) || (classLoader == ClassLoader.getPlatformClassLoader());
  }

  /**
   * Feeds a type's constructors and methods to a {@link ProxyClassVisitor} from reflection rather than byte
   * code, in the order a {@link org.objectweb.asm.ClassReader} would: the class header, then the members, then
   * the end of the class. The header carries Java 8 as the class file version. No annotations are visited.
   *
   * @param classVisitor the visitor for the type
   * @param type         the type to describe
   */
  private static void visitReflectively (ClassVisitor classVisitor, Class<?> type) {

    classVisitor.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, Type.getInternalName(type), null, null, null);

    for (Constructor<?> constructor : type.getDeclaredConstructors()) {
      visitReflectedMember(classVisitor, constructor.getModifiers(), constructor.isSynthetic(), "<init>", Type.getConstructorDescriptor(constructor), constructor.getExceptionTypes());
    }
    for (Method method : type.getDeclaredMethods()) {
      visitReflectedMember(classVisitor, method.getModifiers(), method.isSynthetic(), method.getName(), Type.getMethodDescriptor(method), method.getExceptionTypes());
    }

    classVisitor.visitEnd();
  }

  /**
   * Feeds one reflected constructor or method to a class visitor, ending the method visitor it returns.
   *
   * @param classVisitor   the visitor for the declaring type
   * @param modifiers      the member's modifiers
   * @param synthetic      {@code true} if the member is synthetic
   * @param name           the member name, {@code <init>} for a constructor
   * @param descriptor     the JVM method descriptor
   * @param exceptionTypes the declared exception types
   */
  private static void visitReflectedMember (ClassVisitor classVisitor, int modifiers, boolean synthetic, String name, String descriptor, Class<?>[] exceptionTypes) {

    MethodVisitor methodVisitor;
    String[] exceptions = null;
    int access = modifiers & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ABSTRACT);

    if (synthetic) {
      access |= Opcodes.ACC_SYNTHETIC;
    }

    if (exceptionTypes.length > 0) {
      exceptions = new String[exceptionTypes.length];
      for (int index = 0; index < exceptionTypes.length; index++) {
        exceptions[index] = Type.getInternalName(exceptionTypes[index]);
      }
    }

    if ((methodVisitor = classVisitor.visitMethod(access, name, descriptor, null, exceptions)) != null) {
      methodVisitor.visitEnd();
    }
  }

  /**
   * Verifies that a proxy can be generated for the given type. Every proxied type must be public and neither
   * sealed nor hidden. A proxy defined in a separate class loader also needs the type's package to be exported
   * unconditionally, because the generated class extends or implements it from the unnamed module. A class must
   * also be neither an inner (non-static nested) class, final, nor abstract, and must have a no-arg constructor the
   * proxy can call: one that is public or protected, or, when the proxy is defined in the proxied module, one that
   * is not private.
   *
   * @param toBeProxiedClass      the class or interface being proxied
   * @param defineInProxiedModule {@code true} when the proxy will be defined in the runtime package of
   *                              {@code toBeProxiedClass}
   * @throws ByteCodeManipulationException if the type is ineligible
   */
  private static void checkEligibility (Class<?> toBeProxiedClass, boolean defineInProxiedModule) {

    int toBeProxiedClassModifiers = toBeProxiedClass.getModifiers();

    if (!Modifier.isPublic(toBeProxiedClassModifiers)) {
      throw new ByteCodeManipulationException("The proxy class(%s) must be 'public'", toBeProxiedClass.getName());
    }
    if (toBeProxiedClass.isSealed()) {
      throw new ByteCodeManipulationException("The proxy class(%s) must not be 'sealed'", toBeProxiedClass.getName());
    }
    if (toBeProxiedClass.isHidden()) {
      throw new ByteCodeManipulationException("The proxy class(%s) must not be hidden", toBeProxiedClass.getName());
    }
    if ((!defineInProxiedModule) && (!toBeProxiedClass.getModule().isExported(toBeProxiedClass.getPackageName()))) {
      throw new ByteCodeManipulationException("The module(%s) must export the package(%s) of the proxy class(%s) unconditionally", describeModule(toBeProxiedClass.getModule()), toBeProxiedClass.getPackageName(), toBeProxiedClass.getName());
    }

    if (!toBeProxiedClass.isInterface()) {

      Constructor<?> noArgConstructor;
      int noArgConstructorModifiers;

      if (toBeProxiedClass.isMemberClass() && (!Modifier.isStatic(toBeProxiedClassModifiers))) {
        throw new ByteCodeManipulationException("A nested proxy class(%s) must be 'static'", toBeProxiedClass.getName());
      }
      if (Modifier.isFinal(toBeProxiedClassModifiers)) {
        throw new ByteCodeManipulationException("A concrete proxy class(%s) must not be 'final'", toBeProxiedClass.getName());
      }
      if (Modifier.isAbstract(toBeProxiedClassModifiers)) {
        throw new ByteCodeManipulationException("A concrete proxy class(%s) must not be 'abstract'", toBeProxiedClass.getName());
      }

      try {
        noArgConstructor = toBeProxiedClass.getDeclaredConstructor();
      } catch (NoSuchMethodException noSuchMethodException) {
        throw new ByteCodeManipulationException(noSuchMethodException, "A concrete proxy class(%s) must have a no-arg constructor", toBeProxiedClass.getName());
      }

      if (Modifier.isPrivate(noArgConstructorModifiers = noArgConstructor.getModifiers())) {
        throw new ByteCodeManipulationException("The no-arg constructor of the proxy class(%s) must not be 'private'", toBeProxiedClass.getName());
      }
      if (!(defineInProxiedModule || Modifier.isPublic(noArgConstructorModifiers) || Modifier.isProtected(noArgConstructorModifiers))) {
        throw new ByteCodeManipulationException("The no-arg constructor of the proxy class(%s) must be 'public' or 'protected' unless the proxy is defined in the proxied module", toBeProxiedClass.getName());
      }
    }
  }

  /**
   * Verifies that the generated class will be able to access a type that a proxied method returns or declares as
   * thrown, because the generated method casts to the former and catches the latter. A type in the runtime package
   * of the generated class is always accessible. Otherwise the type must be public (or a protected member type),
   * and its package must be exported to the module of the generated class, which must read the type's module. A
   * proxy defined in the proxied module therefore needs that module to read the type's module and to be exported
   * the type's package; a proxy defined in a separate class loader, whose unnamed module reads every module, needs
   * the type's package to be exported unconditionally. Primitive types need no access, and an array type needs
   * access to its element type.
   *
   * @param toBeProxiedClass      the class or interface being proxied
   * @param methodReference       the proxied method whose signature contains the type
   * @param typeDescriptor        the JVM type descriptor of the type
   * @param defineInProxiedModule {@code true} when the proxy will be defined in the class loader, runtime
   *                              package, and module of {@code toBeProxiedClass}
   * @throws ByteCodeManipulationException if the type cannot be loaded, or would not be accessible to the
   *                                       generated class
   */
  private static void checkSignatureAccess (Class<?> toBeProxiedClass, MethodReference methodReference, String typeDescriptor, boolean defineInProxiedModule) {

    Class<?> signatureClass;

    if ((signatureClass = loadSignatureClass(toBeProxiedClass, methodReference, typeDescriptor)) != null) {

      String binaryName = signatureClass.getName();

      if (!(defineInProxiedModule && (signatureClass.getClassLoader() == toBeProxiedClass.getClassLoader()) && signatureClass.getPackageName().equals(toBeProxiedClass.getPackageName()))) {

        Module signatureModule = signatureClass.getModule();
        int signatureClassModifiers = signatureClass.getModifiers();

        if (!(Modifier.isPublic(signatureClassModifiers) || (signatureClass.isMemberClass() && Modifier.isProtected(signatureClassModifiers)))) {
          throw new ByteCodeManipulationException("The type(%s) returned or thrown by method(%s) of the proxy class(%s) must be 'public'", binaryName, methodReference.methodName(), toBeProxiedClass.getName());
        }

        if (defineInProxiedModule) {

          Module toBeProxiedModule = toBeProxiedClass.getModule();

          if (!toBeProxiedModule.canRead(signatureModule)) {
            throw new ByteCodeManipulationException("The module(%s) of the proxy class(%s) must read the module(%s) of the type(%s) returned or thrown by method(%s)", describeModule(toBeProxiedModule), toBeProxiedClass.getName(), describeModule(signatureModule), binaryName, methodReference.methodName());
          }
          if (!signatureModule.isExported(signatureClass.getPackageName(), toBeProxiedModule)) {
            throw new ByteCodeManipulationException("The module(%s) must export the package(%s) to the module(%s) of the proxy class(%s), whose method(%s) returns or throws the type(%s)", describeModule(signatureModule), signatureClass.getPackageName(), describeModule(toBeProxiedModule), toBeProxiedClass.getName(), methodReference.methodName(), binaryName);
          }
        } else if (!signatureModule.isExported(signatureClass.getPackageName())) {
          throw new ByteCodeManipulationException("The module(%s) must export the package(%s) unconditionally, because method(%s) of the proxy class(%s) returns or throws the type(%s)", describeModule(signatureModule), signatureClass.getPackageName(), methodReference.methodName(), toBeProxiedClass.getName(), binaryName);
        }
      }
    }
  }

  /**
   * Loads, without initializing it, a type from the signature of a proxied method through the class loader of the
   * proxied class, which is where the generated class resolves it: the generated class is defined either by that
   * loader or by a {@link ProxyClassLoader} that delegates to it. A type that only the loader of a supertype can
   * see therefore fails here, rather than when the generated class is initialized or the method is called.
   *
   * @param toBeProxiedClass the class or interface being proxied
   * @param methodReference  the proxied method whose signature contains the type
   * @param typeDescriptor   the JVM type descriptor of the type
   * @return the type, or the element type of an array type, or {@code null} for a primitive type
   * @throws ByteCodeManipulationException if the type cannot be loaded through the proxied class's loader
   */
  private static Class<?> loadSignatureClass (Class<?> toBeProxiedClass, MethodReference methodReference, String typeDescriptor) {

    String elementDescriptor = typeDescriptor;

    while (elementDescriptor.charAt(0) == '[') {
      elementDescriptor = elementDescriptor.substring(1);
    }

    if (elementDescriptor.charAt(0) == 'L') {

      String binaryName = elementDescriptor.substring(1, elementDescriptor.length() - 1).replace('/', '.');

      try {
        return Class.forName(binaryName, false, toBeProxiedClass.getClassLoader());
      } catch (ClassNotFoundException | LinkageError exception) {
        throw new ByteCodeManipulationException(exception, "Unable to load the type(%s) in the signature of method(%s) through the class loader of the proxy class(%s)", binaryName, methodReference.methodName(), toBeProxiedClass.getName());
      }
    }

    return null;
  }

  /**
   * Names a module for an exception message.
   *
   * @param module the module to describe
   * @return the module name, or {@code unnamed} for an unnamed module
   */
  private static String describeModule (Module module) {

    return module.isNamed() ? module.getName() : "unnamed";
  }

  /**
   * Lists the types whose byte code contributes proxy methods, in the order they are visited. For a class,
   * that is the class and its superclasses up to but not including {@link Object}, followed by every interface
   * they implement; for an interface, the interface followed by every superinterface. {@link ObjectImpersonator}
   * always comes last, contributing {@code hashCode}, {@code equals} and {@code toString}. Each interface
   * appears once.
   *
   * @param toBeProxiedClass the class or interface being proxied
   * @return the ordered list of types to visit
   */
  private static LinkedList<Class<?>> assembleHierarchy (Class<?> toBeProxiedClass) {

    LinkedList<Class<?>> hierarchyList = new LinkedList<>();
    LinkedList<Class<?>> interfaceQueue = new LinkedList<>();
    HashSet<Class<?>> interfaceSet = new HashSet<>();
    Class<?> currentClass;

    if (toBeProxiedClass.isInterface()) {
      interfaceQueue.add(toBeProxiedClass);
    } else {
      currentClass = toBeProxiedClass;
      do {
        hierarchyList.add(currentClass);
        for (Class<?> interfaceClass : currentClass.getInterfaces()) {
          interfaceQueue.add(interfaceClass);
        }
      } while (((currentClass = currentClass.getSuperclass()) != null) && (!currentClass.equals(Object.class)));
    }

    while (!interfaceQueue.isEmpty()) {
      if (interfaceSet.add(currentClass = interfaceQueue.removeFirst())) {
        hierarchyList.add(currentClass);
        for (Class<?> interfaceClass : currentClass.getInterfaces()) {
          interfaceQueue.add(interfaceClass);
        }
      }
    }

    hierarchyList.add(ObjectImpersonator.class);

    return hierarchyList;
  }

  /**
   * Emits a static {@link java.lang.reflect.Method} field for every proxied method, and a static initializer
   * that resolves each one through {@link Class#getMethod(String, Class[])}. Reference parameter types are
   * loaded through the class loader of the proxy class.
   *
   * @param classVisitor        the visitor accumulating the generated class
   * @param proxyInternalName   the internal name of the generated class
   * @param methodReferenceList the proxied methods, each naming the field that will hold it
   */
  private static void createMethodInitializer (ClassVisitor classVisitor, String proxyInternalName, LinkedList<MethodReference> methodReferenceList) {

    MethodVisitor initializerVisitor;

    if (!methodReferenceList.isEmpty()) {
      for (MethodReference methodReference : methodReferenceList) {
        classVisitor.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, methodReference.fieldName(), METHOD, null, null).visitEnd();
      }

      initializerVisitor = classVisitor.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
      initializerVisitor.visitCode();

      for (MethodReference methodReference : methodReferenceList) {
        initializerVisitor.visitLdcInsn(Type.getObjectType(methodReference.ownerInternalName()));
        initializerVisitor.visitLdcInsn(methodReference.methodName());
        insertNumber(initializerVisitor, methodReference.parameters().length);
        initializerVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");

        for (int index = 0; index < methodReference.parameters().length; index++) {
          initializerVisitor.visitInsn(Opcodes.DUP);
          insertNumber(initializerVisitor, index);

          switch (methodReference.parameters()[index].charAt(0)) {
            case 'Z':
              initializerVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
              break;
            case 'B':
              initializerVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
              break;
            case 'C':
              initializerVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
              break;
            case 'S':
              initializerVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
              break;
            case 'I':
              initializerVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
              break;
            case 'J':
              initializerVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
              break;
            case 'F':
              initializerVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
              break;
            case 'D':
              initializerVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
              break;
            case 'L':
              insertClassForName(initializerVisitor, proxyInternalName, methodReference.parameters()[index].substring(1, methodReference.parameters()[index].length() - 1).replace('/', '.'));
              break;
            case '[':
              insertClassForName(initializerVisitor, proxyInternalName, methodReference.parameters()[index].replace('/', '.'));
              break;
            default:
              throw new ByteCodeManipulationException("Unknown format for parameter signature(%s)", methodReference.parameters()[index]);
          }

          initializerVisitor.visitInsn(Opcodes.AASTORE);
        }

        initializerVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)" + METHOD, false);
        initializerVisitor.visitFieldInsn(Opcodes.PUTSTATIC, proxyInternalName, methodReference.fieldName(), METHOD);
      }

      initializerVisitor.visitInsn(Opcodes.RETURN);
      initializerVisitor.visitMaxs(12, 0);
      initializerVisitor.visitEnd();
    }
  }

  /**
   * Pushes the {@link Class} for the given binary name, loaded without initialization through the class
   * loader of the proxy class.
   *
   * @param methodVisitor     the visitor that should receive the instructions
   * @param proxyInternalName the internal name of the generated class
   * @param binaryName        the binary class name, or array descriptor, to load
   */
  private static void insertClassForName (MethodVisitor methodVisitor, String proxyInternalName, String binaryName) {

    methodVisitor.visitLdcInsn(binaryName);
    methodVisitor.visitInsn(Opcodes.ICONST_0);
    methodVisitor.visitLdcInsn(Type.getObjectType(proxyInternalName));
    methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
  }

  /**
   * Pushes an integer constant onto the operand stack using the most compact opcode available.
   *
   * @param methodVisitor the visitor that should receive the push instruction
   * @param number        the non-negative integer to push; values {@code 0}–{@code 5} use
   *                      {@code ICONST_n}, values up to {@link Byte#MAX_VALUE} use {@code BIPUSH},
   *                      and larger values use {@code SIPUSH}
   */
  private static void insertNumber (MethodVisitor methodVisitor, int number) {

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

  /**
   * Defines the generated proxy class in the class loader, runtime package, and module of the proxied class,
   * and initializes it.
   *
   * @param toBeProxiedClass the class or interface being proxied
   * @param proxyClassBytes  the raw class byte code produced by ASM
   * @return the newly defined proxy {@link Class}
   * @throws ByteCodeManipulationException if the proxied class's module does not open the proxied class's
   *                                       package to this module, or the class cannot be defined or initialized
   */
  private static Class<?> defineProxyClassInProxiedModule (Class<?> toBeProxiedClass, byte[] proxyClassBytes) {

    Module proxyGeneratorModule = ProxyGenerator.class.getModule();
    Module toBeProxiedModule = toBeProxiedClass.getModule();
    MethodHandles.Lookup proxiedLookup;
    Class<?> proxyClass;

    proxyGeneratorModule.addReads(toBeProxiedModule);

    try {
      proxiedLookup = MethodHandles.privateLookupIn(toBeProxiedClass, MethodHandles.lookup());
    } catch (IllegalAccessException illegalAccessException) {
      throw new ByteCodeManipulationException(illegalAccessException, "The module(%s) of the proxy class(%s) must open the package(%s) to the module(%s)", describeModule(toBeProxiedModule), toBeProxiedClass.getName(), toBeProxiedClass.getPackageName(), describeModule(proxyGeneratorModule));
    }

    try {
      proxyClass = proxiedLookup.defineClass(proxyClassBytes);
      proxiedLookup.ensureInitialized(proxyClass);
    } catch (IllegalAccessException | LinkageError exception) {
      throw new ByteCodeManipulationException(exception, "Unable to define the proxy for class(%s)", toBeProxiedClass.getName());
    }

    return proxyClass;
  }

  /**
   * Defines the generated proxy class in the unnamed module of a new {@link ProxyClassLoader} whose parent is
   * the class loader of the proxied class, and initializes it. Each generated class has a loader of its own,
   * which nothing but that class retains.
   *
   * @param toBeProxiedClass the class or interface being proxied
   * @param proxyClassName   the binary name of the generated class
   * @param proxyClassBytes  the raw class byte code produced by ASM
   * @return the newly defined proxy {@link Class}
   * @throws ByteCodeManipulationException if the class cannot be defined or initialized
   */
  private static Class<?> defineProxyClassInProxyClassLoader (Class<?> toBeProxiedClass, String proxyClassName, byte[] proxyClassBytes) {

    ProxyClassLoader proxyClassLoader = new ProxyClassLoader(toBeProxiedClass.getClassLoader());

    try {
      return Class.forName(proxyClassLoader.extractInterface(proxyClassName, proxyClassBytes).getName(), true, proxyClassLoader);
    } catch (ClassNotFoundException | LinkageError exception) {
      throw new ByteCodeManipulationException(exception, "Unable to define the proxy for class(%s)", toBeProxiedClass.getName());
    }
  }

  /**
   * Describes a proxied method, so that the static initializer of the generated class can resolve it into
   * the named field, and so that the types in its signature can be checked for access.
   *
   * @param fieldName         the name of the static field that holds the resolved method
   * @param ownerInternalName the internal name of the type the method is resolved on
   * @param methodName        the method name
   * @param parameters        the JVM type descriptors of the method parameters, in order
   * @param returnType        the JVM type descriptor of the method's return type
   * @param exceptions        the internal names of the method's declared exceptions, or {@code null}
   */
  private record MethodReference (String fieldName, String ownerInternalName, String methodName, String[] parameters, String returnType, String[] exceptions) {

  }

  /**
   * Records a method by name and descriptor so that duplicate proxy methods are not emitted when
   * the same signature appears in multiple classes of the hierarchy.
   */
  private static class MethodTracker {

    private final String name;
    private final String description;

    private MethodTracker (String name, String description) {

      this.name = name;
      this.description = description;
    }

    /**
     * Returns the method name tracked by this record.
     *
     * @return the method name
     */
    public String getName () {

      return name;
    }

    /**
     * Returns the JVM method descriptor tracked by this record.
     *
     * @return the JVM descriptor string, e.g. {@code (Ljava/lang/String;)V}
     */
    public String getDescription () {

      return description;
    }

    /**
     * Computes a hash code from the method name and descriptor for use in {@link java.util.HashSet} membership tests.
     *
     * @return the combined hash of the name and descriptor
     */
    @Override
    public int hashCode () {

      return name.hashCode() ^ ((description == null) ? 0 : description.hashCode());
    }

    /**
     * Returns {@code true} if {@code obj} is a {@code MethodTracker} with the same name and descriptor.
     *
     * @param obj the object to compare with this tracker
     * @return {@code true} if both trackers represent the same method signature
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof MethodTracker) && ((MethodTracker)obj).getName().equals(name) && ((description == null) ? ((MethodTracker)obj).getDescription() == null : description.equals(((MethodTracker)obj).getDescription()));
    }
  }

  /**
   * A dedicated {@link ClassLoader} that can define dynamically generated proxy class bytes
   * as {@link Class} objects in the same hierarchy as the proxied class.
   */
  private static class ProxyClassLoader extends ClassLoader {

    /**
     * Constructs a proxy class loader that delegates to the specified parent.
     *
     * @param parent the parent class loader to delegate standard class loading to
     */
    public ProxyClassLoader (ClassLoader parent) {

      super(parent);
    }

    /**
     * Defines a new {@link Class} from the supplied byte code and returns it.
     *
     * @param name the binary name of the class to define
     * @param b    the raw class byte code produced by ASM
     * @return the newly defined {@link Class} object
     */
    public Class<?> extractInterface (String name, byte[] b) {

      return defineClass(name, b, 0, b.length);
    }
  }

  /**
   * ASM {@link ClassVisitor} that walks the byte code, or the reflected members, of one type in the proxied
   * hierarchy and emits, into the
   * generated class, a method that dispatches each eligible method through the stored {@link InvocationHandler}
   * field.
   */
  private static class ProxyClassVisitor extends ClassVisitor {

    private final ClassVisitor nextClassVisitor;
    private final Class<?> toBeProxiedClass;
    private final Class<?> currentClass;
    private final String proxyInternalName;
    private final AnnotationFilter annotationFilter;
    private final HashSet<MethodTracker> methodTrackerSet;
    private final LinkedList<MethodReference> methodReferenceList;
    private final boolean initialized;
    private boolean constructed = false;

    /**
     * Constructs the visitor for one type in the proxied hierarchy.
     *
     * @param nextClassVisitor    downstream visitor that accumulates the generated byte code
     * @param toBeProxiedClass    the root type being proxied
     * @param currentClass        the specific type in the hierarchy currently being visited
     * @param proxyInternalName   the internal name of the generated class
     * @param annotationFilter    optional filter applied to method annotations; {@code null} passes all
     * @param methodTrackerSet    set of method signatures already emitted to prevent duplicates
     * @param methodReferenceList the methods emitted so far, to which each newly emitted method is added
     * @param initialized         {@code true} when the class header and handler field have already been written
     */
    public ProxyClassVisitor (ClassVisitor nextClassVisitor, Class<?> toBeProxiedClass, Class<?> currentClass, String proxyInternalName, AnnotationFilter annotationFilter, HashSet<MethodTracker> methodTrackerSet, LinkedList<MethodReference> methodReferenceList, boolean initialized) {

      super(Opcodes.ASM9);

      this.nextClassVisitor = nextClassVisitor;
      this.toBeProxiedClass = toBeProxiedClass;
      this.currentClass = currentClass;
      this.proxyInternalName = proxyInternalName;
      this.annotationFilter = annotationFilter;
      this.methodTrackerSet = methodTrackerSet;
      this.methodReferenceList = methodReferenceList;
      this.initialized = initialized;
    }

    /**
     * Emits the generated class header and the {@code $proxy$_handler} field on the first call;
     * subsequent calls (for supertypes) are ignored. The generated class uses the class file version of the
     * proxied type, raised to at least Java 8.
     */
    @Override
    public void visit (int version, int access, String name, String signature, String superName, String[] interfaces) {

      if (!initialized) {

        int proxyVersion = ((version & 0xFFFF) < Opcodes.V1_8) ? Opcodes.V1_8 : version;

        if (toBeProxiedClass.isInterface()) {
          nextClassVisitor.visit(proxyVersion, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, proxyInternalName, null, "java/lang/Object", new String[] {name});
        } else {
          nextClassVisitor.visit(proxyVersion, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, proxyInternalName, null, name, null);
        }

        nextClassVisitor.visitField(Opcodes.ACC_PRIVATE, "$proxy$_handler", INVOCATION_HANDLER, null, null).visitEnd();
      }
    }

    /**
     * Emits a public constructor that accepts an {@link InvocationHandler} and stores it in the
     * handler field, calling the no-arg super constructor.  Does nothing if a constructor has
     * already been emitted or the class has not yet been initialised.
     *
     * @param signature  the generic type signature of the original constructor, or {@code null}
     * @param exceptions the checked exception type names declared by the original constructor, or {@code null}
     */
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
        initVisitor.visitFieldInsn(Opcodes.PUTFIELD, proxyInternalName, "$proxy$_handler", INVOCATION_HANDLER);
        Label l2 = new Label();
        initVisitor.visitLabel(l2);
        initVisitor.visitInsn(Opcodes.RETURN);
        Label l3 = new Label();
        initVisitor.visitLabel(l3);
        initVisitor.visitLocalVariable("this", "L" + proxyInternalName + ";", null, l0, l3, 0);
        initVisitor.visitLocalVariable("$proxy$_handler", INVOCATION_HANDLER, null, l0, l3, 1);
        initVisitor.visitMaxs(2, 2);
        initVisitor.visitEnd();

        constructed = true;
      }
    }

    /**
     * Visits a method of the current type and emits a proxy dispatcher for it when the method is
     * public, non-static, non-final, non-synthetic, and has not yet been emitted. For a class proxy, abstract
     * methods are skipped, because the class hierarchy implements them. A final method is recorded without being
     * emitted, so that the versions it overrides in supertypes are not emitted either.
     *
     * @return a {@link MethodVisitor} that copies annotations through the filter, or {@code null} to skip
     */
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
                LinkedList<String> parameterList;
                String[] parameters;
                String methodField;

                methodTrackerSet.add(methodTracker);

                parameterList = new LinkedList<>();
                for (String parameter : new ParameterIterable(desc.substring(1, desc.indexOf(')')))) {
                  parameterList.add(parameter);
                }
                parameters = new String[parameterList.size()];
                parameterList.toArray(parameters);

                methodField = "$proxy$_method" + methodReferenceList.size();
                methodReferenceList.add(new MethodReference(methodField, (toBeProxiedClass.isInterface() && currentClass.equals(ObjectImpersonator.class)) ? "java/lang/Object" : toBeProxiedClass.getName().replace('.', '/'), name, parameters, desc.substring(desc.indexOf(')') + 1), exceptions));

                proxyVisitor = nextClassVisitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, name, desc, null, exceptions);
                createProxyMethod(proxyVisitor, methodField, desc, parameters, exceptions);

                return new ProxyMethodVisitor(proxyVisitor, annotationFilter);
              } else if (((access & Opcodes.ACC_STATIC) == 0) && ((access & Opcodes.ACC_FINAL) != 0)) {
                methodTrackerSet.add(methodTracker);
              }
            }
          }
        }
      }

      return null;
    }

    /**
     * Called when the class visitor is done; ensures the constructor is emitted even if
     * the proxied class has no no-arg constructor visible to the visitor.
     */
    @Override
    public void visitEnd () {

      createConstructor(null, null);
    }

    /**
     * Emits the body of a proxy method. A {@code null} handler returns {@code null}, {@code false}, or zero.
     * Otherwise the arguments are boxed into an array and passed, with the resolved method, to
     * {@link InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])}, whose result is unboxed or
     * cast to the return type. {@link RuntimeException}, {@link Error}, and the declared exceptions are rethrown
     * unchanged; any other {@link Throwable} is wrapped in an {@link java.lang.reflect.UndeclaredThrowableException}.
     *
     * @param proxyVisitor the visitor for the generated method
     * @param methodField  the name of the static field holding the resolved method
     * @param desc         the JVM descriptor of the method
     * @param parameters   the JVM type descriptors of the method parameters, in order
     * @param exceptions   the internal names of the declared exceptions, or {@code null}
     */
    private void createProxyMethod (MethodVisitor proxyVisitor, String methodField, String desc, String[] parameters, String[] exceptions) {

      Label startLabel = new Label();
      Label tryStartLabel = new Label();
      Label tryEndLabel = new Label();
      Label undeclaredLabel = new Label();
      Label endLabel = new Label();
      LinkedList<String> rethrownList = new LinkedList<>();
      Label[] rethrownLabels;
      int[] parameterRegisters = new int[parameters.length];
      String returnType = desc.substring(desc.indexOf(')') + 1);
      int variableIndex = 1;
      int rethrownIndex = 0;

      rethrownList.add("java/lang/RuntimeException");
      rethrownList.add("java/lang/Error");
      if (exceptions != null) {
        for (String exception : exceptions) {
          rethrownList.add(exception);
        }
      }

      proxyVisitor.visitCode();

      rethrownLabels = new Label[rethrownList.size()];
      for (String rethrown : rethrownList) {
        proxyVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, rethrownLabels[rethrownIndex++] = new Label(), rethrown);
      }
      proxyVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, undeclaredLabel, "java/lang/Throwable");

      proxyVisitor.visitLabel(startLabel);
      proxyVisitor.visitVarInsn(Opcodes.ALOAD, 0);
      proxyVisitor.visitFieldInsn(Opcodes.GETFIELD, proxyInternalName, "$proxy$_handler", INVOCATION_HANDLER);
      proxyVisitor.visitJumpInsn(Opcodes.IFNONNULL, tryStartLabel);

      switch (returnType.charAt(0)) {
        case 'V':
          proxyVisitor.visitInsn(Opcodes.RETURN);
          break;
        case 'Z':
        case 'B':
        case 'C':
        case 'S':
        case 'I':
          proxyVisitor.visitInsn(Opcodes.ICONST_0);
          proxyVisitor.visitInsn(Opcodes.IRETURN);
          break;
        case 'J':
          proxyVisitor.visitInsn(Opcodes.LCONST_0);
          proxyVisitor.visitInsn(Opcodes.LRETURN);
          break;
        case 'F':
          proxyVisitor.visitInsn(Opcodes.FCONST_0);
          proxyVisitor.visitInsn(Opcodes.FRETURN);
          break;
        case 'D':
          proxyVisitor.visitInsn(Opcodes.DCONST_0);
          proxyVisitor.visitInsn(Opcodes.DRETURN);
          break;
        case 'L':
        case '[':
          proxyVisitor.visitInsn(Opcodes.ACONST_NULL);
          proxyVisitor.visitInsn(Opcodes.ARETURN);
          break;
        default:
          throw new ByteCodeManipulationException("Unknown return type(%s)", returnType);
      }

      proxyVisitor.visitLabel(tryStartLabel);
      proxyVisitor.visitVarInsn(Opcodes.ALOAD, 0);
      proxyVisitor.visitFieldInsn(Opcodes.GETFIELD, proxyInternalName, "$proxy$_handler", INVOCATION_HANDLER);
      proxyVisitor.visitVarInsn(Opcodes.ALOAD, 0);
      proxyVisitor.visitFieldInsn(Opcodes.GETSTATIC, proxyInternalName, methodField, METHOD);

      insertNumber(proxyVisitor, parameters.length);
      proxyVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

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

      proxyVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/reflect/InvocationHandler", "invoke", "(Ljava/lang/Object;" + METHOD + "[Ljava/lang/Object;)Ljava/lang/Object;", true);

      if (returnType.length() == 1) {
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

      proxyVisitor.visitLabel(tryEndLabel);

      switch (returnType.charAt(0)) {
        case 'V':
          proxyVisitor.visitInsn(Opcodes.RETURN);
          break;
        case 'Z':
        case 'B':
        case 'C':
        case 'S':
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
        case 'L':
        case '[':
          proxyVisitor.visitInsn(Opcodes.ARETURN);
          break;
        default:
          throw new ByteCodeManipulationException("Unknown return type(%s)", returnType);
      }

      for (Label rethrownLabel : rethrownLabels) {
        proxyVisitor.visitLabel(rethrownLabel);
        proxyVisitor.visitInsn(Opcodes.ATHROW);
      }

      proxyVisitor.visitLabel(undeclaredLabel);
      proxyVisitor.visitVarInsn(Opcodes.ASTORE, variableIndex);
      proxyVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/reflect/UndeclaredThrowableException");
      proxyVisitor.visitInsn(Opcodes.DUP);
      proxyVisitor.visitVarInsn(Opcodes.ALOAD, variableIndex);
      proxyVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/reflect/UndeclaredThrowableException", "<init>", "(Ljava/lang/Throwable;)V", false);
      proxyVisitor.visitInsn(Opcodes.ATHROW);

      proxyVisitor.visitLabel(endLabel);
      proxyVisitor.visitLocalVariable("this", "L" + proxyInternalName + ";", null, startLabel, endLabel, 0);
      for (int index = 0; index < parameters.length; index++) {
        proxyVisitor.visitLocalVariable("$proxy$_var" + index, parameters[index], null, startLabel, endLabel, parameterRegisters[index]);
      }

      proxyVisitor.visitMaxs(12, variableIndex + 1);
    }
  }

  /**
   * ASM {@link MethodVisitor} that forwards annotation visits to a downstream visitor only when
   * the configured {@link AnnotationFilter} allows the annotation.
   */
  private static class ProxyMethodVisitor extends MethodVisitor {

    private final MethodVisitor nextMethodVisitor;
    private final AnnotationFilter annotationFilter;

    /**
     * Constructs a method visitor that wraps {@code nextMethodVisitor} and applies the filter.
     *
     * @param nextMethodVisitor the downstream visitor that accumulates method byte code
     * @param annotationFilter  optional filter; {@code null} means all annotations are forwarded
     */
    public ProxyMethodVisitor (MethodVisitor nextMethodVisitor, AnnotationFilter annotationFilter) {

      super(Opcodes.ASM9);

      this.nextMethodVisitor = nextMethodVisitor;
      this.annotationFilter = annotationFilter;
    }

    /**
     * Forwards the annotation-default visit to the downstream visitor.
     *
     * @return the annotation visitor provided by the downstream visitor
     */
    @Override
    public AnnotationVisitor visitAnnotationDefault () {

      return nextMethodVisitor.visitAnnotationDefault();
    }

    /**
     * Forwards the method-level annotation to the downstream visitor only when the filter permits it.
     *
     * @param desc    the JVM descriptor of the annotation
     * @param visible {@code true} if the annotation is visible at runtime
     * @return the downstream annotation visitor if allowed, or {@code null} to discard the annotation
     */
    @Override
    public AnnotationVisitor visitAnnotation (String desc, boolean visible) {

      if ((annotationFilter == null) || annotationFilter.isAllowed(desc)) {

        return nextMethodVisitor.visitAnnotation(desc, visible);
      }

      return null;
    }

    /**
     * Forwards a parameter-level annotation to the downstream visitor only when the filter permits it.
     *
     * @param parameter the zero-based parameter index
     * @param desc      the JVM descriptor of the annotation
     * @param visible   {@code true} if the annotation is visible at runtime
     * @return the downstream annotation visitor if allowed, or {@code null} to discard the annotation
     */
    @Override
    public AnnotationVisitor visitParameterAnnotation (int parameter, String desc, boolean visible) {

      if ((annotationFilter == null) || annotationFilter.isAllowed(desc)) {

        return nextMethodVisitor.visitParameterAnnotation(parameter, desc, visible);
      }

      return null;
    }

    /**
     * Signals the end of the method to the downstream visitor.
     */
    @Override
    public void visitEnd () {

      nextMethodVisitor.visitEnd();
    }
  }
}
