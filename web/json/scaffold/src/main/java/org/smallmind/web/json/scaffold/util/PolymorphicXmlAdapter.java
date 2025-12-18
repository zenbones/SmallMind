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

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.nutsnbolts.reflection.AnnotationFilter;
import org.smallmind.nutsnbolts.reflection.OffloadingInvocationHandler;
import org.smallmind.nutsnbolts.reflection.PassType;
import org.smallmind.nutsnbolts.reflection.ProxyGenerator;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;

/**
 * JAXB adapter that serializes polymorphic types by wrapping them in a single-key object whose key
 * corresponds to {@link XmlRootElement#name()}.
 *
 * @param <T> polymorphic base type
 */
public abstract class PolymorphicXmlAdapter<T> extends XmlAdapter<JsonNode, T> {

  private final Class<?> baseClass;

  /**
   * Resolves the polymorphic base class from the generic type parameter.
   */
  public PolymorphicXmlAdapter () {

    baseClass = GenericUtility.getTypeArgumentsOfSubclass(PolymorphicXmlAdapter.class, this.getClass()).get(0);
  }

  /**
   * Deserializes a single-field JSON object into the appropriate subclass, using the field name as
   * the polymorphic discriminator.
   *
   * @param node JSON node representing the polymorphic value
   * @return instantiated subclass
   * @throws JAXBProcessingException if the payload is improperly formatted or cannot be resolved
   */
  @Override
  public T unmarshal (JsonNode node) {

    if (!(JsonNodeType.OBJECT.equals(node.getNodeType()) && (node.size() == 1))) {
      throw new JAXBProcessingException("The json for the sub-class of class(%s) is improperly formatted", baseClass.getName());
    } else {

      Class<?> polymorphicSubClass;
      String polymorphicKey;

      if ((polymorphicSubClass = PolymorphicClassCache.getPolymorphicSubClass(baseClass, polymorphicKey = node.fieldNames().next())) == null) {
        throw new JAXBProcessingException("Unable to map the root element name(%s) to any known sub-class of class(%s) listed in the %s annotation", polymorphicKey, baseClass.getName(), XmlPolymorphicSubClasses.class.getSimpleName());
      } else {

        Class<?> proxySubClass;
        Object polymorphicInstance;

        if ((proxySubClass = PolymorphicClassCache.getProxyClassForPolymorphicClass(polymorphicSubClass)) == null) {

          Object proxyObject = ProxyGenerator.createProxy(polymorphicSubClass, null, new AnnotationFilter(PassType.EXCLUDE, XmlJavaTypeAdapter.class));

          PolymorphicClassCache.addClassRelationship(polymorphicSubClass, proxySubClass = proxyObject.getClass());
        }

        try {
          PolymorphicValueInstantiator.setPolymorphicInstance(polymorphicInstance = polymorphicSubClass.getDeclaredConstructor().newInstance());
        } catch (Exception exception) {
          throw new JAXBProcessingException(exception);
        }

        JsonCodec.convert(node.get(polymorphicKey), proxySubClass);

        return (T)polymorphicInstance;
      }
    }
  }

  /**
   * Serializes a subclass instance by wrapping it in an object keyed by its {@link XmlRootElement#name()}.
   *
   * @param value value to serialize
   * @return JSON node containing the wrapped value
   * @throws JAXBProcessingException if the subclass lacks the required annotation
   */
  @Override
  public JsonNode marshal (T value) {

    XmlRootElement xmlRootElementAnnotation;

    if ((xmlRootElementAnnotation = value.getClass().getAnnotation(XmlRootElement.class)) == null) {
      throw new JAXBProcessingException("The class(%s) is missing a %s annotation", value.getClass().getName(), XmlRootElement.class.getSimpleName());
    } else {

      ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
      Object proxyObject = ProxyGenerator.createProxy(value.getClass(), new OffloadingInvocationHandler(value), new AnnotationFilter(PassType.EXCLUDE, XmlJavaTypeAdapter.class));

      PolymorphicClassCache.addClassRelationship(value.getClass(), proxyObject.getClass());
      rootNode.set(xmlRootElementAnnotation.name(), JsonCodec.writeAsJsonNode(proxyObject));

      return rootNode;
    }
  }
}
