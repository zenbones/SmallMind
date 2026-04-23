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
package org.smallmind.web.json.scaffold.util;

import java.util.HashMap;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.smallmind.nutsnbolts.lang.ClassLoaderAwareCache;

/**
 * Thread-safe cache that maps polymorphic base classes to their keyed subclasses and tracks the
 * corresponding generated proxy classes to avoid repeated reflection work during marshaling.
 */
public class PolymorphicClassCache {

  private static final ClassLoaderAwareCache<Class<?>, Class<?>> TO_PROXY_CLASS_CACHE = new ClassLoaderAwareCache<>(Class::getClassLoader);
  private static final ClassLoaderAwareCache<Class<?>, Class<?>> FROM_PROXY_CLASS_CACHE = new ClassLoaderAwareCache<>(Class::getClassLoader);
  private static final ClassLoaderAwareCache<Class<?>, HashMap<String, Class<?>>> SUB_CLASS_MAP_CACHE = new ClassLoaderAwareCache<>(Class::getClassLoader);

  /**
   * Looks up the concrete subclass for a given base class and polymorphic key, building the subclass
   * map on first access from the {@link XmlPolymorphicSubClasses} annotation.
   *
   * @param baseClass      polymorphic base class annotated with {@link XmlPolymorphicSubClasses}
   * @param polymorphicKey key (typically the {@link XmlRootElement#name()}) identifying the subclass
   * @return matching subclass, or {@code null} if no subclass is registered for the key
   * @throws JAXBProcessingException if required annotations are missing from the base or sub classes
   */
  public static Class<?> getPolymorphicSubClass (Class<?> baseClass, String polymorphicKey) {

    HashMap<String, Class<?>> polymorphicKeyMap;

    if ((polymorphicKeyMap = SUB_CLASS_MAP_CACHE.get(baseClass)) == null) {
      synchronized (SUB_CLASS_MAP_CACHE) {
        if ((polymorphicKeyMap = SUB_CLASS_MAP_CACHE.get(baseClass)) == null) {

          XmlPolymorphicSubClasses xmlPolymorphicSubClassesAnnotation;

          if ((xmlPolymorphicSubClassesAnnotation = baseClass.getAnnotation(XmlPolymorphicSubClasses.class)) == null) {
            throw new JAXBProcessingException("The class(%s) is missing an %s annotation", baseClass.getName(), XmlPolymorphicSubClasses.class.getSimpleName());
          } else {

            XmlRootElement baseClassXmlRootElementAnnotation;

            polymorphicKeyMap = new HashMap<>();

            if ((baseClassXmlRootElementAnnotation = baseClass.getAnnotation(XmlRootElement.class)) != null) {
              polymorphicKeyMap.put(baseClassXmlRootElementAnnotation.name(), baseClass);
            }

            for (Class<?> polymorphicSubClass : xmlPolymorphicSubClassesAnnotation.value()) {

              XmlRootElement subClassXmlRootElementAnnotation;

              if ((subClassXmlRootElementAnnotation = polymorphicSubClass.getAnnotation(XmlRootElement.class)) == null) {
                throw new JAXBProcessingException("The sub-class(%s) is missing an %s annotation", polymorphicSubClass.getName(), XmlRootElement.class.getSimpleName());
              }

              polymorphicKeyMap.put(subClassXmlRootElementAnnotation.name(), polymorphicSubClass);
            }

            SUB_CLASS_MAP_CACHE.put(baseClass, polymorphicKeyMap);
          }
        }
      }
    }

    return polymorphicKeyMap.get(polymorphicKey);
  }

  /**
   * Registers the bidirectional mapping between a polymorphic subclass and its generated proxy class.
   *
   * @param polymorphicSubClass real subclass
   * @param proxySubClass       dynamically generated proxy subclass
   */
  public static void addClassRelationship (Class<?> polymorphicSubClass, Class<?> proxySubClass) {

    TO_PROXY_CLASS_CACHE.putIfAbsent(polymorphicSubClass, proxySubClass);
    FROM_PROXY_CLASS_CACHE.putIfAbsent(proxySubClass, polymorphicSubClass);
  }

  /**
   * Retrieves the proxy class previously associated with a polymorphic subclass.
   *
   * @param polymorphicSubClass real subclass to look up
   * @return associated proxy class, or {@code null} if not yet cached
   */
  public static Class<?> getProxyClassForPolymorphicClass (Class<?> polymorphicSubClass) {

    return TO_PROXY_CLASS_CACHE.get(polymorphicSubClass);
  }

  /**
   * Retrieves the real polymorphic subclass associated with a proxy class.
   *
   * @param proxySubClass proxy subclass to look up
   * @return real subclass, or {@code null} if not yet cached
   */
  public static Class<?> getPolymorphicClassForProxyClass (Class<?> proxySubClass) {

    return FROM_PROXY_CLASS_CACHE.get(proxySubClass);
  }
}
