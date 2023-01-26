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
package org.smallmind.persistence.orm.data.mongo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.smallmind.nutsnbolts.reflection.type.TypeUtility;

public class AnnotatedEntityModel {

  private final HashMap<Class<? extends Annotation>, HashMap<Field, List<Annotation>>> annotationMap = new HashMap<>();
  private final HashMap<Field, AnnotatedEntityModel> childMap = new HashMap<>();

  public AnnotatedEntityModel (Class<?> entityType) {

    for (Field field : obtainFields(entityType)) {
      for (Annotation annotation : field.getAnnotations()) {

        HashMap<Field, List<Annotation>> fieldMap;

        if ((fieldMap = annotationMap.get(annotation.annotationType())) == null) {
          annotationMap.put(annotation.annotationType(), fieldMap = new HashMap<>());
        }

        List<Annotation> annotationList;

        if ((annotationList = fieldMap.get(field)) == null) {
          fieldMap.put(field, annotationList = new LinkedList<>());
        }

        annotationList.add(annotation);
      }

      if (!(TypeUtility.isEssentiallyPrimitive(field.getType()) || String.class.equals(field.getType()) || isDateType(field.getType()))) {
        childMap.put(field, new AnnotatedEntityModel(field.getType()));
      }
    }
  }

  public void process (Class<? extends Annotation> annotationClass, Object value, MongoFieldProcessor fieldProcessor)
    throws Exception {

    HashMap<Field, List<Annotation>> fieldMap;

    if ((fieldMap = annotationMap.get(annotationClass)) != null) {
      for (Map.Entry<Field, List<Annotation>> fieldEntry : fieldMap.entrySet()) {
        for (Annotation annotation : fieldEntry.getValue()) {
          fieldProcessor.process(value, fieldEntry.getKey(), annotation);
        }
      }
    }

    for (Map.Entry<Field, AnnotatedEntityModel> childEntry : childMap.entrySet()) {
      childEntry.getValue().process(annotationClass, childEntry.getKey().get(value), fieldProcessor);
    }
  }

  private boolean isDateType (Class<?> fieldClass) {

    return Date.class.equals(fieldClass) ||
             Calendar.class.equals(fieldClass) ||
             Instant.class.equals(fieldClass) ||
             LocalTime.class.equals(fieldClass) ||
             LocalDate.class.equals(fieldClass) ||
             LocalDateTime.class.equals(fieldClass) ||
             ZonedDateTime.class.equals(fieldClass);
  }

  private LinkedList<Field> obtainFields (Class<?> entityClass) {

    Class<?> currentClass = entityClass;
    LinkedList<Field> fieldList = new LinkedList<>();

    do {
      for (Field field : currentClass.getDeclaredFields()) {
        if (!(field.isSynthetic() || Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))) {

          field.setAccessible(true);
          fieldList.add(field);
        }
      }
    } while ((currentClass = currentClass.getSuperclass()) != null);

    return fieldList;
  }
}
