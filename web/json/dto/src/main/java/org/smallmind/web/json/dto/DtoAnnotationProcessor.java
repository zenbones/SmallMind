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
package org.smallmind.web.json.dto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.web.json.dto.translator.DtoTranslatorFactory;

@SupportedAnnotationTypes({"org.smallmind.web.json.dto.DtoGenerator"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"prefix"})
@AutoService(Processor.class)
public class DtoAnnotationProcessor extends AbstractProcessor {

  private final VisibilityTracker visibilityTracker = new VisibilityTracker();
  private final ClassTracker classTracker = new ClassTracker();
  private final HashSet<TypeElement> processedSet = new HashSet<>();

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

    for (TypeElement typeElement : new TypeElementIterable(processingEnv, typeMirror)) {
      generate(typeElement);
    }
  }

  private void generate (TypeElement classElement)
    throws IOException, DtoDefinitionException {

    if (!processedSet.contains(classElement)) {

      AnnotationMirror dtoGeneratorAnnotationMirror;

      if ((dtoGeneratorAnnotationMirror = AptUtility.extractAnnotationMirror(processingEnv, classElement, processingEnv.getElementUtils().getTypeElement(DtoGenerator.class.getName()).asType())) != null) {
        if (classTracker.isPreCompiled(classElement)) {
        } else {
          if ((!ElementKind.CLASS.equals(classElement.getKind())) || (!NestingKind.TOP_LEVEL.equals(classElement.getNestingKind()))) {
            throw new DtoDefinitionException("The class(%s) must be a root implementation of type 'class'", classElement.getQualifiedName());
          } else {

            UsefulTypeMirrors usefulTypeMirrors = new UsefulTypeMirrors(processingEnv);
            GeneratorInformation generatorInformation;
            TypeElement nearestDtoSuperclass;

            processedSet.add(classElement);
            if ((nearestDtoSuperclass = getNearestDtoSuperclass(classElement)) != null) {
              generate(nearestDtoSuperclass);
            }

            generatorInformation = new GeneratorInformation(processingEnv, usefulTypeMirrors, this, classElement, visibilityTracker, classTracker, dtoGeneratorAnnotationMirror);
            ClassWalker.walk(processingEnv, this, classElement, generatorInformation, usefulTypeMirrors);
            generatorInformation.update(classElement, visibilityTracker);

            for (TypeElement polymorphicSubClass : classTracker.getPolymorphicSubclasses(classElement)) {
              visibilityTracker.add(polymorphicSubClass, classElement);
              generate(polymorphicSubClass);
            }

            for (Map.Entry<String, PropertyLexicon> purposeEntry : generatorInformation.getInDirectionalGuide().entrySet()) {
              processIn(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purposeEntry.getKey(), purposeEntry.getValue());
            }
            for (String unfulfilledPurpose : generatorInformation.unfulfilledPurposes(classElement, visibilityTracker, Direction.IN)) {
              processIn(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, unfulfilledPurpose, new PropertyLexicon());
            }
            for (Map.Entry<String, PropertyLexicon> purposeEntry : generatorInformation.getOutDirectionalGuide().entrySet()) {
              processOut(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purposeEntry.getKey(), purposeEntry.getValue());
            }
            for (String unfulfilledPurpose : generatorInformation.unfulfilledPurposes(classElement, visibilityTracker, Direction.OUT)) {
              processOut(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, unfulfilledPurpose, new PropertyLexicon());
            }

            if (visibilityTracker.hasNoPurpose(classElement) && (!classElement.getModifiers().contains(Modifier.ABSTRACT))) {
              throw new DtoDefinitionException("The class(%s) was annotated as @%s but contained no properties", classElement.getQualifiedName(), DtoGenerator.class.getSimpleName());
            } else {

              String[] inOverwroughtPurposes = generatorInformation.overwroughtPurposes(classElement, visibilityTracker, Direction.IN);
              String[] outOverwroughtPurposes = generatorInformation.overwroughtPurposes(classElement, visibilityTracker, Direction.OUT);

              if ((inOverwroughtPurposes.length > 0) || (outOverwroughtPurposes.length > 0)) {

                StringBuilder warningBuilder = new StringBuilder("The class(").append(classElement.getQualifiedName()).append(") was annotated with the unnecessary @Pledge(");

                if (inOverwroughtPurposes.length > 0) {
                  warningBuilder.append("IN").append(Arrays.toString(inOverwroughtPurposes));
                }
                if (outOverwroughtPurposes.length > 0) {
                  if (inOverwroughtPurposes.length > 0) {
                    warningBuilder.append(" , ");
                  }
                  warningBuilder.append("OUT").append(Arrays.toString(outOverwroughtPurposes));
                }

                warningBuilder.append(')');

                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, warningBuilder, classElement);
              }
            }
          }
        }
      }
    }
  }

  private void processIn (GeneratorInformation generatorInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestDtoSuperclass, String purpose, PropertyLexicon propertyLexicon)
    throws IOException {

    writeDto(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purpose, Direction.IN, propertyLexicon);

    generatorInformation.denotePurpose(purpose, Direction.IN);
  }

  private void processOut (GeneratorInformation generatorInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestDtoSuperclass, String purpose, PropertyLexicon propertyLexicon)
    throws IOException {

    writeDto(generatorInformation, usefulTypeMirrors, classElement, nearestDtoSuperclass, purpose, Direction.OUT, propertyLexicon);

    generatorInformation.denotePurpose(purpose, Direction.OUT);
  }

  private String asMemberName (Name name) {

    return Character.toLowerCase(name.charAt(0)) + name.subSequence(1, name.length()).toString();
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

  private void writeDto (GeneratorInformation generatorInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestDtoSuperclass, String purpose, Direction direction, PropertyLexicon propertyLexicon)
    throws IOException {

    JavaFileObject sourceFile;

    if (classTracker.hasPolymorphicSubClasses(classElement)) {
      writePolymorphicAdapter(classElement, purpose, direction);
    }

    sourceFile = processingEnv.getFiler().createSourceFile(new StringBuilder(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName()).append('.').append(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement)), classElement);

    if (sourceFile.getNestingKind() == null) {
      try (BufferedWriter writer = new BufferedWriter(sourceFile.openWriter())) {

        LinkedList<TypeElement> matchingSubClassList = new LinkedList<>();

        // package
        writer.write("package ");
        writer.write(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString());
        writer.write(";");
        writer.newLine();
        writer.newLine();

        for (TypeElement polymorphicSubClass : classTracker.getPolymorphicSubclasses(classElement)) {

          Visibility visibility;

          if (((visibility = visibilityTracker.getVisibility(polymorphicSubClass, purpose)) != null) && visibility.matches(direction)) {
            matchingSubClassList.add(polymorphicSubClass);
          }
        }

        // imports
        writer.write("import javax.annotation.Generated;");
        writer.newLine();
        if ((nearestDtoSuperclass == null)) {
          writer.write("import javax.xml.bind.annotation.XmlAccessType;");
          writer.newLine();
          writer.write("import javax.xml.bind.annotation.XmlAccessorType;");
          writer.newLine();
        }
        writer.write("import javax.xml.bind.annotation.XmlElement;");
        writer.newLine();
        if (!classElement.getModifiers().contains(Modifier.ABSTRACT)) {
          writer.write("import javax.xml.bind.annotation.XmlRootElement;");
          writer.newLine();
        }
        writer.write("import javax.xml.bind.annotation.XmlAnyElement;");
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
          writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, nearestDtoSuperclass));
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
        if (!classElement.getModifiers().contains(Modifier.ABSTRACT)) {
          writer.write("@XmlRootElement(name = \"");
          writer.write(generatorInformation.getName().isEmpty() ? asMemberName(classElement.getSimpleName()) : generatorInformation.getName());
          writer.write("\")");
          writer.newLine();
        }

        // XmlAccessorType
        if ((nearestDtoSuperclass == null)) {
          writer.write("@XmlAccessorType(XmlAccessType.PROPERTY)");
          writer.newLine();
        }

        if (classTracker.isPolymorphic(classElement)) {
          // XmlJavaTypeAdapter
          writer.write("@XmlJavaTypeAdapter(");
          writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, (classTracker.hasPolymorphicSubClasses(classElement) ? classElement : classTracker.getPolymorphicBaseClass(classElement))));
          if (classTracker.hasPolymorphicSubClasses(classElement) ? classTracker.usePolymorphicAttribute(classElement) : classTracker.usePolymorphicAttribute(classTracker.getPolymorphicBaseClass(classElement))) {
            writer.write("Attributed");
          }
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
              writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, polymorphicSubClass));
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

        // class level constraints
        writeConstraints(writer, generatorInformation.constraints(), 0);

        // class declaration
        writer.write("public ");
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
          writer.write("abstract ");
        }
        writer.write("class ");
        writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        if (classTracker.hasPolymorphicSubClasses(classElement)) {
          writer.write("<D extends ");
          writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
          writer.write("<D>>");
        }
        if (nearestDtoSuperclass != null) {
          writer.write(" extends ");
          writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, nearestDtoSuperclass));
          if (classTracker.hasPolymorphicBaseClass(classElement)) {
            writer.write("<");
            writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
            writer.write(">");
          }
        }
        writer.write(" {");
        writer.newLine();

        // virtual field declarations
        if (propertyLexicon.isVirtual()) {
          writer.newLine();
          writer.write("  // virtual fields");
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyLexicon.getVirtualMap().entrySet()) {
            writeField(writer, purpose, direction, propertyInformationEntry);
          }
        }

        // native field declarations
        if (propertyLexicon.isReal()) {
          writer.newLine();
          writer.write("  // native fields");
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyLexicon.getRealMap().entrySet()) {
            writeField(writer, purpose, direction, propertyInformationEntry);
          }
        }

        // static instance creator
        writer.newLine();
        writer.write("  public static ");
        writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        writer.write(" instance (");
        writer.write(classElement.getSimpleName().toString());
        writer.write(" ");
        writer.write(asMemberName(classElement.getSimpleName()));
        writer.write(") {");
        writer.newLine();
        writer.newLine();
        if (classTracker.hasPolymorphicSubClasses(classElement)) {

          boolean firstPolymorphicSubClass = true;

          for (TypeElement polymorphicSubClass : classTracker.getPolymorphicSubclasses(classElement)) {
            if (firstPolymorphicSubClass) {
              firstPolymorphicSubClass = false;
              writer.write("    ");
            } else {
              writer.write(" else ");
            }
            writer.write("if (");
            writer.write(asMemberName(classElement.getSimpleName()));
            writer.write(" instanceof ");
            writer.write(polymorphicSubClass.getQualifiedName().toString());
            writer.write(") {");
            writer.newLine();
            writer.write("      return ");
            writer.write(DtoNameUtility.getPackageName(processingEnv, polymorphicSubClass));
            writer.write(".");
            writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, polymorphicSubClass));
            writer.write(".instance((");
            writer.write(polymorphicSubClass.getQualifiedName().toString());
            writer.write(")");
            writer.write(asMemberName(classElement.getSimpleName()));
            writer.write(");");
            writer.newLine();
            writer.write("    }");
          }
          writer.write(" else {");
          writer.newLine();
          if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            writer.write("      throw new IllegalStateException(\"Unable to find a known polymorphic dto subclass for type(\" + ");
            writer.write(asMemberName(classElement.getSimpleName()));
            writer.write(".getClass().getName()");
            writer.write(" + \")\");");
            writer.newLine();
          } else {
            writer.write("      ");
            writeSelfConstruction(writer, classElement, purpose, direction);
          }
          writer.write("    }");
          writer.newLine();
        } else {
          writer.write("    ");
          writeSelfConstruction(writer, classElement, purpose, direction);
        }
        writer.write("  }");
        writer.newLine();

        // constructors
        writer.newLine();
        writer.write("  public ");
        writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        writer.write(" () {");
        writer.newLine();
        writer.newLine();
        writer.write("  }");
        writer.newLine();
        writer.newLine();

        writer.write("  public ");
        writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
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
          if (propertyLexicon.isReal()) {
            writer.newLine();
          }
        }

        if (propertyLexicon.isReal()) {
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyLexicon.getRealMap().entrySet()) {
            writer.write("    this.");
            writer.write(propertyInformationEntry.getKey());
            writer.write(" = ");
            DtoTranslatorFactory.create(processingEnv, usefulTypeMirrors, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()).writeRightSideOfEquals(writer, processingEnv, asMemberName(classElement.getSimpleName()), propertyInformationEntry.getKey(), propertyInformationEntry.getValue().getType(), DtoNameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()));
            writer.newLine();
          }
        }
        writer.write("  }");
        writer.newLine();
        writer.newLine();

        // entity factory
        writer.write("  public ");
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
          writer.write("abstract ");
        }
        writer.write(classElement.getSimpleName().toString());
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
          writer.write(" factory ();");
        } else {
          writer.write(" factory () {");
          writer.newLine();
          writer.newLine();
          writer.write("    return factory(new ");
          writer.write(classElement.getQualifiedName().toString());
          writer.write("());");
          writer.newLine();
          writer.write("  }");
        }
        writer.newLine();
        writer.newLine();

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

        if (propertyLexicon.isReal()) {
          writer.newLine();
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyLexicon.getRealMap().entrySet()) {
            writer.write("    ");
            writer.write(asMemberName(classElement.getSimpleName()));
            writer.write(".");
            writer.write(BeanUtility.asSetterName(propertyInformationEntry.getKey()));
            writer.write("(");
            DtoTranslatorFactory.create(processingEnv, usefulTypeMirrors, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()).writeInsideOfSet(writer, processingEnv, propertyInformationEntry.getValue().getType(), DtoNameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()), propertyInformationEntry.getKey());
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
        if (propertyLexicon.isVirtual()) {
          writer.newLine();
          writer.write("  // virtual getters and setters");
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyLexicon.getVirtualMap().entrySet()) {
            writer.newLine();
            writeGettersAndSetters(writer, usefulTypeMirrors, classElement, purpose, direction, propertyInformationEntry);
          }
        }

        // native getters and setters
        if (propertyLexicon.isReal()) {
          writer.newLine();
          writer.write("  // native getters and setters");
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyLexicon.getRealMap().entrySet()) {
            writer.newLine();
            writeGettersAndSetters(writer, usefulTypeMirrors, classElement, purpose, direction, propertyInformationEntry);
          }
        }

        writer.write("}");
        writer.newLine();
      }
    }
  }

  private void writeSelfConstruction (BufferedWriter writer, TypeElement classElement, String purpose, Direction direction)
    throws IOException {

    writer.write("return new ");
    writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
    writer.write("(");
    writer.write(asMemberName(classElement.getSimpleName()));
    writer.write(");");
    writer.newLine();
  }

  private void writeField (BufferedWriter writer, String purpose, Direction direction, Map.Entry<String, PropertyInformation> propertyInformationEntry)
    throws IOException {

    writeConstraints(writer, propertyInformationEntry.getValue().constraints(), 2);
    writer.write("  private ");
    writer.write(DtoNameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()));
    writer.write(" ");
    writer.write(propertyInformationEntry.getKey());
    writer.write(";");
    writer.newLine();
  }

  private void writeConstraints (BufferedWriter writer, Iterable<ConstraintInformation> constraints, int indent)
    throws IOException {

    for (ConstraintInformation constraintInformation : constraints) {
      for (int count = 0; count < indent; count++) {
        writer.write(" ");
      }
      writer.write("@");
      writer.write(constraintInformation.getType().toString());
      if (!constraintInformation.getArguments().isEmpty()) {
        writer.write("(");
        writer.write(constraintInformation.getArguments());
        writer.write(")");
      }
      writer.newLine();
    }
  }

  private void writeGettersAndSetters (BufferedWriter writer, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, String purpose, Direction direction, Map.Entry<String, PropertyInformation> propertyInformationEntry)
    throws IOException {

    if (propertyInformationEntry.getValue().getAdapter() != null) {
      writer.write("  @XmlJavaTypeAdapter(");
      writer.write(propertyInformationEntry.getValue().getAdapter().toString());
      writer.write(".class)");
      writer.newLine();
    } else if (processingEnv.getTypeUtils().isSameType(usefulTypeMirrors.getObjectTypeMirror(), propertyInformationEntry.getValue().getType()) || processingEnv.getTypeUtils().isSameType(usefulTypeMirrors.getJsonNodeTypeMirror(), propertyInformationEntry.getValue().getType())) {
      writer.write("  @XmlAnyElement");
      writer.newLine();
    }
    writer.write("  @XmlElement(name = \"");
    writer.write(propertyInformationEntry.getValue().getName().isEmpty() ? propertyInformationEntry.getKey() : propertyInformationEntry.getValue().getName());
    writer.write(propertyInformationEntry.getValue().isRequired() ? "\", required = true)" : "\")");
    writer.newLine();
    writer.write("  public ");
    writer.write(DtoNameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()));
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
    if (classTracker.hasPolymorphicSubClasses(classElement)) {
      writer.write("D");
    } else {
      writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
    }
    writer.write(" ");
    writer.write(BeanUtility.asSetterName(propertyInformationEntry.getKey()));
    writer.write(" (");
    writer.write(DtoNameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()));
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
    writer.write("    return ");
    if (classTracker.hasPolymorphicSubClasses(classElement)) {
      writer.write("(D)");
    }
    writer.write("this;");
    writer.newLine();
    writer.write("  }");
    writer.newLine();
  }

  private void writePolymorphicAdapter (TypeElement classElement, String purpose, Direction direction)
    throws IOException {

    JavaFileObject sourceFile;
    StringBuilder sourceFileBuilder = new StringBuilder(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName()).append('.').append(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));

    if (classTracker.usePolymorphicAttribute(classElement)) {
      sourceFileBuilder.append("Attributed");
    }
    sourceFileBuilder.append("PolymorphicXmlAdapter");

    sourceFile = processingEnv.getFiler().createSourceFile(sourceFileBuilder, classElement);

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
        writer.write("import org.smallmind.web.json.scaffold.util.");
        if (classTracker.usePolymorphicAttribute(classElement)) {
          writer.write("Attributed");
        }
        writer.write("PolymorphicXmlAdapter;");
        writer.newLine();
        writer.newLine();

        // @Generated
        writer.write("@Generated(\"");
        writer.write(DtoAnnotationProcessor.class.getName());
        writer.write("\")");
        writer.newLine();

        // class declaration
        writer.write("public class ");
        writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        if (classTracker.usePolymorphicAttribute(classElement)) {
          writer.write("Attributed");
        }
        writer.write("PolymorphicXmlAdapter");
        writer.write(" extends ");
        if (classTracker.usePolymorphicAttribute(classElement)) {
          writer.write("Attributed");
        }
        writer.write("PolymorphicXmlAdapter<");
        writer.write(DtoNameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        writer.write("> {");
        writer.newLine();
        writer.newLine();
        writer.write("}");
        writer.newLine();
      }
    }
  }
}