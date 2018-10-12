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
package org.smallmind.web.json.dto;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import org.smallmind.nutsnbolts.apt.AptUtility;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class ClassWalker {

  public static void walk (ProcessingEnvironment processingEnvironment, DtoAnnotationProcessor dtoAnnotationProcessor, TypeElement classElement, GeneratorInformation generatorInformation, UsefulTypeMirrors usefulTypeMirrors)
    throws IOException, DtoDefinitionException {

    HashMap<String, ExecutableElement> setMethodMap = new HashMap<>();
    HashSet<Name> getMethodNameSet = new HashSet<>();
    HashSet<String> getFieldNameSet = new HashSet<>();
    HashSet<String> isFieldNameSet = new HashSet<>();

    for (Element enclosedElement : classElement.getEnclosedElements()) {
      if (enclosedElement.getModifiers().contains(javax.lang.model.element.Modifier.STATIC) && (enclosedElement.getAnnotation(DtoProperty.class) != null)) {
        throw new DtoDefinitionException("The element(%s) annotated as @%s may not be 'static'", enclosedElement.getSimpleName(), DtoProperty.class.getSimpleName());
      } else if (enclosedElement.getModifiers().contains(Modifier.ABSTRACT) && (enclosedElement.getAnnotation(DtoProperty.class) != null)) {
        throw new DtoDefinitionException("The element(%s) annotated as @%s may not be 'abstract'", enclosedElement.getSimpleName(), DtoProperty.class.getSimpleName());
      } else if (ElementKind.FIELD.equals(enclosedElement.getKind())) {
        dtoAnnotationProcessor.processTypeMirror(enclosedElement.asType());
      } else if (ElementKind.METHOD.equals(enclosedElement.getKind()) && enclosedElement.getModifiers().contains(javax.lang.model.element.Modifier.PUBLIC)) {

        String methodName = enclosedElement.getSimpleName().toString();

        if (methodName.startsWith("is") && (methodName.length() > 2) && Character.isUpperCase(methodName.charAt(2)) && ((ExecutableElement)enclosedElement).getParameters().isEmpty() && TypeKind.BOOLEAN.equals(((ExecutableElement)enclosedElement).getReturnType().getKind())) {

          AnnotationMirror dtoPropertyAnnotationMirror;
          String fieldName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);

          if ((dtoPropertyAnnotationMirror = AptUtility.extractAnnotationMirror(processingEnvironment, enclosedElement, usefulTypeMirrors.getDtoPropertyTypeMirror())) != null) {
            for (PropertyBox propertyBox : new PropertyParser(dtoPropertyAnnotationMirror, ((ExecutableElement)enclosedElement).getReturnType(), false)) {
              if (Visibility.IN.equals(propertyBox.getVisibility())) {
                throw new DtoDefinitionException("The 'is' method(%s) found in class(%s) can't be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              }

              generatorInformation.getOutDirectionalGuide().put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
            }
          }

          getMethodNameSet.add(enclosedElement.getSimpleName());
          isFieldNameSet.add(fieldName);
        } else if (methodName.startsWith("get") && (methodName.length() > 3) && Character.isUpperCase(methodName.charAt(3)) && ((ExecutableElement)enclosedElement).getParameters().isEmpty() && (!TypeKind.VOID.equals(((ExecutableElement)enclosedElement).getReturnType().getKind()))) {

          AnnotationMirror dtoPropertyAnnotationMirror;
          String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

          if ((dtoPropertyAnnotationMirror = AptUtility.extractAnnotationMirror(processingEnvironment, enclosedElement, usefulTypeMirrors.getDtoPropertyTypeMirror())) != null) {
            for (PropertyBox propertyBox : new PropertyParser(dtoPropertyAnnotationMirror, ((ExecutableElement)enclosedElement).getReturnType(), false)) {
              if (Visibility.IN.equals(propertyBox.getVisibility())) {
                throw new DtoDefinitionException("The 'get' method(%s) found in class(%s) can't be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              }

              dtoAnnotationProcessor.processTypeMirror(((ExecutableElement)enclosedElement).getReturnType());
              generatorInformation.getOutDirectionalGuide().put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
            }
          }

          getMethodNameSet.add(enclosedElement.getSimpleName());
          getFieldNameSet.add(fieldName);
        } else if (methodName.startsWith("set") && (methodName.length() > 3) && Character.isUpperCase(methodName.charAt(3)) && (((ExecutableElement)enclosedElement).getParameters().size() == 1)) {

          AnnotationMirror dtoPropertyAnnotationMirror;
          String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

          if ((dtoPropertyAnnotationMirror = AptUtility.extractAnnotationMirror(processingEnvironment, enclosedElement, usefulTypeMirrors.getDtoPropertyTypeMirror())) != null) {
            for (PropertyBox propertyBox : new PropertyParser(dtoPropertyAnnotationMirror, ((ExecutableElement)enclosedElement).getParameters().get(0).asType(), false)) {
              if (!Visibility.IN.equals(propertyBox.getVisibility())) {
                throw new DtoDefinitionException("The 'set' method(%s) found in class(%s) must be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              }

              dtoAnnotationProcessor.processTypeMirror(((ExecutableElement)enclosedElement).getParameters().get(0).asType());
              generatorInformation.getInDirectionalGuide().put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
            }
          }

          setMethodMap.put(fieldName, (ExecutableElement)enclosedElement);
        } else if (enclosedElement.getAnnotation(DtoProperty.class) != null) {
          throw new DtoDefinitionException("The method(%s) found in class(%s) must be a 'getter' or 'setter'", enclosedElement.getSimpleName(), classElement.getQualifiedName());
        }
      }
    }

    for (Element enclosedElement : classElement.getEnclosedElements()) {

      AnnotationMirror dtoPropertyAnnotationMirror;

      if ((dtoPropertyAnnotationMirror = AptUtility.extractAnnotationMirror(processingEnvironment, enclosedElement, usefulTypeMirrors.getDtoPropertyTypeMirror())) != null) {
        if (ElementKind.FIELD.equals(enclosedElement.getKind())) {
          for (PropertyBox propertyBox : new PropertyParser(dtoPropertyAnnotationMirror, enclosedElement.asType(), false)) {
            switch (propertyBox.getVisibility()) {
              case IN:
                addInField(classElement, generatorInformation, setMethodMap, enclosedElement.getSimpleName().toString(), propertyBox);
                break;
              case OUT:
                addOutField(classElement, generatorInformation, getFieldNameSet, isFieldNameSet, enclosedElement.getSimpleName().toString(), propertyBox);
                break;
              case BOTH:
                addInField(classElement, generatorInformation, setMethodMap, enclosedElement.getSimpleName().toString(), propertyBox);
                addOutField(classElement, generatorInformation, getFieldNameSet, isFieldNameSet, enclosedElement.getSimpleName().toString(), propertyBox);
                break;
              default:
                throw new UnknownSwitchCaseException(propertyBox.getVisibility().name());
            }
          }
        } else if (ElementKind.METHOD.equals(enclosedElement.getKind()) && getMethodNameSet.contains(enclosedElement.getSimpleName())) {

          String methodName = enclosedElement.getSimpleName().toString();
          String fieldName = methodName.startsWith("is") ? Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3) : Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

          for (PropertyBox propertyBox : new PropertyParser(dtoPropertyAnnotationMirror, ((ExecutableElement)enclosedElement).getReturnType(), false)) {
            if (Visibility.BOTH.equals(propertyBox.getVisibility())) {
              if (!setMethodMap.containsKey(fieldName)) {
                throw new DtoDefinitionException("The 'getter' method(%s) found in class(%s) must have a corresponding 'setter'", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              } else {
                dtoAnnotationProcessor.processTypeMirror(setMethodMap.get(fieldName).getParameters().get(0).asType());
                generatorInformation.getInDirectionalGuide().put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
              }
            }
          }
        }
      }
    }
  }

  private static void addInField (TypeElement classElement, GeneratorInformation generatorInformation, HashMap<String, ExecutableElement> setMethodMap, String fieldName, PropertyBox propertyBox)
    throws DtoDefinitionException {

    if (setMethodMap.containsKey(fieldName)) {
      generatorInformation.getInDirectionalGuide().put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
    } else {
      throw new DtoDefinitionException("The property field(%s) has no 'setter' method in class(%s)", fieldName, classElement.getQualifiedName());
    }
  }

  private static void addOutField (TypeElement classElement, GeneratorInformation generatorInformation, HashSet<String> getFieldNameSet, HashSet<String> isFieldNameSet, String fieldName, PropertyBox propertyBox)
    throws DtoDefinitionException {

    if (getFieldNameSet.contains(fieldName) || isFieldNameSet.contains(fieldName)) {
      generatorInformation.getOutDirectionalGuide().put(propertyBox.getPurpose(), fieldName, propertyBox.getPropertyInformation());
    } else {
      throw new DtoDefinitionException("The property field(%s) has no 'getter' method in class(%s)", fieldName, classElement.getQualifiedName());
    }
  }
}