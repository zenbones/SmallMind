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
import java.util.HashMap;
import java.util.LinkedList;
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
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import com.google.auto.service.AutoService;
import org.smallmind.nutsnbolts.apt.AptUtility;
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

  public void processTypeMirror (TypeMirror typeMirror)
    throws IOException, DtoDefinitionException {

    if (TypeKind.DECLARED.equals(typeMirror.getKind())) {

      Element element;

      if (ElementKind.CLASS.equals((element = processingEnv.getTypeUtils().asElement(typeMirror)).getKind())) {
        generate((TypeElement)element);
      }
    }
  }

  private void generate (TypeElement classElement)
    throws IOException, DtoDefinitionException {

    AnnotationMirror dtoGeneratorAnnotationMirror;

    if ((!generatedMap.containsKey(classElement)) && ((dtoGeneratorAnnotationMirror = AptUtility.extractAnnotationMirror(processingEnv, classElement, processingEnv.getElementUtils().getTypeElement(DtoGenerator.class.getName()).asType())) != null)) {
      if ((!ElementKind.CLASS.equals(classElement.getKind())) || (!NestingKind.TOP_LEVEL.equals(classElement.getNestingKind()))) {
        throw new DtoDefinitionException("The class(%s) must be a root implementation of type 'class'", classElement.getQualifiedName());
      } else {

        UsefulTypeMirrors usefulTypeMirrors = new UsefulTypeMirrors(processingEnv);
        GeneratorInformation generatorInformation = new GeneratorInformation(processingEnv, this, dtoGeneratorAnnotationMirror, usefulTypeMirrors);
        DtoClass dtoClass = new DtoClass(processingEnv, this, classElement, generatorInformation, usefulTypeMirrors);
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
          processIn(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purposeEntry.getKey(), purposeEntry.getValue());
          written = true;
        }
        for (String unfulfilledPurpose : generatorInformation.unfulfilledPurposes(Direction.IN)) {
          processIn(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, unfulfilledPurpose, new PropertyMap());

          written = true;
        }
        for (Map.Entry<String, PropertyMap> purposeEntry : dtoClass.getOutMap().entrySet()) {
          processOut(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purposeEntry.getKey(), purposeEntry.getValue());

          written = true;
        }
        for (String unfulfilledPurpose : generatorInformation.unfulfilledPurposes(Direction.IN)) {
          processOut(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, unfulfilledPurpose, new PropertyMap());
          written = true;
        }

        if (!written) {
          throw new DtoDefinitionException("The class(%s) was annotated as @%s but contained no properties", classElement.getQualifiedName(), DtoGenerator.class.getSimpleName());
        }
      }
    }
  }

  private void processIn (GeneratorInformation generatorInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestDtoSuperclass, String purpose, PropertyMap propertyMap)
    throws IOException {

    writeDto(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purpose, Direction.IN, propertyMap);

    generatorInformation.denotePurpose(Direction.IN, purpose);
    generatedMap.put(classElement, Visibility.IN);
  }

  private void processOut (GeneratorInformation generatorInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestDtoSuperclass, String purpose, PropertyMap propertyMap)
    throws IOException {

    writeDto(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purpose, Direction.OUT, propertyMap);

    generatorInformation.denotePurpose(Direction.OUT, purpose);
    if (!Visibility.BOTH.equals(generatedMap.get(classElement))) {
      if (Visibility.IN.equals(generatedMap.get(classElement))) {
        generatedMap.put(classElement, Visibility.BOTH);
      } else {
        generatedMap.put(classElement, Visibility.OUT);
      }
    }
  }

  private String asMemberName (Name name) {

    return Character.toLowerCase(name.charAt(0)) + name.subSequence(1, name.length()).toString();
  }

  private StringBuilder asDtoName (Name simpleName, String purpose, Direction direction) {

    StringBuilder dtoNameBuilder = new StringBuilder((processingEnv.getOptions().get("prefix") == null) ? "" : processingEnv.getOptions().get("prefix")).append(simpleName);

    if ((purpose != null) && (!purpose.isEmpty())) {
      dtoNameBuilder.append(Character.toUpperCase(purpose.charAt(0))).append(purpose.substring(1));
    }

    return dtoNameBuilder.append(direction.getCode()).append("Dto");
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
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyMap.getVirtualMap().entrySet()) {
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
            writeGettersAndSetters(writer, usefulTypeMirrors, classElement, purpose, direction, propertyInformationEntry);
          }
        }

        // native getters and setters
        if (propertyMap.isReal()) {
          writer.newLine();
          writer.write("  // native getters and setters");
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyMap.getRealMap().entrySet()) {
            writeGettersAndSetters(writer, usefulTypeMirrors, classElement, purpose, direction, propertyInformationEntry);
          }
        }

        writer.write("}");
        writer.newLine();
      }
    }
  }

  private void writeField (BufferedWriter writer, String purpose, Direction direction, Map.Entry<String, PropertyInformation> propertyInformationEntry)
    throws IOException {

    for (ConstraintInformation constraintInformation : propertyInformationEntry.getValue().getConstraints()) {
      writer.write("  @");
      writer.write(constraintInformation.getType().toString());
      if (!constraintInformation.getArguments().isEmpty()) {
        writer.write("(");
        writer.write(constraintInformation.getArguments());
        writer.write(")");
      }
      writer.newLine();
    }
    writer.write("  private ");
    writer.write(asCompatibleName(propertyInformationEntry.getValue().getType(), purpose, direction));
    writer.write(" ");
    writer.write(propertyInformationEntry.getKey());
    writer.write(";");
    writer.newLine();
  }

  private void writeGettersAndSetters (BufferedWriter writer, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, String purpose, Direction direction, Map.Entry<String, PropertyInformation> propertyInformationEntry)
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

    writer.write("  public ");
    writer.write(asDtoName(classElement.getSimpleName(), purpose, direction).toString());
    writer.write(" ");
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
    writer.newLine();
    writer.write("    return this;");
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
}