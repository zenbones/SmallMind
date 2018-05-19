/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.web.json.dto.engine;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import com.google.auto.service.AutoService;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;

@SupportedAnnotationTypes({"org.smallmind.web.json.dto.engine.DtoGenerator"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"prefix"})
@AutoService(Processor.class)
public class DtoAnnotationProcessor extends AbstractProcessor {

  private final HashMap<TypeElement, Visibility> generatedMap = new HashMap<>();

  @Override
  public boolean process (Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(DtoGenerator.class)) {
      try {
        generate((TypeElement)annotatedElement);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return true;
  }

  public void generate (TypeElement classElement)
    throws IOException, DataDefinitionException {

    DtoGenerator dtoGenerator;

    if (((dtoGenerator = classElement.getAnnotation(DtoGenerator.class)) != null) && !generatedMap.containsKey(classElement)) {
      if ((!ElementKind.CLASS.equals(classElement.getKind())) || (!NestingKind.TOP_LEVEL.equals(classElement.getNestingKind()))) {
        throw new DataDefinitionException("The class(%s) must be a root implementation of type 'class'", classElement.getQualifiedName());
      } else {

        DtoClass dtoClass = new DtoClass(processingEnv, classElement);
        TypeElement nearestDtoSuperclass;
        boolean written = false;

        if ((nearestDtoSuperclass = getNearestDtoSuperclass(classElement)) != null) {
          generate(nearestDtoSuperclass);
        }

        for (Element enclosedElement : classElement.getEnclosedElements()) {

          DtoProperty dtoProperty;

          if ((dtoProperty = enclosedElement.getAnnotation(DtoProperty.class)) != null) {
            if (ElementKind.FIELD.equals(enclosedElement.getKind())) {
              switch (dtoProperty.visibility()) {
                case IN:
                  dtoClass.inField(((VariableElement)enclosedElement), dtoProperty);
                  break;
                case OUT:
                  dtoClass.outField(((VariableElement)enclosedElement), dtoProperty);
                  break;
                case BOTH:
                  dtoClass.inField(((VariableElement)enclosedElement), dtoProperty);
                  dtoClass.outField(((VariableElement)enclosedElement), dtoProperty);
                  break;
                default:
                  throw new UnknownSwitchCaseException(dtoProperty.visibility().name());
              }
            } else if (ElementKind.METHOD.equals(enclosedElement.getKind()) && Visibility.BOTH.equals(dtoProperty.visibility()) && (!dtoClass.hasSetter((ExecutableElement)enclosedElement))) {
              throw new DataDefinitionException("Missing 'setter' method(%s) in class(%s)", enclosedElement.getSimpleName(), classElement.getQualifiedName());
            }
          }
        }

        if (!dtoClass.getInMap().isEmpty()) {
          write(dtoGenerator, classElement, nearestDtoSuperclass, Direction.IN, dtoClass.getInMap());

          generatedMap.put(classElement, Visibility.IN);
          written = true;
        }
        if (!dtoClass.getOutMap().isEmpty()) {
          write(dtoGenerator, classElement, nearestDtoSuperclass, Direction.OUT, dtoClass.getOutMap());

          if (Visibility.IN.equals(generatedMap.get(classElement))) {
            generatedMap.put(classElement, Visibility.BOTH);
          } else {
            generatedMap.put(classElement, Visibility.OUT);
          }
          written = true;
        }

        if (!written) {
          throw new DataDefinitionException("The class(%s) was annotated as '%s' but contained no properties", classElement.getQualifiedName(), DtoGenerator.class.getSimpleName());
        }
      }
    }
  }

  private String asMemberName (Name name) {

    return Character.toLowerCase(name.charAt(0)) + name.subSequence(1, name.length()).toString();
  }

  private StringBuilder asDtoName (Name simpleName, Direction direction) {

    return new StringBuilder((processingEnv.getOptions().get("prefix") == null) ? "" : processingEnv.getOptions().get("prefix")).append(simpleName).append(direction.getCode()).append("Dto");
  }

  private String asCompatibleName (TypeMirror typeMirror, Direction direction) {

    if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

      Element element;

      if (ElementKind.CLASS.equals((element = processingEnv.getTypeUtils().asElement(typeMirror)).getKind())) {

        Visibility visibility;

        if (((visibility = generatedMap.get((TypeElement)element)) != null) && visibility.matches(direction)) {
          return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString() + '.' + asDtoName(element.getSimpleName(), direction).toString();
        }
      }
    }

    return processingEnv.getTypeUtils().asElement(typeMirror).getSimpleName().toString();
  }

  private boolean isDtoType (TypeMirror typeMirror, Direction direction) {

    if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

      Element element;

      return ElementKind.CLASS.equals((element = processingEnv.getTypeUtils().asElement(typeMirror)).getKind()) && generatedMap.containsKey((TypeElement)element);
    }

    return false;
  }

  private TypeElement getNearestDtoSuperclass (TypeElement classElement) {

    TypeElement currentClassElement = classElement;

    while ((currentClassElement = (TypeElement)processingEnv.getTypeUtils().asElement(currentClassElement.getSuperclass())) != null) {
      if (currentClassElement.getAnnotation(DtoGenerator.class) != null) {

        return currentClassElement;
      }
    }

    return null;
  }

  private void write (DtoGenerator dtoGenerator, TypeElement classElement, TypeElement nearestDtoSuperclass, Direction direction, HashMap<String, PropertyInfo> propertyMap)
    throws IOException {

    try (Writer writer = processingEnv.getFiler().createSourceFile(new StringBuilder(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName()).append('.').append(asDtoName(classElement.getSimpleName(), direction))).openWriter()) {
      boolean firstPair = true;

      // package
      writer.write("package ");
      writer.write(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString());
      writer.write(";");
      writer.write("\n");
      writer.write("\n");

      // imports
      writer.write("import javax.annotation.Generated;");
      writer.write("\n");
      writer.write("import javax.xml.bind.annotation.XmlAccessType;");
      writer.write("\n");
      writer.write("import javax.xml.bind.annotation.XmlAccessorType;");
      writer.write("\n");
      writer.write("import javax.xml.bind.annotation.XmlElement;");
      writer.write("\n");
      writer.write("import javax.xml.bind.annotation.XmlRootElement;");
      writer.write("\n");
      writer.write("import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;");
      writer.write("\n");

      if ((nearestDtoSuperclass != null) && (!Objects.equals(processingEnv.getElementUtils().getPackageOf(classElement), processingEnv.getElementUtils().getPackageOf(nearestDtoSuperclass)))) {
        writer.write("import ");
        writer.write(processingEnv.getElementUtils().getPackageOf(nearestDtoSuperclass).getQualifiedName().toString());
        writer.write(".");
        writer.write(asDtoName(nearestDtoSuperclass.getSimpleName(), direction).toString());
        writer.write(";");
        writer.write("\n");
      }
      writer.write("\n");

      // @Generated
      writer.write("@Generated(\"");
      writer.write(DtoEngine.class.getName());
      writer.write("\")");
      writer.write("\n");

      // @XmlRootElement
      writer.write("@XmlRootElement(name = \"");
      writer.write(dtoGenerator.name().isEmpty() ? asMemberName(classElement.getSimpleName()) : dtoGenerator.name());
      writer.write("\")");
      writer.write("\n");

      // XmlAccessorType
      writer.write("@XmlAccessorType(XmlAccessType.PROPERTY)");
      writer.write("\n");

      // class declaration
      writer.write("public ");
      if (classElement.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT)) {
        writer.write("abstract ");
      }
      writer.write("class ");
      writer.write(asDtoName(classElement.getSimpleName(), direction).toString());
      if (nearestDtoSuperclass != null) {
        writer.write(" extends ");
        writer.write(asDtoName(nearestDtoSuperclass.getSimpleName(), direction).toString());
      }
      writer.write(" {");
      writer.write("\n");
      writer.write("\n");

      // field declarations
      for (Map.Entry<String, PropertyInfo> propertyInfoEntry : propertyMap.entrySet()) {
        writer.write("  private ");
        writer.write(asCompatibleName(propertyInfoEntry.getValue().getTypeMirror(), direction));
        writer.write(" ");
        writer.write(propertyInfoEntry.getKey());
        writer.write(";");
        writer.write("\n");
      }
      writer.write("\n");

      // constructors
      writer.write("  public ");
      writer.write(asDtoName(classElement.getSimpleName(), direction).toString());
      writer.write(" () {");
      writer.write("\n");
      writer.write("  }");
      writer.write("\n");
      writer.write("\n");

      writer.write("  public ");
      writer.write(asDtoName(classElement.getSimpleName(), direction).toString());
      writer.write(" (");
      writer.write(classElement.getSimpleName().toString());
      writer.write(" ");
      writer.write(asMemberName(classElement.getSimpleName()));
      writer.write(") {");
      writer.write("\n");
      writer.write("\n");
      for (Map.Entry<String, PropertyInfo> propertyInfoEntry : propertyMap.entrySet()) {
        writer.write("    this.");
        writer.write(propertyInfoEntry.getKey());
        writer.write(" = ");
        if (isDtoType(propertyInfoEntry.getValue().getTypeMirror(), direction)) {
          writer.write("new ");
          writer.write(asCompatibleName(propertyInfoEntry.getValue().getTypeMirror(), direction));
          writer.write("(");
        }
        writer.write(asMemberName(classElement.getSimpleName()));
        writer.write(".");
        writer.write(TypeKind.BOOLEAN.equals(propertyInfoEntry.getValue().getTypeMirror().getKind()) ? BeanUtility.asIsName(propertyInfoEntry.getKey()) : BeanUtility.asGetterName(propertyInfoEntry.getKey()));
        writer.write("()");
        if (isDtoType(propertyInfoEntry.getValue().getTypeMirror(), direction)) {
          writer.write(")");
        }
        writer.write(";");
        writer.write("\n");
      }
      writer.write("  }");
      writer.write("\n");
      writer.write("\n");

      // entity factory
      writer.write("  public ");
      writer.write(classElement.getSimpleName().toString());
      writer.write(" factory () {");
      writer.write("\n");
      writer.write("\n");
      writer.write("    ");
      writer.write(classElement.getSimpleName().toString());
      writer.write(" ");
      writer.write(asMemberName(classElement.getSimpleName()));
      writer.write(" = new ");
      writer.write(classElement.getSimpleName().toString());
      writer.write("();");
      writer.write("\n");
      writer.write("\n");
      for (Map.Entry<String, PropertyInfo> propertyInfoEntry : propertyMap.entrySet()) {
        writer.write("    ");
        writer.write(asMemberName(classElement.getSimpleName()));
        writer.write(".");
        writer.write(BeanUtility.asSetterName(propertyInfoEntry.getKey()));
        writer.write("(");
        writer.write(propertyInfoEntry.getKey());
        writer.write(");");
        writer.write("\n");
      }
      writer.write("  }");
      writer.write("\n");
      writer.write("\n");

      // getters and setters
      for (Map.Entry<String, PropertyInfo> propertyInfoEntry : propertyMap.entrySet()) {
        if (!firstPair) {
          writer.write("\n");
        }

        writer.write("  @XmlElement(name = \"");
        writer.write(propertyInfoEntry.getValue().getDtoProperty().name().isEmpty() ? propertyInfoEntry.getKey() : dtoGenerator.name());
        writer.write(propertyInfoEntry.getValue().getDtoProperty().required() ? "\", required = true)" : "\")");
        writer.write("\n");
        writer.write("  public ");
        writer.write(asCompatibleName(propertyInfoEntry.getValue().getTypeMirror(), direction));
        writer.write(" ");
        writer.write(boolean.class.equals(propertyInfoEntry.getValue()) ? BeanUtility.asIsName(propertyInfoEntry.getKey()) : BeanUtility.asGetterName(propertyInfoEntry.getKey()));
        writer.write("() {");
        writer.write("\n");
        writer.write("\n");
        writer.write("    return ");
        writer.write(propertyInfoEntry.getKey());
        writer.write(";");
        writer.write("\n");
        writer.write("  }");
        writer.write("\n");
        writer.write("\n");

        writer.write("  public void ");
        writer.write(BeanUtility.asSetterName(propertyInfoEntry.getKey()));
        writer.write("(");
        writer.write(asCompatibleName(propertyInfoEntry.getValue().getTypeMirror(), direction));
        writer.write(" ");
        writer.write(propertyInfoEntry.getKey());
        writer.write(") {");
        writer.write("\n");
        writer.write("\n");
        writer.write("    this.");
        writer.write(propertyInfoEntry.getKey());
        writer.write(" = ");
        writer.write(propertyInfoEntry.getKey());
        writer.write(";");
        writer.write("\n");
        writer.write("  }");
        writer.write("\n");

        firstPair = false;
      }

      writer.write("}");
      writer.write("\n");
    }
  }

  private class PropertyInfo {

    private DtoProperty dtoProperty;
    private TypeMirror typeMirror;

    private PropertyInfo (TypeMirror typeMirror, DtoProperty dtoProperty) {

      this.typeMirror = typeMirror;
      this.dtoProperty = dtoProperty;
    }

    public TypeMirror getTypeMirror () {

      return typeMirror;
    }

    public DtoProperty getDtoProperty () {

      return dtoProperty;
    }
  }

  private class DtoClass {

    private HashMap<String, PropertyInfo> inMap = new HashMap<>();
    private HashMap<String, PropertyInfo> outMap = new HashMap<>();
    private HashSet<Name> setMethodNameSet = new HashSet<>();
    private HashSet<String> isFieldNameSet = new HashSet<>();
    private HashSet<String> getFieldNameSet = new HashSet<>();
    private HashSet<String> setFieldNameSet = new HashSet<>();
    private TypeElement classElement;

    private DtoClass (ProcessingEnvironment processingEnv, TypeElement classElement)
      throws IOException, DataDefinitionException {

      this.classElement = classElement;

      for (Element enclosedElement : classElement.getEnclosedElements()) {
        if (!enclosedElement.getModifiers().contains(javax.lang.model.element.Modifier.STATIC)) {
          if (ElementKind.FIELD.equals(enclosedElement.getKind())) {
            processTypeMirror(enclosedElement.asType());
          } else if (ElementKind.METHOD.equals(enclosedElement.getKind()) && enclosedElement.getModifiers().contains(javax.lang.model.element.Modifier.PUBLIC)) {

            String methodName = enclosedElement.getSimpleName().toString();

            if (methodName.startsWith("is") && (methodName.length() > 2) && ((ExecutableElement)enclosedElement).getParameters().isEmpty() && TypeKind.BOOLEAN.equals(((ExecutableElement)enclosedElement).getReturnType().getKind())) {

              DtoProperty dtoProperty;
              String fieldName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);

              if ((dtoProperty = enclosedElement.getAnnotation(DtoProperty.class)) != null) {
                if (Visibility.IN.equals(dtoProperty.visibility())) {
                  throw new DataDefinitionException("The 'is' method(%s) found in class(%s) can't be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
                }

                outMap.put(fieldName, new PropertyInfo(((ExecutableElement)enclosedElement).getReturnType(), dtoProperty));
              }

              isFieldNameSet.add(fieldName);
            } else if (methodName.startsWith("get") && (methodName.length() > 3) && ((ExecutableElement)enclosedElement).getParameters().isEmpty()) {

              DtoProperty dtoProperty;
              String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

              if ((dtoProperty = enclosedElement.getAnnotation(DtoProperty.class)) != null) {
                if (Visibility.IN.equals(dtoProperty.visibility())) {
                  throw new DataDefinitionException("The 'get' method(%s) found in class(%s) can't be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
                }

                processTypeMirror(((ExecutableElement)enclosedElement).getReturnType());
                outMap.put(fieldName, new PropertyInfo(((ExecutableElement)enclosedElement).getReturnType(), dtoProperty));
              }

              getFieldNameSet.add(fieldName);
            } else if (methodName.startsWith("set") && (methodName.length() > 3) && (((ExecutableElement)enclosedElement).getParameters().size() == 1)) {

              DtoProperty dtoProperty;
              String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

              if ((dtoProperty = enclosedElement.getAnnotation(DtoProperty.class)) != null) {
                if (!Visibility.IN.equals(dtoProperty.visibility())) {
                  throw new DataDefinitionException("The 'set' method(%s) found in class(%s) must be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
                }

                processTypeMirror(((ExecutableElement)enclosedElement).getParameters().get(0).asType());
                inMap.put(fieldName, new PropertyInfo(((ExecutableElement)enclosedElement).getParameters().get(0).asType(), dtoProperty));
              }

              setMethodNameSet.add(enclosedElement.getSimpleName());
              setFieldNameSet.add(fieldName);
            } else if (enclosedElement.getAnnotation(DtoProperty.class) != null) {
              throw new DataDefinitionException("The method(%s) found in class(%s) must be a 'getter' or 'setter'", enclosedElement.getSimpleName(), classElement.getQualifiedName());
            }
          }
        }
      }
    }

    private void processTypeMirror (TypeMirror typeMirror)
      throws IOException, DataDefinitionException {

      if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

        Element element;

        if (ElementKind.CLASS.equals((element = processingEnv.getTypeUtils().asElement(typeMirror)).getKind())) {
          generate((TypeElement)element);
        }
      }
    }

    public HashMap<String, PropertyInfo> getInMap () {

      return inMap;
    }

    public HashMap<String, PropertyInfo> getOutMap () {

      return outMap;
    }

    public boolean hasSetter (ExecutableElement methodElement) {

      return setMethodNameSet.contains(methodElement.getSimpleName());
    }

    public void inField (VariableElement fieldElement, DtoProperty dtoProperty)
      throws DataDefinitionException {

      if (setFieldNameSet.contains(fieldElement.getSimpleName().toString())) {
        inMap.put(fieldElement.getSimpleName().toString(), new PropertyInfo(fieldElement.asType(), dtoProperty));
      } else {
        throw new DataDefinitionException("The property field(%s) has no 'setter' method in class(%s)", fieldElement.getSimpleName(), classElement.getQualifiedName());
      }
    }

    public void outField (VariableElement fieldElement, DtoProperty dtoProperty)
      throws DataDefinitionException {

      if (getFieldNameSet.contains(fieldElement.getSimpleName().toString()) || isFieldNameSet.contains(fieldElement.getSimpleName().toString())) {
        outMap.put(fieldElement.getSimpleName().toString(), new PropertyInfo(fieldElement.asType(), dtoProperty));
      } else {
        throw new DataDefinitionException("The property field(%s) has no 'getter' method in class(%s)", fieldElement.getSimpleName(), classElement.getQualifiedName());
      }
    }
  }
}