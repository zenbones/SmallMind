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
package org.smallmind.nutsnbolts.apt;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class AptUtility {

  public static AnnotationMirror extractAnnotationMirror (ProcessingEnvironment processingEnv, Element element, TypeMirror annotationTypeMirror) {

    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      if (processingEnv.getTypeUtils().isSameType(annotationTypeMirror, annotationMirror.getAnnotationType())) {

        return annotationMirror;
      }
    }

    return null;
  }

  public static AnnotationMirror[] extractAnnotationMirrors (ProcessingEnvironment processingEnv, Element element, TypeMirror annotationCollectionTypeMirror, TypeMirror annotationTypeMirror) {

    AnnotationMirror[] annotationMirrors;
    LinkedList<AnnotationMirror> annotationMirrorList = new LinkedList<>();

    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      if (processingEnv.getTypeUtils().isSameType(annotationTypeMirror, annotationMirror.getAnnotationType())) {
        annotationMirrorList.add(annotationMirror);
      } else if ((annotationCollectionTypeMirror != null) && processingEnv.getTypeUtils().isSameType(annotationCollectionTypeMirror, annotationMirror.getAnnotationType())) {

        List<AnnotationValue> childAnnotationValueList;

        if ((childAnnotationValueList = extractAnnotationValue(annotationMirror, "value", List.class, null)) != null) {
          for (AnnotationValue childAnnotationValue : childAnnotationValueList) {
            annotationMirrorList.add((AnnotationMirror)childAnnotationValue.getValue());
          }
        }
      }
    }

    annotationMirrors = new AnnotationMirror[annotationMirrorList.size()];
    annotationMirrorList.toArray(annotationMirrors);

    return annotationMirrors;
  }

  public static <T> T extractAnnotationValue (AnnotationMirror annotationMirror, String valueName, Class<T> clazz, T defaultValue) {

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> valueEntry : annotationMirror.getElementValues().entrySet()) {
      if (valueEntry.getKey().getSimpleName().contentEquals(valueName)) {

        if (clazz.isEnum()) {

          return (T)Enum.valueOf((Class<Enum>)clazz, valueEntry.getValue().getValue().toString());
        } else {

          return clazz.cast(valueEntry.getValue().getValue());
        }
      }
    }

    return defaultValue;
  }

  public static <T> List<T> extractAnnotationValueAsList (AnnotationMirror annotationMirror, String valueName, Class<T> itemClass) {

    List<T> extractedList = new LinkedList<>();
    List<AnnotationValue> annotationValueList;

    if ((annotationValueList = extractAnnotationValue(annotationMirror, valueName, List.class, null)) != null) {
      for (AnnotationValue annotationValue : annotationValueList) {
        extractedList.add(itemClass.cast(annotationValue.getValue()));
      }
    }

    return extractedList;
  }

  public static List<TypeElement> toConcreteList (ProcessingEnvironment processingEnv, List<TypeMirror> typeMirrorList) {

    LinkedList<TypeElement> typeElementList = new LinkedList<>();

    if (typeMirrorList != null) {
      for (TypeMirror typeMirror : typeMirrorList) {
        typeElementList.add((TypeElement)processingEnv.getTypeUtils().asElement(typeMirror));
      }
    }

    return typeElementList;
  }
}
