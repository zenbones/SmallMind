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

/**
 * Helper methods for common annotation-processing operations such as locating annotations and extracting typed values.
 */
public class AptUtility {

  /**
   * Finds the annotation mirror of the specified type on an element.
   *
   * @param processingEnv        processing environment providing type utilities
   * @param element              element being inspected
   * @param annotationTypeMirror annotation type to match
   * @return matching annotation mirror or {@code null} if absent
   */
  public static AnnotationMirror extractAnnotationMirror (ProcessingEnvironment processingEnv, Element element, TypeMirror annotationTypeMirror) {

    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      if (processingEnv.getTypeUtils().isSameType(annotationTypeMirror, annotationMirror.getAnnotationType())) {

        return annotationMirror;
      }
    }

    return null;
  }

  /**
   * Locates an annotation whose own annotation type is annotated by the supplied annotation type.
   *
   * @param processingEnv        processing environment providing type utilities
   * @param element              element being inspected
   * @param annotationTypeMirror annotation type that must be present on the candidate annotation
   * @return first matching annotation mirror or {@code null} if none
   */
  public static AnnotationMirror extractAnnotationMirrorAnnotatedBy (ProcessingEnvironment processingEnv, Element element, TypeMirror annotationTypeMirror) {

    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      for (AnnotationMirror subAnnotationMirror : annotationMirror.getAnnotationType().asElement().getAnnotationMirrors()) {
        if (processingEnv.getTypeUtils().isSameType(annotationTypeMirror, subAnnotationMirror.getAnnotationType())) {

          return annotationMirror;
        }
      }
    }

    return null;
  }

  /**
   * Extracts a named value from an annotation, returning a default if the value is not present.
   *
   * @param annotationMirror annotation to inspect
   * @param valueName        name of the value to retrieve
   * @param clazz            expected value type; if an enum the value is resolved by name
   * @param defaultValue     fallback when the value is not set
   * @param <T>              value type
   * @return extracted value or {@code defaultValue} when missing
   */
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

  /**
   * Extracts a named value from an annotation, consulting default values from the processing environment.
   *
   * @param processingEnvironment processing utilities used to materialize defaults
   * @param annotationMirror      annotation to inspect
   * @param valueName             name of the value to retrieve
   * @param clazz                 expected value type; if an enum the value is resolved by name
   * @param <T>                   value type
   * @return extracted value or {@code null} when the value is unavailable
   */
  public static <T> T extractAnnotationValueWithDefault (ProcessingEnvironment processingEnvironment, AnnotationMirror annotationMirror, String valueName, Class<T> clazz) {

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> valueEntry : processingEnvironment.getElementUtils().getElementValuesWithDefaults(annotationMirror).entrySet()) {
      if (valueEntry.getKey().getSimpleName().contentEquals(valueName)) {

        if (clazz.isEnum()) {

          return (T)Enum.valueOf((Class<Enum>)clazz, valueEntry.getValue().getValue().toString());
        } else {

          return clazz.cast(valueEntry.getValue().getValue());
        }
      }
    }

    return null;
  }

  /**
   * Reads a list-typed annotation value into a strongly typed list.
   *
   * @param annotationMirror annotation to inspect
   * @param valueName        name of the value to retrieve
   * @param itemClass        expected class of each list element
   * @param <T>              list element type
   * @return list of extracted values (empty if the value is not present)
   */
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

  /**
   * Converts a list of {@link TypeMirror}s to their corresponding {@link TypeElement}s.
   *
   * @param processingEnv  processing environment providing type utilities
   * @param typeMirrorList list of type mirrors, possibly {@code null}
   * @return list of concrete type elements in the same order as the input
   */
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
