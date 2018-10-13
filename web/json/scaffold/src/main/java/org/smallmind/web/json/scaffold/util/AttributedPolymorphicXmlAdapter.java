/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.nutsnbolts.reflection.AnnotationFilter;
import org.smallmind.nutsnbolts.reflection.OffloadingInvocationHandler;
import org.smallmind.nutsnbolts.reflection.PassType;
import org.smallmind.nutsnbolts.reflection.ProxyGenerator;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;

public abstract class AttributedPolymorphicXmlAdapter<T> extends XmlAdapter<ObjectNode, T> {

  private Class<?> baseClass;

  public AttributedPolymorphicXmlAdapter () {

    baseClass = GenericUtility.getTypeArguments(AttributedPolymorphicXmlAdapter.class, this.getClass()).get(0);
  }

  public String getPolymorphicAttributeName () {

    return "java/object";
  }

  @Override
  public T unmarshal (ObjectNode objectNode) {

    JsonNode polymorphicKeyNode;

    if ((polymorphicKeyNode = objectNode.get(getPolymorphicAttributeName())) == null) {
      throw new JAXBProcessingException("The json for the sub-class of class(%s) is improperly formatted", baseClass.getName());
    } else {

      Class<?> polymorphicSubClass;
      String polymorphicKey;

      if ((polymorphicSubClass = PolymorphicClassCache.getPolymorphicSubClass(baseClass, polymorphicKey = polymorphicKeyNode.asText())) == null) {
        throw new JAXBProcessingException("Unable to map the root element name(%s) to any known sub-class of class(%s) listed in the %s annotation", polymorphicKey, baseClass.getName(), XmlPolymorphicSubClasses.class.getSimpleName());
      } else {

        Class<?> proxySubClass;
        Object polymorphicInstance;

        if ((proxySubClass = PolymorphicClassCache.getProxyClassForPolymorphicClass(polymorphicSubClass)) == null) {

          Object proxyObject = ProxyGenerator.createProxy(polymorphicSubClass, null, new AnnotationFilter(PassType.EXCLUDE, XmlJavaTypeAdapter.class));

          PolymorphicClassCache.addClassRelationship(polymorphicSubClass, proxySubClass = proxyObject.getClass());
        }

        objectNode.remove(getPolymorphicAttributeName());

        try {
          PolymorphicValueInstantiator.setPolymorphicInstance(polymorphicInstance = polymorphicSubClass.newInstance());
        } catch (Exception exception) {
          throw new JAXBProcessingException(exception);
        }

        JsonCodec.convert(objectNode, proxySubClass);

        return (T)polymorphicInstance;
      }
    }
  }

  @Override
  public ObjectNode marshal (T value)
    throws JsonProcessingException {

    if (value.getClass().equals(baseClass)) {
      throw new JAXBProcessingException("Attempting to serialize the polymorphic root class(%s) would be infinitely recursive", baseClass.getName());
    } else {

      XmlRootElement xmlRootElementAnnotation;

      if ((xmlRootElementAnnotation = value.getClass().getAnnotation(XmlRootElement.class)) == null) {
        throw new JAXBProcessingException("The class(%s) is missing a %s annotation", value.getClass().getName(), XmlRootElement.class.getSimpleName());
      } else {

        ObjectNode objectNode;
        Object proxyObject = ProxyGenerator.createProxy(value.getClass(), new OffloadingInvocationHandler(value), new AnnotationFilter(PassType.EXCLUDE, XmlJavaTypeAdapter.class));

        PolymorphicClassCache.addClassRelationship(value.getClass(), proxyObject.getClass());
        objectNode = (ObjectNode)JsonCodec.writeAsJsonNode(proxyObject);
        objectNode.put(getPolymorphicAttributeName(), xmlRootElementAnnotation.name());

        return objectNode;
      }
    }
  }
}