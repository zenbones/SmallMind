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
 * Utility methods for common annotation-processing tasks such as locating annotation mirrors and extracting typed values.
 */
public class AptUtility {

  /**
   * Finds the annotation mirror of the specified type on an element, returning {@code null} if not present.
   *
   * @param processingEnv        processing environment providing type utilities
   * @param element              element whose annotations are inspected
   * @param annotationTypeMirror annotation type to match against each annotation mirror
   * @return matching {@link AnnotationMirror}, or {@code null} if not found
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
   * Finds the first annotation on an element whose annotation type is itself annotated by the given meta-annotation type.
   *
   * @param processingEnv        processing environment providing type utilities
   * @param element              element whose annotations are inspected
   * @param annotationTypeMirror meta-annotation type that must appear on the candidate annotation's own type
   * @return first matching {@link AnnotationMirror}, or {@code null} if none found
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
   * Extracts a named element value from an annotation mirror, resolving enum constants by name and falling back to a default when the element is absent.
   *
   * @param annotationMirror annotation whose element values are searched
   * @param valueName        name of the annotation element to retrieve
   * @param clazz            expected runtime type; enum types are resolved via {@link Enum#valueOf}
   * @param defaultValue     value returned when the element is not explicitly set
   * @param <T>              declared return type
   * @return extracted value cast to {@code T}, or {@code defaultValue} when the element is not present
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
   * Extracts a named element value from an annotation mirror, including default values provided by the processing environment.
   *
   * @param processingEnvironment processing environment used to retrieve element values with defaults
   * @param annotationMirror      annotation whose element values are searched
   * @param valueName             name of the annotation element to retrieve
   * @param clazz                 expected runtime type; enum types are resolved via {@link Enum#valueOf}
   * @param <T>                   declared return type
   * @return extracted value cast to {@code T}, or {@code null} when the element cannot be resolved
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
   * Extracts a list-typed annotation element value, casting each item to the given type.
   *
   * @param annotationMirror annotation whose element values are searched
   * @param valueName        name of the annotation element whose value is a list
   * @param itemClass        expected runtime type of each list item
   * @param <T>              list element type
   * @return strongly typed list of extracted values, or an empty list if the element is absent
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
   * Converts a list of {@link TypeMirror}s to their corresponding {@link TypeElement}s in the same order.
   *
   * @param processingEnv  processing environment used to resolve mirrors to elements
   * @param typeMirrorList list of type mirrors to convert; {@code null} is treated as an empty list
   * @return ordered list of {@link TypeElement}s corresponding to the input mirrors
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
