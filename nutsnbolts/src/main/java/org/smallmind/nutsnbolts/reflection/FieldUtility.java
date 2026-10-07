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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;

/**
 * Static helpers that discover {@link FieldAccessor} descriptors for the non-static, non-transient,
 * non-synthetic fields of a class and its superclasses. Access to those fields, and to their getters and
 * setters, comes from the {@link MethodHandles.Lookup} the caller passes, never from this module, so a caller
 * cannot reach any member through these helpers that it could not reach itself. Pass
 * {@link MethodHandles#lookup()}, called from the module whose access should apply.
 * <p>
 * The accessors for a class are cached separately for each module whose lookups request them, because what a
 * full-privilege lookup can reach depends only on its module, and a lookup from another module never receives
 * them. The cache does not keep a class, its
 * class loader, or this module's class loader reachable for longer than they would be anyway, except when the
 * class's loader and the lookup's loader are unrelated through their parent chains; then the class keeps the
 * lookup's module, and this module, reachable for as long as it lives.
 */
public class FieldUtility {

  private static final AlphaNumericComparator<FieldAccessor> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<>(FieldAccessor::getName);
  private static final MethodType ADD_READS_TYPE = MethodType.methodType(Module.class, Module.class);
  private static final int FULL_PRIVILEGE_MODES = MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.MODULE;
  // Held against the inspected class, keyed by the module of the lookup
  private static final ClassValue<ConcurrentHashMap<Module, FieldAccessor[]>> FIELD_ACCESSOR_MAP_VALUE = new ClassValue<>() {

    @Override
    protected ConcurrentHashMap<Module, FieldAccessor[]> computeValue (Class<?> type) {

      return new ConcurrentHashMap<>();
    }
  };
  // Held against the lookup class, keyed by the inspected class, when the inspected class's loader is an ancestor of the lookup's
  private static final ClassValue<ConcurrentHashMap<Class<?>, FieldAccessor[]>> ANCESTOR_FIELD_ACCESSOR_MAP_VALUE = new ClassValue<>() {

    @Override
    protected ConcurrentHashMap<Class<?>, FieldAccessor[]> computeValue (Class<?> type) {

      return new ConcurrentHashMap<>();
    }
  };

  /**
   * Returns the {@link FieldAccessor} for the field with the given name on the supplied class or its
   * superclasses. When a subclass and a superclass both declare the name, the subclass field is returned.
   * Access rules, and caching, are those of {@link #getFieldAccessors(MethodHandles.Lookup, Class)}; every field
   * of the class must be accessible, not only the one named.
   *
   * @param lookup a full-privilege lookup, as returned by {@link MethodHandles#lookup()}, whose access is used
   * @param clazz  the class to inspect
   * @param name   the exact field name to look up
   * @return the accessor, or {@code null} if no such field exists
   * @throws IllegalArgumentException if {@code lookup} does not have full privilege access
   * @throws IllegalAccessException   if {@code lookup} cannot access a field of the class, or its getter or setter
   */
  public static FieldAccessor getFieldAccessor (MethodHandles.Lookup lookup, Class<?> clazz, String name)
    throws IllegalAccessException {

    for (FieldAccessor fieldAccessor : acquireFieldAccessors(lookup, clazz)) {
      if (fieldAccessor.getName().equals(name)) {

        return fieldAccessor;
      }
    }

    return null;
  }

  /**
   * Returns accessors for all non-static, non-transient, non-synthetic fields of the given class and every
   * superclass, sorted alpha-numerically by field name. The accessors are created on the first request from a
   * module and cached for later requests from the same module; each call returns a new array holding the cached
   * accessors, which are immutable. A failed request is not cached.
   * <p>
   * A field is read through its public {@code getXxx} or {@code isXxx} method that returns a value and written
   * through its public {@code setXxx} method taking the field's type when they exist, and directly otherwise. Every member is reached with the access of
   * {@code lookup}: a getter or setter must be accessible to the lookup's module, which holds when the lookup's
   * module declares it or its package is exported or opened to the lookup's module; a field read or written
   * directly must be declared in the lookup's module or in a package opened to it. When the lookup's module does
   * not yet read the module declaring a member, a read edge is added on its behalf through {@code lookup}. A
   * {@code final} field without a setter can be read but not written.
   *
   * @param lookup a full-privilege lookup, as returned by {@link MethodHandles#lookup()}, whose access is used
   * @param clazz  the class whose fields should be discovered
   * @return a new, sorted array of the {@link FieldAccessor} objects for the discovered fields
   * @throws IllegalArgumentException if {@code lookup} does not have full privilege access
   * @throws IllegalAccessException   if {@code lookup} cannot access a field, or its getter or setter
   */
  public static FieldAccessor[] getFieldAccessors (MethodHandles.Lookup lookup, Class<?> clazz)
    throws IllegalAccessException {

    return acquireFieldAccessors(lookup, clazz).clone();
  }

  /**
   * Returns the cached accessors of a class for the module of a lookup, creating and caching them on the first
   * request. They are held against the class itself, unless the class's loader is a strict ancestor of the
   * lookup's loader and so might outlive the lookup's module; then they are held against the lookup class, where
   * the longer-lived class they refer to is kept reachable no longer than it would be anyway.
   *
   * @param lookup a full-privilege lookup whose access is used
   * @param clazz  the class whose fields should be discovered
   * @return the cached, sorted array of accessors, which callers must not modify or expose
   * @throws IllegalArgumentException if {@code lookup} does not have full privilege access
   * @throws IllegalAccessException   if {@code lookup} cannot access a field, or its getter or setter
   */
  private static FieldAccessor[] acquireFieldAccessors (MethodHandles.Lookup lookup, Class<?> clazz)
    throws IllegalAccessException {

    FieldAccessor[] fieldAccessors;
    FieldAccessor[] priorFieldAccessors;

    checkLookup(lookup);

    if (ClassLoaderAncestry.isStrictAncestor(clazz.getClassLoader(), lookup.lookupClass().getClassLoader())) {

      ConcurrentHashMap<Class<?>, FieldAccessor[]> ancestorFieldAccessorMap = ANCESTOR_FIELD_ACCESSOR_MAP_VALUE.get(lookup.lookupClass());

      if ((fieldAccessors = ancestorFieldAccessorMap.get(clazz)) == null) {
        if ((priorFieldAccessors = ancestorFieldAccessorMap.putIfAbsent(clazz, fieldAccessors = assembleFieldAccessors(lookup, clazz))) != null) {
          fieldAccessors = priorFieldAccessors;
        }
      }
    } else {

      ConcurrentHashMap<Module, FieldAccessor[]> fieldAccessorMap = FIELD_ACCESSOR_MAP_VALUE.get(clazz);
      Module lookupModule = lookup.lookupClass().getModule();

      if ((fieldAccessors = fieldAccessorMap.get(lookupModule)) == null) {
        if ((priorFieldAccessors = fieldAccessorMap.putIfAbsent(lookupModule, fieldAccessors = assembleFieldAccessors(lookup, clazz))) != null) {
          fieldAccessors = priorFieldAccessors;
        }
      }
    }

    return fieldAccessors;
  }

  /**
   * Creates the accessors for every field of a class and its superclasses, sorted by field name.
   *
   * @param lookup a full-privilege lookup whose access is used
   * @param clazz  the class whose fields should be discovered
   * @return a sorted array of new accessors
   * @throws IllegalAccessException if {@code lookup} cannot access a field, or its getter or setter
   */
  private static FieldAccessor[] assembleFieldAccessors (MethodHandles.Lookup lookup, Class<?> clazz)
    throws IllegalAccessException {

    Class<?> currentClass = clazz;
    LinkedList<FieldAccessor> fieldAccessorList = new LinkedList<>();

    do {
      for (Field field : currentClass.getDeclaredFields()) {
        if (isAccessorField(field)) {
          fieldAccessorList.add(assembleFieldAccessor(lookup, clazz, field));
        }
      }
    } while ((currentClass = currentClass.getSuperclass()) != null);

    fieldAccessorList.sort(ALPHA_NUMERIC_COMPARATOR);

    return fieldAccessorList.toArray(new FieldAccessor[0]);
  }

  /**
   * Verifies that a lookup has the full privilege access ({@code PRIVATE} and {@code MODULE}) needed to act on
   * behalf of its module.
   *
   * @param lookup the lookup to check
   * @throws IllegalArgumentException if {@code lookup} does not have full privilege access
   */
  static void checkLookup (MethodHandles.Lookup lookup) {

    if ((lookup.lookupModes() & FULL_PRIVILEGE_MODES) != FULL_PRIVILEGE_MODES) {
      throw new IllegalArgumentException("The lookup(" + lookup + ") must have full privilege access, as returned by MethodHandles.lookup()");
    }
  }

  /**
   * Makes the module of a lookup read the given module, if it does not already. The read edge is added
   * through {@code lookup}, so it is the lookup's module that adds it.
   *
   * @param lookup a full-privilege lookup
   * @param module the module that the lookup's module must read
   * @throws IllegalAccessException if the read edge cannot be added
   */
  static void ensureReadable (MethodHandles.Lookup lookup, Module module)
    throws IllegalAccessException {

    Module lookupModule = lookup.lookupClass().getModule();

    if (!lookupModule.canRead(module)) {
      try {
        lookup.findVirtual(Module.class, "addReads", ADD_READS_TYPE).invoke(lookupModule, module);
      } catch (RuntimeException | Error exception) {
        throw exception;
      } catch (Throwable throwable) {

        IllegalAccessException illegalAccessException = new IllegalAccessException("Unable to make the module(" + lookupModule + ") read the module(" + module + ")");

        illegalAccessException.initCause(throwable);
        throw illegalAccessException;
      }
    }
  }

  /**
   * Determines whether a field is one that accessors are created for.
   *
   * @param field the field to test
   * @return {@code true} unless the field is synthetic, static, or transient
   */
  private static boolean isAccessorField (Field field) {

    return !(field.isSynthetic() || Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()));
  }

  /**
   * Creates the accessor for one field, choosing between its getter and setter methods and direct access.
   *
   * @param lookup a full-privilege lookup whose access is used
   * @param clazz  the class being inspected, on which getters and setters are located
   * @param field  the field, declared by {@code clazz} or a superclass
   * @return the new accessor
   * @throws IllegalAccessException if {@code lookup} cannot access the field, or its getter or setter
   */
  private static FieldAccessor assembleFieldAccessor (MethodHandles.Lookup lookup, Class<?> clazz, Field field)
    throws IllegalAccessException {

    Method getterMethod = locateGetter(clazz, field);
    Method setterMethod = locateSetter(clazz, field);
    MethodHandle getterHandle;
    MethodHandle setterHandle;

    if (getterMethod != null) {
      getterHandle = unreflectMethod(lookup, getterMethod);
    } else {
      ensureReadable(lookup, field.getDeclaringClass().getModule());
      getterHandle = MethodHandles.privateLookupIn(field.getDeclaringClass(), lookup).unreflectGetter(field);
    }

    if (setterMethod != null) {
      setterHandle = unreflectMethod(lookup, setterMethod);
    } else if (Modifier.isFinal(field.getModifiers())) {
      setterHandle = null;
    } else {
      ensureReadable(lookup, field.getDeclaringClass().getModule());
      setterHandle = MethodHandles.privateLookupIn(field.getDeclaringClass(), lookup).unreflectSetter(field);
    }

    return new FieldAccessor(field, getterHandle, setterHandle);
  }

  /**
   * Creates a handle for a public getter or setter method. The method is reached through {@code lookup}
   * directly when it is accessible to the lookup's module, and otherwise through a private lookup in the
   * class that declares it, which needs that class's package to be opened to the lookup's module.
   *
   * @param lookup a full-privilege lookup whose access is used
   * @param method the method
   * @return the handle invoking the method
   * @throws IllegalAccessException if neither way of reaching the method is permitted
   */
  private static MethodHandle unreflectMethod (MethodHandles.Lookup lookup, Method method)
    throws IllegalAccessException {

    ensureReadable(lookup, method.getDeclaringClass().getModule());

    try {
      return lookup.unreflect(method);
    } catch (IllegalAccessException illegalAccessException) {
      return MethodHandles.privateLookupIn(method.getDeclaringClass(), lookup).unreflect(method);
    }
  }

  /**
   * Attempts to find either a {@code getXxx} or {@code isXxx} public non-static method for the field that returns a value.
   *
   * @param clazz the class to search for getter methods
   * @param field the field for which a getter is sought
   * @return the getter {@link Method}, or {@code null} if neither convention yields a match
   */
  private static Method locateGetter (Class<?> clazz, Field field) {

    try {

      Method method;

      try {
        method = clazz.getMethod(BeanUtility.asGetterName(field.getName()));
      } catch (NoSuchMethodException noSuchMethodException) {
        method = clazz.getMethod(BeanUtility.asIsName(field.getName()));
      }

      return (Modifier.isStatic(method.getModifiers()) || (method.getReturnType() == void.class)) ? null : method;
    } catch (NoSuchMethodException noSuchMethodException) {

      return null;
    }
  }

  /**
   * Attempts to find a {@code setXxx} public non-static method for the field that accepts the field's type.
   *
   * @param clazz the class to search for setter methods
   * @param field the field for which a setter is sought
   * @return the setter {@link Method}, or {@code null} if none is found
   */
  private static Method locateSetter (Class<?> clazz, Field field) {

    try {

      Method method;

      if (Modifier.isStatic((method = clazz.getMethod(BeanUtility.asSetterName(field.getName()), field.getType())).getModifiers())) {

        return null;
      }

      return method;
    } catch (NoSuchMethodException noSuchMethodException) {

      return null;
    }
  }
}
