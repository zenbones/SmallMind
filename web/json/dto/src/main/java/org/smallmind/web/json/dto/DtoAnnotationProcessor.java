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
package org.smallmind.web.json.dto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import com.google.auto.service.AutoService;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;

@SupportedAnnotationTypes({"org.smallmind.web.json.dto.DtoGenerator"})
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
      } catch (Exception exception) {
        exception.printStackTrace();
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
      }
    }

    return true;
  }

  private void generate (TypeElement classElement)
    throws IOException, DtoDefinitionException {

    AnnotationMirror dtoGeneratorMirror;

    if (!generatedMap.containsKey(classElement) && ((dtoGeneratorMirror = extractAnnotationMirror(classElement, processingEnv.getElementUtils().getTypeElement(DtoGenerator.class.getName()).asType())) != null)) {
      if ((!ElementKind.CLASS.equals(classElement.getKind())) || (!NestingKind.TOP_LEVEL.equals(classElement.getNestingKind()))) {
        throw new DtoDefinitionException("The class(%s) must be a root implementation of type 'class'", classElement.getQualifiedName());
      } else {

        UsefulTypeMirrors usefulTypeMirrors = new UsefulTypeMirrors();
        GeneratorInformation generatorInformation = new GeneratorInformation(dtoGeneratorMirror, usefulTypeMirrors);
        DtoClass dtoClass = new DtoClass(classElement, usefulTypeMirrors);
        TypeElement nearestDtoSuperclass;
        boolean written = false;

        if ((nearestDtoSuperclass = getNearestDtoSuperclass(classElement)) != null) {
          generate(nearestDtoSuperclass);
        }

        for (Map.Entry<String, HashMap<String, PropertyInformation>> purposeEntry : dtoClass.getInMap().entrySet()) {
          write(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purposeEntry.getKey(), Direction.IN, purposeEntry.getValue());

          generatedMap.put(classElement, Visibility.IN);
          written = true;
        }
        for (Map.Entry<String, HashMap<String, PropertyInformation>> purposeEntry : dtoClass.getOutMap().entrySet()) {
          write(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purposeEntry.getKey(), Direction.OUT, purposeEntry.getValue());

          if (Visibility.IN.equals(generatedMap.get(classElement))) {
            generatedMap.put(classElement, Visibility.BOTH);
          } else {
            generatedMap.put(classElement, Visibility.OUT);
          }
          written = true;
        }

        if (!written) {
          throw new DtoDefinitionException("The class(%s) was annotated as @%s but contained no properties", classElement.getQualifiedName(), DtoGenerator.class.getSimpleName());
        }
      }
    }
  }

  private AnnotationMirror extractAnnotationMirror (Element element, TypeMirror annotationTypeMirror) {

    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      if (processingEnv.getTypeUtils().isSameType(annotationTypeMirror, annotationMirror.getAnnotationType())) {

        return annotationMirror;
      }
    }

    return null;
  }

  private AnnotationMirror[] extractAnnotationMirrors (Element element, TypeMirror annotationCollectionTypeMirror, TypeMirror annotationTypeMirror) {

    AnnotationMirror[] annotationMirrors;
    LinkedList<AnnotationMirror> annotationMirrorList = new LinkedList<>();

    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      if (processingEnv.getTypeUtils().isSameType(annotationTypeMirror, annotationMirror.getAnnotationType())) {
        annotationMirrorList.add(annotationMirror);
      } else if ((annotationCollectionTypeMirror != null) && processingEnv.getTypeUtils().isSameType(annotationCollectionTypeMirror, annotationMirror.getAnnotationType())) {

        List<AnnotationValue> childAnnotationValueList;

        if ((childAnnotationValueList = Collections.checkedList(extractAnnotationValue(annotationMirror, "value", List.class, null), AnnotationValue.class)) != null) {
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

  private <T> T extractAnnotationValue (AnnotationMirror annotationMirror, String valueName, Class<T> clazz, T defaultVlaue) {

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> valueEntry : annotationMirror.getElementValues().entrySet()) {
      if (valueEntry.getKey().getSimpleName().contentEquals(valueName)) {

        return clazz.cast(valueEntry.getValue().getValue());
      }
    }

    return defaultVlaue;
  }

  private String asMemberName (Name name) {

    return Character.toLowerCase(name.charAt(0)) + name.subSequence(1, name.length()).toString();
  }

  private StringBuilder asDtoName (Name simpleName, String purpose, Direction direction) {

    return new StringBuilder((processingEnv.getOptions().get("prefix") == null) ? "" : processingEnv.getOptions().get("prefix")).append(simpleName).append(purpose).append(direction.getCode()).append("Dto");
  }

  private String asCompatibleName (TypeMirror typeMirror, String purpose, Direction direction) {

    if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

      Element element;

      if (ElementKind.CLASS.equals((element = processingEnv.getTypeUtils().asElement(typeMirror)).getKind())) {

        Visibility visibility;

        if (((visibility = generatedMap.get((TypeElement)element)) != null) && visibility.matches(direction)) {

          return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString() + '.' + asDtoName(element.getSimpleName(), purpose, direction).toString();
        }
      }
    }

    return typeMirror.toString();
  }

  private boolean isDtoType (TypeMirror typeMirror, Direction direction) {

    if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

      Element element;

      if (ElementKind.CLASS.equals((element = processingEnv.getTypeUtils().asElement(typeMirror)).getKind())) {

        Visibility visibility;

        return ((visibility = generatedMap.get((TypeElement)element)) != null) && visibility.matches(direction);
      }
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

  private void write (GeneratorInformation generatorInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestDtoSuperclass, String purpose, Direction direction, HashMap<String, PropertyInformation> propertyMap)
    throws IOException {

    JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(new StringBuilder(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName()).append('.').append(asDtoName(classElement.getSimpleName(), purpose, direction)), classElement);

    if (sourceFile.getNestingKind() == null) {
      try (BufferedWriter writer = new BufferedWriter(sourceFile.openWriter())) {

        boolean firstPair = true;

        // package
        writer.write("package ");
        writer.write(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString());
        writer.write(";");
        writer.newLine();
        writer.newLine();

        // imports
        writer.write("import javax.annotation.Generated;");
        writer.newLine();
        writer.write("import javax.xml.bind.annotation.XmlAccessType;");
        writer.newLine();
        writer.write("import javax.xml.bind.annotation.XmlAccessorType;");
        writer.newLine();
        writer.write("import javax.xml.bind.annotation.XmlElement;");
        writer.newLine();
        writer.write("import javax.xml.bind.annotation.XmlRootElement;");
        writer.newLine();
        writer.write("import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;");
        writer.newLine();

        if ((nearestDtoSuperclass != null) && (!Objects.equals(processingEnv.getElementUtils().getPackageOf(classElement), processingEnv.getElementUtils().getPackageOf(nearestDtoSuperclass)))) {
          writer.write("import ");
          writer.write(processingEnv.getElementUtils().getPackageOf(nearestDtoSuperclass).getQualifiedName().toString());
          writer.write(".");
          writer.write(asDtoName(nearestDtoSuperclass.getSimpleName(), purpose, direction).toString());
          writer.write(";");
          writer.newLine();
        }
        writer.newLine();

        // @Generated
        writer.write("@Generated(\"");
        writer.write(DtoAnnotationProcessor.class.getName());
        writer.write("\")");
        writer.newLine();

        // @XmlRootElement
        writer.write("@XmlRootElement(name = \"");
        writer.write(generatorInformation.getName().isEmpty() ? asMemberName(classElement.getSimpleName()) : generatorInformation.getName());
        writer.write("\")");
        writer.newLine();

        // XmlAccessorType
        writer.write("@XmlAccessorType(XmlAccessType.PROPERTY)");
        writer.newLine();

        // class declaration
        writer.write("public ");
        if (classElement.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT)) {
          writer.write("abstract ");
        }
        writer.write("class ");
        writer.write(asDtoName(classElement.getSimpleName(), purpose, direction).toString());
        if (nearestDtoSuperclass != null) {
          writer.write(" extends ");
          writer.write(asDtoName(nearestDtoSuperclass.getSimpleName(), purpose, direction).toString());
        }
        writer.write(" {");
        writer.newLine();
        writer.newLine();

        // field declarations
        for (Map.Entry<String, PropertyInformation> propertyMirrorEntry : propertyMap.entrySet()) {
          writer.write("  private ");
          writer.write(asCompatibleName(propertyMirrorEntry.getValue().getType(), purpose, direction));
          writer.write(" ");
          writer.write(propertyMirrorEntry.getKey());
          writer.write(";");
          writer.newLine();
        }
        writer.newLine();

        // constructors
        writer.write("  public ");
        writer.write(asDtoName(classElement.getSimpleName(), purpose, direction).toString());
        writer.write(" () {");
        writer.newLine();
        writer.write("  }");
        writer.newLine();
        writer.newLine();

        writer.write("  public ");
        writer.write(asDtoName(classElement.getSimpleName(), purpose, direction).toString());
        writer.write(" (");
        writer.write(classElement.getSimpleName().toString());
        writer.write(" ");
        writer.write(asMemberName(classElement.getSimpleName()));
        writer.write(") {");
        writer.newLine();
        writer.newLine();
        if (nearestDtoSuperclass != null) {
          writer.write("    super(");
          writer.write(asMemberName(classElement.getSimpleName()));
          writer.write(");");
          writer.newLine();
          writer.newLine();
        }
        for (Map.Entry<String, PropertyInformation> propertyMirrorEntry : propertyMap.entrySet()) {
          writer.write("    this.");
          writer.write(propertyMirrorEntry.getKey());
          writer.write(" = ");
          if (isDtoType(propertyMirrorEntry.getValue().getType(), direction)) {
            writer.write("new ");
            writer.write(asCompatibleName(propertyMirrorEntry.getValue().getType(), purpose, direction));
            writer.write("(");
          }
          writer.write(asMemberName(classElement.getSimpleName()));
          writer.write(".");
          writer.write(TypeKind.BOOLEAN.equals(propertyMirrorEntry.getValue().getType().getKind()) ? BeanUtility.asIsName(propertyMirrorEntry.getKey()) : BeanUtility.asGetterName(propertyMirrorEntry.getKey()));
          writer.write("()");
          if (isDtoType(propertyMirrorEntry.getValue().getType(), direction)) {
            writer.write(")");
          }
          writer.write(";");
          writer.newLine();
        }
        writer.write("  }");
        writer.newLine();
        writer.newLine();

        // entity factory
        if (!classElement.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT)) {
          writer.write("  public ");
          writer.write(classElement.getSimpleName().toString());
          writer.write(" factory () {");
          writer.newLine();
          writer.newLine();
          writer.write("    return factory(new ");
          writer.write(classElement.getQualifiedName().toString());
          writer.write("());");
          writer.newLine();
          writer.write("  }");
          writer.newLine();
          writer.newLine();
        }

        writer.write("  public ");
        writer.write(classElement.getSimpleName().toString());
        writer.write(" factory (");
        writer.write(classElement.getSimpleName().toString());
        writer.write(" ");
        writer.write(asMemberName(classElement.getSimpleName()));
        writer.write(") {");
        writer.newLine();
        if (nearestDtoSuperclass != null) {
          writer.newLine();
          writer.write("    super.factory(");
          writer.write(asMemberName(classElement.getSimpleName()));
          writer.write(");");
          writer.newLine();
        }
        for (Map.Entry<String, PropertyInformation> propertyMirrorEntry : propertyMap.entrySet()) {
          writer.newLine();
          writer.write("    ");
          writer.write(asMemberName(classElement.getSimpleName()));
          writer.write(".");
          writer.write(BeanUtility.asSetterName(propertyMirrorEntry.getKey()));
          writer.write("(");
          writer.write(propertyMirrorEntry.getKey());
          if (isDtoType(propertyMirrorEntry.getValue().getType(), direction)) {
            writer.write(".factory(");
            writer.write("new ");
            writer.write(processingEnv.getTypeUtils().asElement(propertyMirrorEntry.getValue().getType()).getSimpleName().toString());
            writer.write("())");
          }
          writer.write(");");
        }
        writer.newLine();
        writer.newLine();
        writer.write("    return ");
        writer.write(asMemberName(classElement.getSimpleName()));
        writer.write(";");
        writer.newLine();
        writer.write("  }");
        writer.newLine();
        writer.newLine();

        // getters and setters
        for (Map.Entry<String, PropertyInformation> propertyMirrorEntry : propertyMap.entrySet()) {
          if (!firstPair) {
            writer.newLine();
          }

          if (!processingEnv.getTypeUtils().isSameType(usefulTypeMirrors.getDefaultXmlAdapterTypeMirror(), propertyMirrorEntry.getValue().getAdapter())) {
            writer.write("  @XmlJavaTypeAdapter(");
            writer.write(propertyMirrorEntry.getValue().getAdapter().toString());
            writer.write(".class)");
            writer.newLine();
          }
          writer.write("  @XmlElement(name = \"");
          writer.write(propertyMirrorEntry.getValue().getName().isEmpty() ? propertyMirrorEntry.getKey() : propertyMirrorEntry.getValue().getName());
          writer.write(propertyMirrorEntry.getValue().isRequired() ? "\", required = true)" : "\")");
          writer.newLine();
          writer.write("  public ");
          writer.write(asCompatibleName(propertyMirrorEntry.getValue().getType(), purpose, direction));
          writer.write(" ");
          writer.write(TypeKind.BOOLEAN.equals(propertyMirrorEntry.getValue().getType().getKind()) ? BeanUtility.asIsName(propertyMirrorEntry.getKey()) : BeanUtility.asGetterName(propertyMirrorEntry.getKey()));
          writer.write(" () {");
          writer.newLine();
          writer.newLine();
          writer.write("    return ");
          writer.write(propertyMirrorEntry.getKey());
          writer.write(";");
          writer.newLine();
          writer.write("  }");
          writer.newLine();
          writer.newLine();

          writer.write("  public void ");
          writer.write(BeanUtility.asSetterName(propertyMirrorEntry.getKey()));
          writer.write(" (");
          writer.write(asCompatibleName(propertyMirrorEntry.getValue().getType(), purpose, direction));
          writer.write(" ");
          writer.write(propertyMirrorEntry.getKey());
          writer.write(") {");
          writer.newLine();
          writer.newLine();
          writer.write("    this.");
          writer.write(propertyMirrorEntry.getKey());
          writer.write(" = ");
          writer.write(propertyMirrorEntry.getKey());
          writer.write(";");
          writer.newLine();
          writer.write("  }");
          writer.newLine();

          firstPair = false;
        }

        writer.write("}");
        writer.newLine();
      }
    }
  }

  private class UsefulTypeMirrors {

    TypeMirror dtoPropertyTypeMirror = processingEnv.getElementUtils().getTypeElement(DtoProperty.class.getName()).asType();
    TypeMirror dtoPropertiesTypeMirror = processingEnv.getElementUtils().getTypeElement(DtoProperties.class.getName()).asType();
    TypeMirror defaultXmlAdapterTypeMirror = processingEnv.getElementUtils().getTypeElement(DefaultXmlAdapter.class.getName()).asType();

    public TypeMirror getDtoPropertyTypeMirror () {

      return dtoPropertyTypeMirror;
    }

    public TypeMirror getDtoPropertiesTypeMirror () {

      return dtoPropertiesTypeMirror;
    }

    public TypeMirror getDefaultXmlAdapterTypeMirror () {

      return defaultXmlAdapterTypeMirror;
    }
  }

  private class GeneratorInformation {

    private DirectionalMap inMap = new DirectionalMap(Direction.IN);
    private DirectionalMap outMap = new DirectionalMap(Direction.OUT);
    private String name;

    private GeneratorInformation (AnnotationMirror generatorAnnotationMirror, UsefulTypeMirrors usefulTypeMirrors)
      throws DtoDefinitionException {

      List<AnnotationValue> propertyAnnotationValueList;

      name = extractAnnotationValue(generatorAnnotationMirror, "name", String.class, "");

      if ((propertyAnnotationValueList = extractAnnotationValue(generatorAnnotationMirror, "properties", List.class, null)) != null) {
        for (AnnotationValue propertyAnnotationValue : propertyAnnotationValueList) {

          PropertyInformation propertyInformation = new PropertyInformation(extractAnnotationValue((AnnotationMirror)propertyAnnotationValue.getValue(), "type", TypeMirror.class, null), (AnnotationMirror)propertyAnnotationValue.getValue(), usefulTypeMirrors);
          String purpose = extractAnnotationValue((AnnotationMirror)propertyAnnotationValue.getValue(), "purpose", String.class, "");
          String fieldName = extractAnnotationValue((AnnotationMirror)propertyAnnotationValue.getValue(), "field", String.class, null);

          switch (propertyInformation.getVisibility()) {
            case IN:
              inMap.put(purpose, fieldName, propertyInformation);
              break;
            case OUT:
              outMap.put(purpose, fieldName, propertyInformation);
              break;
            case BOTH:
              inMap.put(purpose, fieldName, propertyInformation);
              outMap.put(purpose, fieldName, propertyInformation);
              break;
            default:
              throw new UnknownSwitchCaseException(propertyInformation.getVisibility().name());
          }
        }
      }
    }

    public String getName () {

      return name;
    }
  }

  private class PropertyInformation {

    private TypeMirror adapter;
    private TypeMirror type;
    private Visibility visibility;
    private String name;
    private boolean required;

    private PropertyInformation (TypeMirror type, AnnotationMirror propertyAnnotationMirror, UsefulTypeMirrors usefulTypeMirrors) {

      this.type = type;

      adapter = extractAnnotationValue(propertyAnnotationMirror, "adapter", TypeMirror.class, usefulTypeMirrors.getDefaultXmlAdapterTypeMirror());
      visibility = extractAnnotationValue(propertyAnnotationMirror, "visibility", Visibility.class, Visibility.BOTH);
      name = extractAnnotationValue(propertyAnnotationMirror, "name", String.class, "");
      required = extractAnnotationValue(propertyAnnotationMirror, "required", boolean.class, false);
    }

    private TypeMirror getAdapter () {

      return adapter;
    }

    private TypeMirror getType () {

      return type;
    }

    private Visibility getVisibility () {

      return visibility;
    }

    private String getName () {

      return name;
    }

    private boolean isRequired () {

      return required;
    }
  }

  private class DirectionalMap extends HashMap<String, HashMap<String, PropertyInformation>> {

    private Direction direction;

    public DirectionalMap (Direction direction) {

      this.direction = direction;
    }

    private void put (String purpose, String fieldName, PropertyInformation propertyInformation)
      throws DtoDefinitionException {

      HashMap<String, PropertyInformation> propertyMap;

      if ((propertyMap = get(purpose)) == null) {
        put(purpose, propertyMap = new HashMap<>());
      }

      if (propertyMap.containsKey(fieldName)) {
        throw new DtoDefinitionException("The field(name=%s, purpose=%s, direction=%s) has already been processed", fieldName, (purpose.isEmpty()) ? "n/a" : purpose, direction.name());
      } else {
        propertyMap.put(fieldName, propertyInformation);
      }
    }
  }

  private class DtoClass {

    private DirectionalMap inMap = new DirectionalMap(Direction.IN);
    private DirectionalMap outMap = new DirectionalMap(Direction.OUT);
    private HashMap<String, ExecutableElement> setMethodMap = new HashMap<>();
    private HashSet<Name> getMethodNameSet = new HashSet<>();
    private HashSet<String> isFieldNameSet = new HashSet<>();
    private HashSet<String> getFieldNameSet = new HashSet<>();
    private TypeElement classElement;

    private DtoClass (TypeElement classElement, UsefulTypeMirrors usefulTypeMirrors)
      throws IOException, DtoDefinitionException {

      this.classElement = classElement;

      for (Element enclosedElement : classElement.getEnclosedElements()) {
        if (enclosedElement.getModifiers().contains(javax.lang.model.element.Modifier.STATIC) && (enclosedElement.getAnnotation(DtoProperty.class) != null)) {
          throw new DtoDefinitionException("The element(%s) annotated as @%s may not be 'static'", enclosedElement.getSimpleName(), DtoProperty.class.getSimpleName());
        } else if (enclosedElement.getModifiers().contains(Modifier.ABSTRACT) && (enclosedElement.getAnnotation(DtoProperty.class) != null)) {
          throw new DtoDefinitionException("The element(%s) annotated as @%s may not be 'abstract'", enclosedElement.getSimpleName(), DtoProperty.class.getSimpleName());
        } else if (ElementKind.FIELD.equals(enclosedElement.getKind())) {
          processTypeMirror(enclosedElement.asType());
        } else if (ElementKind.METHOD.equals(enclosedElement.getKind()) && enclosedElement.getModifiers().contains(javax.lang.model.element.Modifier.PUBLIC)) {

          String methodName = enclosedElement.getSimpleName().toString();

          if (methodName.startsWith("is") && (methodName.length() > 2) && Character.isUpperCase(methodName.charAt(2)) && ((ExecutableElement)enclosedElement).getParameters().isEmpty() && TypeKind.BOOLEAN.equals(((ExecutableElement)enclosedElement).getReturnType().getKind())) {

            String fieldName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);

            for (AnnotationMirror dtoPropertyMirror : extractAnnotationMirrors(enclosedElement, usefulTypeMirrors.getDtoPropertiesTypeMirror(), usefulTypeMirrors.getDtoPropertyTypeMirror())) {

              PropertyInformation propertyInformation = new PropertyInformation(((ExecutableElement)enclosedElement).getReturnType(), dtoPropertyMirror, usefulTypeMirrors);

              if (Visibility.IN.equals(propertyInformation.getVisibility())) {
                throw new DtoDefinitionException("The 'is' method(%s) found in class(%s) can't be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              }

              outMap.put(extractAnnotationValue(dtoPropertyMirror, "purpose", String.class, ""), fieldName, propertyInformation);
            }

            getMethodNameSet.add(enclosedElement.getSimpleName());
            isFieldNameSet.add(fieldName);
          } else if (methodName.startsWith("get") && (methodName.length() > 3) && Character.isUpperCase(methodName.charAt(3)) && ((ExecutableElement)enclosedElement).getParameters().isEmpty() && (!TypeKind.VOID.equals(((ExecutableElement)enclosedElement).getReturnType().getKind()))) {

            String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

            for (AnnotationMirror dtoPropertyMirror : extractAnnotationMirrors(enclosedElement, usefulTypeMirrors.getDtoPropertiesTypeMirror(), usefulTypeMirrors.getDtoPropertyTypeMirror())) {

              PropertyInformation propertyInformation = new PropertyInformation(((ExecutableElement)enclosedElement).getReturnType(), dtoPropertyMirror, usefulTypeMirrors);

              if (Visibility.IN.equals(propertyInformation.getVisibility())) {
                throw new DtoDefinitionException("The 'get' method(%s) found in class(%s) can't be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              }

              processTypeMirror(((ExecutableElement)enclosedElement).getReturnType());
              outMap.put(extractAnnotationValue(dtoPropertyMirror, "purpose", String.class, ""), fieldName, propertyInformation);
            }

            getMethodNameSet.add(enclosedElement.getSimpleName());
            getFieldNameSet.add(fieldName);
          } else if (methodName.startsWith("set") && (methodName.length() > 3) && Character.isUpperCase(methodName.charAt(3)) && (((ExecutableElement)enclosedElement).getParameters().size() == 1)) {

            String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

            for (AnnotationMirror dtoPropertyMirror : extractAnnotationMirrors(enclosedElement, usefulTypeMirrors.getDtoPropertiesTypeMirror(), usefulTypeMirrors.getDtoPropertyTypeMirror())) {

              PropertyInformation propertyInformation = new PropertyInformation(((ExecutableElement)enclosedElement).getParameters().get(0).asType(), dtoPropertyMirror, usefulTypeMirrors);

              if (!Visibility.IN.equals(propertyInformation.getVisibility())) {
                throw new DtoDefinitionException("The 'set' method(%s) found in class(%s) must be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              }

              processTypeMirror(((ExecutableElement)enclosedElement).getParameters().get(0).asType());
              inMap.put(extractAnnotationValue(dtoPropertyMirror, "purpose", String.class, ""), fieldName, propertyInformation);
            }

            setMethodMap.put(fieldName, (ExecutableElement)enclosedElement);
          } else if (enclosedElement.getAnnotation(DtoProperty.class) != null) {
            throw new DtoDefinitionException("The method(%s) found in class(%s) must be a 'getter' or 'setter'", enclosedElement.getSimpleName(), classElement.getQualifiedName());
          }
        }
      }

      for (Element enclosedElement : classElement.getEnclosedElements()) {

        for (AnnotationMirror dtoPropertyMirror : extractAnnotationMirrors(enclosedElement, usefulTypeMirrors.getDtoPropertiesTypeMirror(), usefulTypeMirrors.getDtoPropertyTypeMirror())) {
          if (ElementKind.FIELD.equals(enclosedElement.getKind())) {

            PropertyInformation propertyInformation = new PropertyInformation(enclosedElement.asType(), dtoPropertyMirror, usefulTypeMirrors);

            switch (propertyInformation.getVisibility()) {
              case IN:
                inField(enclosedElement.getSimpleName().toString(), extractAnnotationValue(dtoPropertyMirror, "purpose", String.class, ""), propertyInformation);
                break;
              case OUT:
                outField(enclosedElement.getSimpleName().toString(), extractAnnotationValue(dtoPropertyMirror, "purpose", String.class, ""), propertyInformation);
                break;
              case BOTH:
                inField(enclosedElement.getSimpleName().toString(), extractAnnotationValue(dtoPropertyMirror, "purpose", String.class, ""), propertyInformation);
                outField(enclosedElement.getSimpleName().toString(), extractAnnotationValue(dtoPropertyMirror, "purpose", String.class, ""), propertyInformation);
                break;
              default:
                throw new UnknownSwitchCaseException(propertyInformation.getVisibility().name());
            }
          } else if (ElementKind.METHOD.equals(enclosedElement.getKind()) && getMethodNameSet.contains(enclosedElement.getSimpleName()) && Visibility.BOTH.equals(extractAnnotationValue(dtoPropertyMirror, "visibility", Visibility.class, Visibility.BOTH))) {

            String methodName = enclosedElement.getSimpleName().toString();
            String fieldName = methodName.startsWith("is") ? Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3) : Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

            if (!setMethodMap.containsKey(fieldName)) {
              throw new DtoDefinitionException("The 'getter' method(%s) found in class(%s) must have a corresponding 'setter'", enclosedElement.getSimpleName(), classElement.getQualifiedName());
            } else if (!inMap.containsKey(fieldName)) {
              processTypeMirror(setMethodMap.get(fieldName).getParameters().get(0).asType());
              inMap.put(extractAnnotationValue(dtoPropertyMirror, "purpose", String.class, ""), fieldName, new PropertyInformation(setMethodMap.get(fieldName).getParameters().get(0).asType(), dtoPropertyMirror, usefulTypeMirrors));
            }
          }
        }
      }
    }

    private void processTypeMirror (TypeMirror typeMirror)
      throws IOException, DtoDefinitionException {

      if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

        Element element;

        if (ElementKind.CLASS.equals((element = processingEnv.getTypeUtils().asElement(typeMirror)).getKind())) {
          generate((TypeElement)element);
        }
      }
    }

    private DirectionalMap getInMap () {

      return inMap;
    }

    private DirectionalMap getOutMap () {

      return outMap;
    }

    private void inField (String fieldName, String purpose, PropertyInformation propertyInformation)
      throws DtoDefinitionException {

      if (setMethodMap.containsKey(fieldName)) {
        inMap.put(purpose, fieldName, propertyInformation);
      } else {
        throw new DtoDefinitionException("The property field(%s) has no 'setter' method in class(%s)", fieldName, classElement.getQualifiedName());
      }
    }

    private void outField (String fieldName, String purpose, PropertyInformation propertyInformation)
      throws DtoDefinitionException {

      if (getFieldNameSet.contains(fieldName) || isFieldNameSet.contains(fieldName)) {
        outMap.put(purpose, fieldName, propertyInformation);
      } else {
        throw new DtoDefinitionException("The property field(%s) has no 'getter' method in class(%s)", fieldName, classElement.getQualifiedName());
      }
    }
  }
}