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
import org.smallmind.nutsnbolts.apt.AptUtility;
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

    AnnotationMirror dtoGeneratorAnnotationMirror;

    if ((!generatedMap.containsKey(classElement)) && ((dtoGeneratorAnnotationMirror = AptUtility.extractAnnotationMirror(processingEnv, classElement, processingEnv.getElementUtils().getTypeElement(DtoGenerator.class.getName()).asType())) != null)) {
      if ((!ElementKind.CLASS.equals(classElement.getKind())) || (!NestingKind.TOP_LEVEL.equals(classElement.getNestingKind()))) {
        throw new DtoDefinitionException("The class(%s) must be a root implementation of type 'class'", classElement.getQualifiedName());
      } else {

        UsefulTypeMirrors usefulTypeMirrors = new UsefulTypeMirrors();
        GeneratorInformation generatorInformation = new GeneratorInformation(dtoGeneratorAnnotationMirror, usefulTypeMirrors);
        DtoClass dtoClass = new DtoClass(classElement, generatorInformation, usefulTypeMirrors);
        TypeElement nearestDtoSuperclass;
        boolean written = false;

        // avoid recursion now, add context later
        generatedMap.put(classElement, null);

        if ((nearestDtoSuperclass = getNearestDtoSuperclass(classElement)) != null) {
          generate(nearestDtoSuperclass);
        }

        if (generatorInformation.isPolymorphic()) {
          for (TypeElement polymorphicSubClass : generatorInformation.getPolymorphicSubclassList()) {
            generate(polymorphicSubClass);
          }
        }

        for (Map.Entry<String, PropertyMap> purposeEntry : dtoClass.getInMap().entrySet()) {
          writeDto(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purposeEntry.getKey(), Direction.IN, purposeEntry.getValue());

          generatedMap.put(classElement, Visibility.IN);
          written = true;
        }
        for (Map.Entry<String, PropertyMap> purposeEntry : dtoClass.getOutMap().entrySet()) {
          writeDto(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purposeEntry.getKey(), Direction.OUT, purposeEntry.getValue());

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

  private void writeDto (GeneratorInformation generatorInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestDtoSuperclass, String purpose, Direction direction, PropertyMap propertyMap)
    throws IOException {

    JavaFileObject sourceFile;

    if (generatorInformation.isPolymorphic()) {
      writePolymorphicAdapter(classElement, purpose, direction);
    }

    sourceFile = processingEnv.getFiler().createSourceFile(new StringBuilder(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName()).append('.').append(asDtoName(classElement.getSimpleName(), purpose, direction)), classElement);

    if (sourceFile.getNestingKind() == null) {
      try (BufferedWriter writer = new BufferedWriter(sourceFile.openWriter())) {

        LinkedList<TypeElement> matchingSubClassList = new LinkedList<>();

        // package
        writer.write("package ");
        writer.write(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString());
        writer.write(";");
        writer.newLine();
        writer.newLine();

        if (generatorInformation.isPolymorphic()) {
          for (TypeElement polymorphicSubClass : generatorInformation.getPolymorphicSubclassList()) {

            Visibility visibility;

            if (((visibility = generatedMap.get(polymorphicSubClass)) != null) && visibility.matches(direction)) {
              matchingSubClassList.add(polymorphicSubClass);
            }
          }
        }

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
        if (!matchingSubClassList.isEmpty()) {
          writer.write("import org.smallmind.web.json.scaffold.util.XmlPolymorphicSubClasses;");
          writer.newLine();
        }

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
        if ((nearestDtoSuperclass != null)) {
          writer.write("@XmlAccessorType(XmlAccessType.PROPERTY)");
          writer.newLine();
        }

        if (generatorInformation.isPolymorphic()) {
          // XmlJavaTypeAdapter
          writer.write("@XmlJavaTypeAdapter(");
          writer.write(asDtoName(classElement.getSimpleName(), purpose, direction).toString());
          writer.write("PolymorphicXmlAdapter.class)");
          writer.newLine();

          // XmlPolymorphicSubClasses
          if (!matchingSubClassList.isEmpty()) {

            boolean firstPolymorphicSubClass = true;
            writer.write("@XmlPolymorphicSubClasses(");
            if (matchingSubClassList.size() > 1) {
              writer.write("{");
            }

            for (TypeElement polymorphicSubClass : matchingSubClassList) {
              if (!firstPolymorphicSubClass) {
                writer.write(", ");
              }
              writer.write(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString());
              writer.write(".");
              writer.write(asDtoName(polymorphicSubClass.getSimpleName(), purpose, direction).toString());
              writer.write(".class");

              firstPolymorphicSubClass = false;
            }

            if (matchingSubClassList.size() > 1) {
              writer.write("}");
            }
            writer.write(")");
            writer.newLine();
          }
        }

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

        // virtual field declarations
        if (propertyMap.isVirtual()) {
          writer.newLine();
          writer.write("  // virtual fields");
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyMap.virtualMap.entrySet()) {
            writeField(writer, purpose, direction, propertyInformationEntry);
          }
        }

        // native field declarations
        if (propertyMap.isReal()) {
          writer.newLine();
          writer.write("  // native fields");
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyMap.getRealMap().entrySet()) {
            writeField(writer, purpose, direction, propertyInformationEntry);
          }
        }

        // constructors
        writer.newLine();
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
        }

        if (propertyMap.isReal()) {
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyMap.getRealMap().entrySet()) {
            writer.write("    this.");
            writer.write(propertyInformationEntry.getKey());
            writer.write(" = ");
            if (isDtoType(propertyInformationEntry.getValue().getType(), direction)) {
              writer.write("new ");
              writer.write(asCompatibleName(propertyInformationEntry.getValue().getType(), purpose, direction));
              writer.write("(");
            }
            writer.write(asMemberName(classElement.getSimpleName()));
            writer.write(".");
            writer.write(TypeKind.BOOLEAN.equals(propertyInformationEntry.getValue().getType().getKind()) ? BeanUtility.asIsName(propertyInformationEntry.getKey()) : BeanUtility.asGetterName(propertyInformationEntry.getKey()));
            writer.write("()");
            if (isDtoType(propertyInformationEntry.getValue().getType(), direction)) {
              writer.write(")");
            }
            writer.write(";");
            writer.newLine();
          }
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

        if (propertyMap.isReal()) {
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyMap.getRealMap().entrySet()) {
            writer.write("    ");
            writer.write(asMemberName(classElement.getSimpleName()));
            writer.write(".");
            writer.write(BeanUtility.asSetterName(propertyInformationEntry.getKey()));
            writer.write("(");
            writer.write(propertyInformationEntry.getKey());
            if (isDtoType(propertyInformationEntry.getValue().getType(), direction)) {
              writer.write(".factory(");
              writer.write("new ");
              writer.write(processingEnv.getTypeUtils().asElement(propertyInformationEntry.getValue().getType()).getSimpleName().toString());
              writer.write("())");
            }
            writer.write(");");
            writer.newLine();
          }
        }

        writer.newLine();
        writer.write("    return ");
        writer.write(asMemberName(classElement.getSimpleName()));
        writer.write(";");
        writer.newLine();
        writer.write("  }");
        writer.newLine();

        // virtual getters and setters
        if (propertyMap.isVirtual()) {
          writer.newLine();
          writer.write("  // virtual getters and setters");
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyMap.getVirtualMap().entrySet()) {
            writeGettersAndSetters(writer, usefulTypeMirrors, purpose, direction, propertyInformationEntry);
          }
        }

        // native getters and setters
        if (propertyMap.isReal()) {
          writer.newLine();
          writer.write("  // native getters and setters");
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyMap.getRealMap().entrySet()) {
            writeGettersAndSetters(writer, usefulTypeMirrors, purpose, direction, propertyInformationEntry);
          }
        }

        writer.write("}");
        writer.newLine();
      }
    }
  }

  private void writeField (BufferedWriter writer, String purpose, Direction direction, Map.Entry<String, PropertyInformation> propertyInformationEntry)
    throws IOException {

    writer.write("  private ");
    writer.write(asCompatibleName(propertyInformationEntry.getValue().getType(), purpose, direction));
    writer.write(" ");
    writer.write(propertyInformationEntry.getKey());
    writer.write(";");
    writer.newLine();
  }

  private void writeGettersAndSetters (BufferedWriter writer, UsefulTypeMirrors usefulTypeMirrors, String purpose, Direction direction, Map.Entry<String, PropertyInformation> propertyInformationEntry)
    throws IOException {

    if (!processingEnv.getTypeUtils().isSameType(usefulTypeMirrors.getDefaultXmlAdapterTypeMirror(), propertyInformationEntry.getValue().getAdapter())) {
      writer.write("  @XmlJavaTypeAdapter(");
      writer.write(propertyInformationEntry.getValue().getAdapter().toString());
      writer.write(".class)");
      writer.newLine();
    }
    writer.write("  @XmlElement(name = \"");
    writer.write(propertyInformationEntry.getValue().getName().isEmpty() ? propertyInformationEntry.getKey() : propertyInformationEntry.getValue().getName());
    writer.write(propertyInformationEntry.getValue().isRequired() ? "\", required = true)" : "\")");
    writer.newLine();
    writer.write("  public ");
    writer.write(asCompatibleName(propertyInformationEntry.getValue().getType(), purpose, direction));
    writer.write(" ");
    writer.write(TypeKind.BOOLEAN.equals(propertyInformationEntry.getValue().getType().getKind()) ? BeanUtility.asIsName(propertyInformationEntry.getKey()) : BeanUtility.asGetterName(propertyInformationEntry.getKey()));
    writer.write(" () {");
    writer.newLine();
    writer.newLine();
    writer.write("    return ");
    writer.write(propertyInformationEntry.getKey());
    writer.write(";");
    writer.newLine();
    writer.write("  }");
    writer.newLine();
    writer.newLine();

    writer.write("  public void ");
    writer.write(BeanUtility.asSetterName(propertyInformationEntry.getKey()));
    writer.write(" (");
    writer.write(asCompatibleName(propertyInformationEntry.getValue().getType(), purpose, direction));
    writer.write(" ");
    writer.write(propertyInformationEntry.getKey());
    writer.write(") {");
    writer.newLine();
    writer.newLine();
    writer.write("    this.");
    writer.write(propertyInformationEntry.getKey());
    writer.write(" = ");
    writer.write(propertyInformationEntry.getKey());
    writer.write(";");
    writer.newLine();
    writer.write("  }");
    writer.newLine();
  }

  private void writePolymorphicAdapter (TypeElement classElement, String purpose, Direction direction)
    throws IOException {

    JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(new StringBuilder(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName()).append('.').append(asDtoName(classElement.getSimpleName(), purpose, direction)).append("PolymorphicXmlAdapter"), classElement);

    if (sourceFile.getNestingKind() == null) {
      try (BufferedWriter writer = new BufferedWriter(sourceFile.openWriter())) {

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
        writer.write("import org.smallmind.web.json.scaffold.util.PolymorphicXmlAdapter;");
        writer.newLine();
        writer.newLine();

        // @Generated
        writer.write("@Generated(\"");
        writer.write(DtoAnnotationProcessor.class.getName());
        writer.write("\")");
        writer.newLine();

        // class declaration
        writer.write("public class ");
        writer.write(asDtoName(classElement.getSimpleName(), purpose, direction).toString());
        writer.write("PolymorphicXmlAdapter");
        writer.write(" extends PolymorphicXmlAdapter<");
        writer.write(asDtoName(classElement.getSimpleName(), purpose, direction).toString());
        writer.write("> {");
        writer.newLine();
        writer.newLine();
        writer.write("}");
        writer.newLine();
      }
    }
  }

  private class UsefulTypeMirrors {

    private final TypeMirror dtoPropertyTypeMirror = processingEnv.getElementUtils().getTypeElement(DtoProperty.class.getName()).asType();
    private final TypeMirror dtoPropertiesTypeMirror = processingEnv.getElementUtils().getTypeElement(DtoProperties.class.getName()).asType();
    private final TypeMirror defaultXmlAdapterTypeMirror = processingEnv.getElementUtils().getTypeElement(DefaultXmlAdapter.class.getName()).asType();

    private TypeMirror getDtoPropertyTypeMirror () {

      return dtoPropertyTypeMirror;
    }

    private TypeMirror getDtoPropertiesTypeMirror () {

      return dtoPropertiesTypeMirror;
    }

    private TypeMirror getDefaultXmlAdapterTypeMirror () {

      return defaultXmlAdapterTypeMirror;
    }
  }

  private class GeneratorInformation {

    private final DirectionalMap inMap = new DirectionalMap(Direction.IN);
    private final DirectionalMap outMap = new DirectionalMap(Direction.OUT);
    private final List<TypeElement> polymorphicSubclassList;
    private final String name;
    private final Boolean polymorphic;

    private GeneratorInformation (AnnotationMirror generatorAnnotationMirror, UsefulTypeMirrors usefulTypeMirrors)
      throws DtoDefinitionException {

      name = AptUtility.extractAnnotationValue(generatorAnnotationMirror, "name", String.class, "");
      polymorphicSubclassList = AptUtility.toConcreteList(processingEnv, AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "polymorphicSubClasses", TypeMirror.class));
      polymorphic = AptUtility.extractAnnotationValue(generatorAnnotationMirror, "polymorphic", Boolean.class, Boolean.FALSE);

      for (AnnotationMirror propertyAnnotationMirror : AptUtility.extractAnnotationValueAsList(generatorAnnotationMirror, "properties", AnnotationMirror.class)) {

        PropertyInformation propertyInformation = new PropertyInformation(AptUtility.extractAnnotationValue(propertyAnnotationMirror, "type", TypeMirror.class, null), propertyAnnotationMirror, usefulTypeMirrors, true);
        List<String> purposes = AptUtility.extractAnnotationValueAsList(propertyAnnotationMirror, "purposes", String.class);
        String fieldName = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "field", String.class, null);

        switch (propertyInformation.getVisibility()) {
          case IN:
            inMap.put(purposes, fieldName, propertyInformation);
            break;
          case OUT:
            outMap.put(purposes, fieldName, propertyInformation);
            break;
          case BOTH:
            inMap.put(purposes, fieldName, propertyInformation);
            outMap.put(purposes, fieldName, propertyInformation);
            break;
          default:
            throw new UnknownSwitchCaseException(propertyInformation.getVisibility().name());
        }
      }
    }

    private String getName () {

      return name;
    }

    private boolean isPolymorphic () {

      return ((polymorphic != null) && polymorphic) || (!polymorphicSubclassList.isEmpty());
    }

    private List<TypeElement> getPolymorphicSubclassList () {

      return polymorphicSubclassList;
    }

    private DirectionalMap getInMap () {

      return inMap;
    }

    private DirectionalMap getOutMap () {

      return outMap;
    }
  }

  private class PropertyInformation {

    private final TypeMirror adapter;
    private final TypeMirror type;
    private final Visibility visibility;
    private final String name;
    private final Boolean required;
    private final boolean virtual;

    private PropertyInformation (TypeMirror type, AnnotationMirror propertyAnnotationMirror, UsefulTypeMirrors usefulTypeMirrors, boolean virtual) {

      this.virtual = virtual;

      this.type = type;

      adapter = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "adapter", TypeMirror.class, usefulTypeMirrors.getDefaultXmlAdapterTypeMirror());
      visibility = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "visibility", Visibility.class, Visibility.BOTH);
      name = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "name", String.class, "");
      required = AptUtility.extractAnnotationValue(propertyAnnotationMirror, "required", Boolean.class, Boolean.FALSE);
    }

    private boolean isVirtual () {

      return virtual;
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

      return (required != null) && required;
    }
  }

  private class PropertyMap {

    private final HashMap<String, PropertyInformation> realMap = new HashMap<>();
    private final HashMap<String, PropertyInformation> virtualMap = new HashMap<>();

    private void put (String key, PropertyInformation propertyInformation) {

      if (propertyInformation.isVirtual()) {

        virtualMap.put(key, propertyInformation);
      } else {

        realMap.put(key, propertyInformation);
      }
    }

    private boolean isReal () {

      return !realMap.isEmpty();
    }

    private boolean isVirtual () {

      return !virtualMap.isEmpty();
    }

    private boolean containsKey (String key) {

      return realMap.containsKey(key) || virtualMap.containsKey(key);
    }

    private HashMap<String, PropertyInformation> getRealMap () {

      return realMap;
    }

    private HashMap<String, PropertyInformation> getVirtualMap () {

      return virtualMap;
    }
  }

  private class DirectionalMap {

    private final HashMap<String, PropertyMap> internalMap = new HashMap<>();
    private final Direction direction;

    private DirectionalMap (Direction direction) {

      this.direction = direction;
    }

    private DirectionalMap (Direction direction, DirectionalMap directionalMap) {

      this(direction);

      internalMap.putAll(directionalMap.getInternalMap());
    }

    private HashMap<String, PropertyMap> getInternalMap () {

      return internalMap;
    }

    private void put (List<String> purposes, String fieldName, PropertyInformation propertyInformation)
      throws DtoDefinitionException {

      if ((purposes == null) || purposes.isEmpty()) {
        purposes = Collections.singletonList("");
      }

      for (String purpose : purposes) {

        PropertyMap propertyMap;

        if ((propertyMap = internalMap.get(purpose)) == null) {
          internalMap.put(purpose, propertyMap = new PropertyMap());
        }

        if (propertyMap.containsKey(fieldName)) {
          throw new DtoDefinitionException("The field(name=%s, purpose=%s, direction=%s) has already been processed", fieldName, (purpose.isEmpty()) ? "n/a" : purpose, direction.name());
        } else {
          propertyMap.put(fieldName, propertyInformation);
        }
      }
    }

    private Set<Map.Entry<String, PropertyMap>> entrySet () {

      return internalMap.entrySet();
    }
  }

  private class DtoClass {

    private final DirectionalMap inMap;
    private final DirectionalMap outMap;
    private final HashMap<String, ExecutableElement> setMethodMap = new HashMap<>();
    private final HashSet<Name> getMethodNameSet = new HashSet<>();
    private final HashSet<String> isFieldNameSet = new HashSet<>();
    private final HashSet<String> getFieldNameSet = new HashSet<>();
    private final TypeElement classElement;

    private DtoClass (TypeElement classElement, GeneratorInformation generatorInformation, UsefulTypeMirrors usefulTypeMirrors)
      throws IOException, DtoDefinitionException {

      this.classElement = classElement;

      inMap = new DirectionalMap(Direction.IN, generatorInformation.getInMap());
      outMap = new DirectionalMap(Direction.OUT, generatorInformation.getOutMap());

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

            for (AnnotationMirror dtoPropertyAnnotationMirror : AptUtility.extractAnnotationMirrors(processingEnv, enclosedElement, usefulTypeMirrors.getDtoPropertiesTypeMirror(), usefulTypeMirrors.getDtoPropertyTypeMirror())) {

              PropertyInformation propertyInformation = new PropertyInformation(((ExecutableElement)enclosedElement).getReturnType(), dtoPropertyAnnotationMirror, usefulTypeMirrors, false);

              if (Visibility.IN.equals(propertyInformation.getVisibility())) {
                throw new DtoDefinitionException("The 'is' method(%s) found in class(%s) can't be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              }

              outMap.put(AptUtility.extractAnnotationValueAsList(dtoPropertyAnnotationMirror, "purposes", String.class), fieldName, propertyInformation);
            }

            getMethodNameSet.add(enclosedElement.getSimpleName());
            isFieldNameSet.add(fieldName);
          } else if (methodName.startsWith("get") && (methodName.length() > 3) && Character.isUpperCase(methodName.charAt(3)) && ((ExecutableElement)enclosedElement).getParameters().isEmpty() && (!TypeKind.VOID.equals(((ExecutableElement)enclosedElement).getReturnType().getKind()))) {

            String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

            for (AnnotationMirror dtoPropertyAnnotationMirror : AptUtility.extractAnnotationMirrors(processingEnv, enclosedElement, usefulTypeMirrors.getDtoPropertiesTypeMirror(), usefulTypeMirrors.getDtoPropertyTypeMirror())) {

              PropertyInformation propertyInformation = new PropertyInformation(((ExecutableElement)enclosedElement).getReturnType(), dtoPropertyAnnotationMirror, usefulTypeMirrors, false);

              if (Visibility.IN.equals(propertyInformation.getVisibility())) {
                throw new DtoDefinitionException("The 'get' method(%s) found in class(%s) can't be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              }

              processTypeMirror(((ExecutableElement)enclosedElement).getReturnType());
              outMap.put(AptUtility.extractAnnotationValueAsList(dtoPropertyAnnotationMirror, "purposes", String.class), fieldName, propertyInformation);
            }

            getMethodNameSet.add(enclosedElement.getSimpleName());
            getFieldNameSet.add(fieldName);
          } else if (methodName.startsWith("set") && (methodName.length() > 3) && Character.isUpperCase(methodName.charAt(3)) && (((ExecutableElement)enclosedElement).getParameters().size() == 1)) {

            String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

            for (AnnotationMirror dtoPropertyAnnotationMirror : AptUtility.extractAnnotationMirrors(processingEnv, enclosedElement, usefulTypeMirrors.getDtoPropertiesTypeMirror(), usefulTypeMirrors.getDtoPropertyTypeMirror())) {

              PropertyInformation propertyInformation = new PropertyInformation(((ExecutableElement)enclosedElement).getParameters().get(0).asType(), dtoPropertyAnnotationMirror, usefulTypeMirrors, false);

              if (!Visibility.IN.equals(propertyInformation.getVisibility())) {
                throw new DtoDefinitionException("The 'set' method(%s) found in class(%s) must be annotated as 'IN' only", enclosedElement.getSimpleName(), classElement.getQualifiedName());
              }

              processTypeMirror(((ExecutableElement)enclosedElement).getParameters().get(0).asType());
              inMap.put(AptUtility.extractAnnotationValueAsList(dtoPropertyAnnotationMirror, "purposes", String.class), fieldName, propertyInformation);
            }

            setMethodMap.put(fieldName, (ExecutableElement)enclosedElement);
          } else if (enclosedElement.getAnnotation(DtoProperty.class) != null) {
            throw new DtoDefinitionException("The method(%s) found in class(%s) must be a 'getter' or 'setter'", enclosedElement.getSimpleName(), classElement.getQualifiedName());
          }
        }
      }

      for (Element enclosedElement : classElement.getEnclosedElements()) {

        for (AnnotationMirror dtoPropertyAnnotationMirror : AptUtility.extractAnnotationMirrors(processingEnv, enclosedElement, usefulTypeMirrors.getDtoPropertiesTypeMirror(), usefulTypeMirrors.getDtoPropertyTypeMirror())) {
          if (ElementKind.FIELD.equals(enclosedElement.getKind())) {

            PropertyInformation propertyInformation = new PropertyInformation(enclosedElement.asType(), dtoPropertyAnnotationMirror, usefulTypeMirrors, false);

            switch (propertyInformation.getVisibility()) {
              case IN:
                inField(enclosedElement.getSimpleName().toString(), AptUtility.extractAnnotationValueAsList(dtoPropertyAnnotationMirror, "purposes", String.class), propertyInformation);
                break;
              case OUT:
                outField(enclosedElement.getSimpleName().toString(), AptUtility.extractAnnotationValueAsList(dtoPropertyAnnotationMirror, "purposes", String.class), propertyInformation);
                break;
              case BOTH:
                inField(enclosedElement.getSimpleName().toString(), AptUtility.extractAnnotationValueAsList(dtoPropertyAnnotationMirror, "purposes", String.class), propertyInformation);
                outField(enclosedElement.getSimpleName().toString(), AptUtility.extractAnnotationValueAsList(dtoPropertyAnnotationMirror, "purposes", String.class), propertyInformation);
                break;
              default:
                throw new UnknownSwitchCaseException(propertyInformation.getVisibility().name());
            }
          } else if (ElementKind.METHOD.equals(enclosedElement.getKind()) && getMethodNameSet.contains(enclosedElement.getSimpleName()) && Visibility.BOTH.equals(AptUtility.extractAnnotationValue(dtoPropertyAnnotationMirror, "visibility", Visibility.class, Visibility.BOTH))) {

            String methodName = enclosedElement.getSimpleName().toString();
            String fieldName = methodName.startsWith("is") ? Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3) : Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

            if (!setMethodMap.containsKey(fieldName)) {
              throw new DtoDefinitionException("The 'getter' method(%s) found in class(%s) must have a corresponding 'setter'", enclosedElement.getSimpleName(), classElement.getQualifiedName());
            } else {
              processTypeMirror(setMethodMap.get(fieldName).getParameters().get(0).asType());
              inMap.put(AptUtility.extractAnnotationValueAsList(dtoPropertyAnnotationMirror, "purposes", String.class), fieldName, new PropertyInformation(setMethodMap.get(fieldName).getParameters().get(0).asType(), dtoPropertyAnnotationMirror, usefulTypeMirrors, false));
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

    private void inField (String fieldName, List<String> purposes, PropertyInformation propertyInformation)
      throws DtoDefinitionException {

      if (setMethodMap.containsKey(fieldName)) {
        inMap.put(purposes, fieldName, propertyInformation);
      } else {
        throw new DtoDefinitionException("The property field(%s) has no 'setter' method in class(%s)", fieldName, classElement.getQualifiedName());
      }
    }

    private void outField (String fieldName, List<String> purposes, PropertyInformation propertyInformation)
      throws DtoDefinitionException {

      if (getFieldNameSet.contains(fieldName) || isFieldNameSet.contains(fieldName)) {
        outMap.put(purposes, fieldName, propertyInformation);
      } else {
        throw new DtoDefinitionException("The property field(%s) has no 'getter' method in class(%s)", fieldName, classElement.getQualifiedName());
      }
    }
  }
}