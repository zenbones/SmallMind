/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.web.json.doppelganger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
import org.smallmind.web.json.doppelganger.translator.TranslatorFactory;

/**
 * Annotation processor that scans {@link Doppelganger}-annotated classes and generates JAXB/Jackson friendly
 * view classes for inbound and outbound JSON/XML serialization. The processor also emits polymorphic adapters
 * when needed and validates annotation usage.
 */
@SupportedAnnotationTypes("org.smallmind.web.json.doppelganger.Doppelganger")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions("prefix")
@AutoService(Processor.class)
public class DoppelgangerAnnotationProcessor extends AbstractProcessor {

  private final VisibilityTracker visibilityTracker = new VisibilityTracker();
  private final ClassTracker classTracker = new ClassTracker();
  private final HashSet<TypeElement> processedSet = new HashSet<>();

  /**
   * Entrypoint invoked by the compiler for each processing round. Collects all {@link Doppelganger} elements
   * and generates corresponding views.
   *
   * @param annotations annotation types requested for processing
   * @param roundEnv    environment for the current round
   * @return {@code true} to indicate the annotations are fully handled
   */
  @Override
  public boolean process (Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Doppelganger.class)) {
      try {
        generate((TypeElement)annotatedElement);
      } catch (Exception exception) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
      }
    }

    return true;
  }

  /**
   * Ensures that any referenced types are also processed for generated views.
   *
   * @param typeMirror the referenced type to inspect
   * @throws IOException           if source generation fails
   * @throws DefinitionException   if referenced definitions are invalid
   */
  public void processTypeMirror (TypeMirror typeMirror)
    throws IOException, DefinitionException {

    for (TypeElement typeElement : new TypeElementIterable(processingEnv, typeMirror)) {
      generate(typeElement);
    }
  }

  /**
   * Generates views for a specific {@link Doppelganger}-annotated class (and any hierarchical/ polymorphic relatives).
   *
   * @param classElement the root class to process
   * @throws IOException         if writing a generated source fails
   * @throws DefinitionException if the class definition violates processing rules
   */
  private void generate (TypeElement classElement)
    throws IOException, DefinitionException {

    if (!processedSet.contains(classElement)) {

      AnnotationMirror doppelgangerAnnotationMirror;

      if ((doppelgangerAnnotationMirror = AptUtility.extractAnnotationMirror(processingEnv, classElement, processingEnv.getElementUtils().getTypeElement(Doppelganger.class.getName()).asType())) != null) {
        if (!classTracker.isPreCompiled(classElement)) {
          if ((!ElementKind.CLASS.equals(classElement.getKind())) || (!NestingKind.TOP_LEVEL.equals(classElement.getNestingKind()))) {
            throw new DefinitionException("The class(%s) must be a root implementation of type 'class'", classElement.getQualifiedName());
          } else {

            UsefulTypeMirrors usefulTypeMirrors = new UsefulTypeMirrors(processingEnv);
            DoppelgangerInformation doppelgangerInformation;
            TypeElement nearestViewSuperclass;

            processedSet.add(classElement);
            if ((nearestViewSuperclass = getNearestViewSuperclass(classElement)) != null) {
              generate(nearestViewSuperclass);
            }

            doppelgangerInformation = new DoppelgangerInformation(processingEnv, usefulTypeMirrors, this, classElement, visibilityTracker, classTracker, doppelgangerAnnotationMirror);
            ClassWalker.walk(processingEnv, this, classElement, doppelgangerInformation, usefulTypeMirrors);
            doppelgangerInformation.update(classElement, visibilityTracker);

            for (TypeElement polymorphicSubClass : classTracker.getPolymorphicSubclasses(classElement)) {
              visibilityTracker.add(polymorphicSubClass, classElement);
              generate(polymorphicSubClass);
            }
            for (TypeElement hierarchySubClass : classTracker.getHierarchySubclasses(classElement)) {
              visibilityTracker.add(hierarchySubClass, classElement);
              generate(hierarchySubClass);
            }

            for (Map.Entry<String, PropertyLexicon> purposeEntry : doppelgangerInformation.getInDirectionalGuide().lexiconEntrySet()) {
              processIn(doppelgangerInformation, usefulTypeMirrors, classElement, nearestViewSuperclass, purposeEntry.getKey(), purposeEntry.getValue());
            }
            for (String unfulfilledPurpose : doppelgangerInformation.unfulfilledPurposes(classElement, visibilityTracker, Direction.IN)) {
              processIn(doppelgangerInformation, usefulTypeMirrors, classElement, nearestViewSuperclass, unfulfilledPurpose, new PropertyLexicon());
            }
            for (Map.Entry<String, PropertyLexicon> purposeEntry : doppelgangerInformation.getOutDirectionalGuide().lexiconEntrySet()) {
              processOut(doppelgangerInformation, usefulTypeMirrors, classElement, nearestViewSuperclass, purposeEntry.getKey(), purposeEntry.getValue());
            }
            for (String unfulfilledPurpose : doppelgangerInformation.unfulfilledPurposes(classElement, visibilityTracker, Direction.OUT)) {
              processOut(doppelgangerInformation, usefulTypeMirrors, classElement, nearestViewSuperclass, unfulfilledPurpose, new PropertyLexicon());
            }

            if (visibilityTracker.hasNoPurpose(classElement) && (!classElement.getModifiers().contains(Modifier.ABSTRACT))) {
              throw new DefinitionException("The class(%s) was annotated as @%s but contained no properties", classElement.getQualifiedName(), Doppelganger.class.getSimpleName());
            } else {

              String[] inOverwroughtPurposes = doppelgangerInformation.overwroughtPurposes(classElement, visibilityTracker, Direction.IN);
              String[] outOverwroughtPurposes = doppelgangerInformation.overwroughtPurposes(classElement, visibilityTracker, Direction.OUT);

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

  /**
   * Writes an inbound view for a purpose and records fulfillment.
   *
   * @param doppelgangerInformation accumulated metadata for the class
   * @param usefulTypeMirrors       cached type mirrors
   * @param classElement            the class being processed
   * @param nearestViewSuperclass   nearest ancestor view to extend, if any
   * @param purpose                 idiom purpose value
   * @param propertyLexicon         properties to include
   * @throws IOException         if source generation fails
   * @throws DefinitionException if configuration is invalid
   */
  private void processIn (DoppelgangerInformation doppelgangerInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestViewSuperclass, String purpose, PropertyLexicon propertyLexicon)
    throws IOException, DefinitionException {

    writeView(doppelgangerInformation, usefulTypeMirrors, classElement, nearestViewSuperclass, purpose, Direction.IN, propertyLexicon);

    doppelgangerInformation.denotePurpose(purpose, Direction.IN);
  }

  /**
   * Writes an outbound view for a purpose and records fulfillment.
   *
   * @param doppelgangerInformation accumulated metadata for the class
   * @param usefulTypeMirrors       cached type mirrors
   * @param classElement            the class being processed
   * @param nearestViewSuperclass   nearest ancestor view to extend, if any
   * @param purpose                 idiom purpose value
   * @param propertyLexicon         properties to include
   * @throws IOException         if source generation fails
   * @throws DefinitionException if configuration is invalid
   */
  private void processOut (DoppelgangerInformation doppelgangerInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestViewSuperclass, String purpose, PropertyLexicon propertyLexicon)
    throws IOException, DefinitionException {

    writeView(doppelgangerInformation, usefulTypeMirrors, classElement, nearestViewSuperclass, purpose, Direction.OUT, propertyLexicon);

    doppelgangerInformation.denotePurpose(purpose, Direction.OUT);
  }

  /**
   * Converts a type name into a Java-friendly member name (lowercase first character).
   *
   * @param name the original simple name
   * @return the decapitalized member name
   */
  private String asMemberName (Name name) {

    return Character.toLowerCase(name.charAt(0)) + name.subSequence(1, name.length()).toString();
  }

  /**
   * Finds the closest superclass that is itself annotated with {@link Doppelganger}.
   *
   * @param classElement the class whose ancestors should be examined
   * @return the nearest annotated superclass, or {@code null} if none exist
   */
  private TypeElement getNearestViewSuperclass (TypeElement classElement) {

    TypeElement currentClassElement = classElement;

    while ((currentClassElement = (TypeElement)processingEnv.getTypeUtils().asElement(currentClassElement.getSuperclass())) != null) {
      if (currentClassElement.getAnnotation(Doppelganger.class) != null) {

        return currentClassElement;
      }
    }

    return null;
  }

  /**
   * Checks if a purpose string is present in a list, treating {@code null}/empty purpose as matching the empty list.
   *
   * @param purpose     purpose text to test
   * @param purposeList list of purposes in an idiom
   * @return {@code true} if the purpose should be considered present
   */
  private boolean hasPurpose (String purpose, List<String> purposeList) {

    if ((purpose == null) || purpose.isEmpty()) {

      return purposeList.isEmpty();
    } else {

      return purposeList.contains(purpose);
    }
  }

  /**
   * Generates a concrete view class for a direction/purpose combination.
   *
   * @param doppelgangerInformation accumulated metadata for the class
   * @param usefulTypeMirrors       cached type mirrors
   * @param classElement            the class being processed
   * @param nearestViewSuperclass   nearest ancestor view to extend, if any
   * @param purpose                 idiom purpose value
   * @param direction               IN or OUT
   * @param propertyLexicon         properties to include
   * @throws IOException         if writing a generated source fails
   * @throws DefinitionException if invalid configuration is detected
   */
  private void writeView (DoppelgangerInformation doppelgangerInformation, UsefulTypeMirrors usefulTypeMirrors, TypeElement classElement, TypeElement nearestViewSuperclass, String purpose, Direction direction, PropertyLexicon propertyLexicon)
    throws IOException, DefinitionException {

    JavaFileObject sourceFile;

    if (classTracker.hasPolymorphicSubClasses(classElement)) {
      writePolymorphicAdapter(classElement, purpose, direction, classTracker.usePolymorphicAttribute(classElement));
    }

    sourceFile = processingEnv.getFiler().createSourceFile(new StringBuilder(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName()).append('.').append(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement)), classElement);

    if (sourceFile.getNestingKind() == null) {
      try (BufferedWriter writer = new BufferedWriter(sourceFile.openWriter())) {

        HashSet<TypeMirror> implementationSet = new HashSet<>();
        List<TypeElement> subclassList;
        LinkedList<TypeElement> matchingPolymorphicSubClassList = new LinkedList<>();
        LinkedList<String> getterList = new LinkedList<>();
        String[] imports;
        boolean hasPolymorphicSubclasses;
        boolean hasHierarchySubclasses;

        if (classTracker.hasPolymorphicSubClasses(classElement)) {
          hasPolymorphicSubclasses = true;
          hasHierarchySubclasses = false;
          subclassList = classTracker.getPolymorphicSubclasses(classElement);
        } else if (classTracker.hasHierarchySubClasses(classElement)) {
          hasPolymorphicSubclasses = false;
          hasHierarchySubclasses = true;
          subclassList = classTracker.getHierarchySubclasses(classElement);
        } else {
          hasPolymorphicSubclasses = false;
          hasHierarchySubclasses = false;
          subclassList = Collections.emptyList();
        }

        // package
        writer.write("package ");
        writer.write(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString());
        writer.write(";");
        writer.newLine();
        writer.newLine();

        if (hasPolymorphicSubclasses && (!subclassList.isEmpty())) {

          String packageName = processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString();

          for (TypeElement subClass : subclassList) {

            Visibility visibility;

            if (((visibility = visibilityTracker.getVisibility(subClass, purpose)) != null) && visibility.matches(direction)) {
              if (!packageName.equals(processingEnv.getElementUtils().getPackageOf(subClass).getQualifiedName().toString())) {
                throw new DefinitionException("The class(%s) must be in package(%s)", subClass, packageName);
              } else {
                matchingPolymorphicSubClassList.add(subClass);
              }
            }
          }
        }

        // imports
        writer.write("import java.util.Objects;");
        writer.newLine();
        writer.write("import jakarta.annotation.Generated;");
        writer.newLine();
        if ((nearestViewSuperclass == null)) {
          writer.write("import jakarta.xml.bind.annotation.XmlAccessType;");
          writer.newLine();
          writer.write("import jakarta.xml.bind.annotation.XmlAccessorType;");
          writer.newLine();
        }
        writer.write("import jakarta.xml.bind.annotation.XmlElement;");
        writer.newLine();
        if (!classElement.getModifiers().contains(Modifier.ABSTRACT)) {
          writer.write("import jakarta.xml.bind.annotation.XmlRootElement;");
          writer.newLine();
        }
        writer.write("import jakarta.xml.bind.annotation.XmlAnyElement;");
        writer.newLine();
        writer.write("import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;");
        writer.newLine();
        if (propertyLexicon.hasAs()) {
          writer.write("import org.smallmind.web.json.scaffold.util.As;");
          writer.newLine();
        }
        if (propertyLexicon.hasNullifier()) {
          writer.write("import org.smallmind.web.json.scaffold.util.NullifiedBy;");
          writer.newLine();
        }
        if ((!doppelgangerInformation.getComment().isEmpty()) || propertyLexicon.hasComment()) {
          writer.write("import org.smallmind.web.json.scaffold.util.Comment;");
          writer.newLine();
        }
        if (!matchingPolymorphicSubClassList.isEmpty()) {
          writer.write("import org.smallmind.web.json.scaffold.util.XmlPolymorphicSubClasses;");
          writer.newLine();
        }

        if ((nearestViewSuperclass != null) && (!Objects.equals(processingEnv.getElementUtils().getPackageOf(classElement), processingEnv.getElementUtils().getPackageOf(nearestViewSuperclass)))) {
          writer.write("import ");
          writer.write(processingEnv.getElementUtils().getPackageOf(nearestViewSuperclass).getQualifiedName().toString());
          writer.write(".");
          writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, nearestViewSuperclass));
          writer.write(";");
          writer.newLine();
        }
        writer.newLine();

        if ((imports = doppelgangerInformation.getImports(direction, purpose)).length > 0) {
          writer.write("// additional imports");
          writer.newLine();
          for (String doppelgangerImport : imports) {
            writer.write("import ");
            writer.write(doppelgangerImport);
            writer.write(";");
            writer.newLine();
          }
          writer.newLine();
        }

        // @Generated
        writer.write("@Generated(\"");
        writer.write(DoppelgangerAnnotationProcessor.class.getName());
        writer.write("\")");
        writer.newLine();

        // @Comment
        if (!doppelgangerInformation.getComment().isEmpty()) {
          writer.write("@Comment(\"");
          writer.write(doppelgangerInformation.getComment());
          writer.write("\")");
          writer.newLine();
        }

        // @XmlRootElement
        if (!classElement.getModifiers().contains(Modifier.ABSTRACT)) {
          writer.write("@XmlRootElement(name = \"");
          writer.write(doppelgangerInformation.getName().isEmpty() ? asMemberName(classElement.getSimpleName()) : doppelgangerInformation.getName());
          if (!doppelgangerInformation.getNamespace().isEmpty()) {
            writer.write("\", namespace = \"");
            writer.write(doppelgangerInformation.getNamespace());
          }
          writer.write("\")");
          writer.newLine();
        }

        // XmlAccessorType
        if ((nearestViewSuperclass == null)) {
          writer.write("@XmlAccessorType(XmlAccessType.PROPERTY)");
          writer.newLine();
        }

        if (classTracker.isPolymorphic(classElement)) {
          // XmlJavaTypeAdapter
          writer.write("@XmlJavaTypeAdapter(");
          writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, (hasPolymorphicSubclasses ? classElement : classTracker.getPolymorphicBaseClass(classElement))));
          if (hasPolymorphicSubclasses ? classTracker.usePolymorphicAttribute(classElement) : classTracker.usePolymorphicAttribute(classTracker.getPolymorphicBaseClass(classElement))) {
            writer.write("Attributed");
          }
          writer.write("PolymorphicXmlAdapter.class)");
          writer.newLine();

          // XmlPolymorphicSubClasses
          if (!matchingPolymorphicSubClassList.isEmpty()) {

            boolean firstPolymorphicSubClass = true;
            writer.write("@XmlPolymorphicSubClasses(");
            if (matchingPolymorphicSubClassList.size() > 1) {
              writer.write("{");
            }

            for (TypeElement polymorphicSubClass : matchingPolymorphicSubClassList) {
              if (!firstPolymorphicSubClass) {
                writer.write(", ");
              }
              writer.write(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString());
              writer.write(".");
              writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, polymorphicSubClass));
              writer.write(".class");

              firstPolymorphicSubClass = false;
            }

            if (matchingPolymorphicSubClassList.size() > 1) {
              writer.write("}");
            }
            writer.write(")");
            writer.newLine();
          }
        }

        // class level constraints
        writeConstrainingIdioms(writer, purpose, direction, doppelgangerInformation.constrainingIdioms());

        // class declaration
        writer.write("public ");
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
          writer.write("abstract ");
        }
        writer.write("class ");
        writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        if (hasPolymorphicSubclasses || hasHierarchySubclasses) {
          writer.write("<D extends ");
          writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
          writer.write("<D>>");
        }
        if (nearestViewSuperclass != null) {
          writer.write(" extends ");
          writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, nearestViewSuperclass));
          if (classTracker.hasPolymorphicBaseClass(classElement) || classTracker.hasHierarchyBaseClass(classElement)) {
            writer.write("<");
            if (hasPolymorphicSubclasses || hasHierarchySubclasses) {
              writer.write("D");
            } else {
              writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
            }
            writer.write(">");
          }
        }

        if (doppelgangerInformation.isSerializable()) {
          implementationSet.add(usefulTypeMirrors.getSerializableTypeMirror());
        }
        implementationSet.addAll(Arrays.asList(doppelgangerInformation.getImplementations(direction, purpose)));
        if (!implementationSet.isEmpty()) {

          boolean first = true;

          writer.write(" implements ");
          for (TypeMirror implementedType : implementationSet) {
            if (!first) {
              writer.write(", ");
            }
            writer.write(implementedType.toString());
            first = false;
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

        if (hasPolymorphicSubclasses || hasHierarchySubclasses) {
          writer.write("<T extends ");
          writer.write(classElement.getSimpleName().toString());
          writer.write("> ");
        }

        writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));

        if (hasPolymorphicSubclasses || hasHierarchySubclasses) {
          writer.write("<?>");
        }

        writer.write(" instance (");

        if (hasPolymorphicSubclasses || hasHierarchySubclasses) {
          writer.write("T ");
        } else {
          writer.write(classElement.getSimpleName().toString());
          writer.write(" ");
        }

        writer.write(asMemberName(classElement.getSimpleName()));
        writer.write(") {");
        writer.newLine();
        writer.newLine();
        if (hasPolymorphicSubclasses || hasHierarchySubclasses) {

          boolean firstSubClass = true;

          for (TypeElement subClass : subclassList) {
            if (firstSubClass) {
              firstSubClass = false;
              writer.write("    ");
            } else {
              writer.write(" else ");
            }
            writer.write("if (");
            writer.write(asMemberName(classElement.getSimpleName()));
            writer.write(" instanceof ");
            writer.write(subClass.getQualifiedName().toString());
            writer.write(") {");
            writer.newLine();
            writer.write("      return ");
            writer.write(NameUtility.getPackageName(processingEnv, subClass));
            writer.write(".");
            writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, subClass));
            writer.write(".instance((");
            writer.write(subClass.getQualifiedName().toString());
            writer.write(")");
            writer.write(asMemberName(classElement.getSimpleName()));
            writer.write(");");
            writer.newLine();
            writer.write("    }");
          }
          writer.write(" else {");
          writer.newLine();
          if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            writer.write("      throw new IllegalStateException(\"Unable to find a known polymorphic view subclass for type(\" + ");
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
        writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        writer.write(" () {");
        writer.newLine();
        writer.newLine();
        writer.write("  }");
        writer.newLine();
        writer.newLine();

        writer.write("  public ");
        writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        writer.write(" (");
        writer.write(classElement.getSimpleName().toString());
        writer.write(" ");
        writer.write(asMemberName(classElement.getSimpleName()));
        writer.write(") {");
        writer.newLine();
        writer.newLine();
        if (nearestViewSuperclass != null) {
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
            TranslatorFactory.create(processingEnv, usefulTypeMirrors, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()).writeRightSideOfEquals(writer, processingEnv, asMemberName(classElement.getSimpleName()), propertyInformationEntry.getKey(), propertyInformationEntry.getValue().getType(), NameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()));
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
        if (nearestViewSuperclass != null) {
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
            TranslatorFactory.create(processingEnv, usefulTypeMirrors, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()).writeInsideOfSet(writer, processingEnv, propertyInformationEntry.getValue().getType(), NameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()), propertyInformationEntry.getKey());
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
            writeGettersAndSetters(writer, classElement, purpose, direction, propertyInformationEntry, getterList, hasPolymorphicSubclasses, hasHierarchySubclasses);
          }
        }

        // native getters and setters
        if (propertyLexicon.isReal()) {
          writer.newLine();
          writer.write("  // native getters and setters");
          for (Map.Entry<String, PropertyInformation> propertyInformationEntry : propertyLexicon.getRealMap().entrySet()) {
            writer.newLine();
            writeGettersAndSetters(writer, classElement, purpose, direction, propertyInformationEntry, getterList, hasPolymorphicSubclasses, hasHierarchySubclasses);
          }
        }

        // hashCode and equals
        writeHashCodeAndEquals(writer, NameUtility.getSimpleName(processingEnv, purpose, direction, classElement), getterList, nearestViewSuperclass != null);

        writer.write("}");
        writer.newLine();
      }
    }
  }

  /**
   * Writes the default construction path inside the generated {@code instance(...)} method.
   *
   * @param writer       destination for source output
   * @param classElement the originating entity class
   * @param purpose      current purpose
   * @param direction    current direction
   * @throws IOException if writing fails
   */
  private void writeSelfConstruction (BufferedWriter writer, TypeElement classElement, String purpose, Direction direction)
    throws IOException {

    writer.write("return new ");
    writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
    writer.write("(");
    writer.write(asMemberName(classElement.getSimpleName()));
    writer.write(");");
    writer.newLine();
  }

  /**
   * Emits a field declaration for a generated view, including constraint annotations.
   *
   * @param writer                   destination for source output
   * @param purpose                  current purpose
   * @param direction                current direction
   * @param propertyInformationEntry property name and metadata to emit
   * @throws IOException if writing fails
   */
  private void writeField (BufferedWriter writer, String purpose, Direction direction, Map.Entry<String, PropertyInformation> propertyInformationEntry)
    throws IOException {

    writeConstraints(writer, propertyInformationEntry.getValue().constraints(), 2);
    writer.write("  private ");
    writer.write(NameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()));
    writer.write(" ");
    writer.write(propertyInformationEntry.getKey());
    writer.write(";");
    writer.newLine();
  }

  /**
   * Writes class-level constraint annotations that apply to the current purpose/direction pair.
   *
   * @param writer             destination for source output
   * @param purpose            current purpose
   * @param direction          current direction
   * @param constrainingIdioms iterable of idioms that may carry constraints
   * @throws IOException if writing fails
   */
  private void writeConstrainingIdioms (BufferedWriter writer, String purpose, Direction direction, Iterable<IdiomInformation> constrainingIdioms)
    throws IOException {

    for (IdiomInformation idiom : constrainingIdioms) {
      if ((!idiom.getConstraintList().isEmpty()) && hasPurpose(purpose, idiom.getPurposeList()) && idiom.getVisibility().matches(direction)) {
        writeConstraints(writer, idiom.getConstraintList(), 0);
      }
    }
  }

  /**
   * Emits a list of constraint annotations with indentation.
   *
   * @param writer      destination for source output
   * @param constraints constraints to render
   * @param indent      number of spaces to indent before each annotation
   * @throws IOException if writing fails
   */
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

  /**
   * Generates getter and fluent setter pairs for a property.
   *
   * @param writer                   destination for source output
   * @param classElement             the originating entity class
   * @param purpose                  current purpose
   * @param direction                current direction
   * @param propertyInformationEntry property name and metadata
   * @param getterList               collection of getter names to feed equality/hashCode generation
   * @param hasPolymorphicSubclasses whether the view supports polymorphic subclasses
   * @param hasHierarchySubclasses   whether the view supports hierarchy subclasses
   * @throws IOException if writing fails
   */
  private void writeGettersAndSetters (BufferedWriter writer, TypeElement classElement, String purpose, Direction direction, Map.Entry<String, PropertyInformation> propertyInformationEntry, LinkedList<String> getterList, boolean hasPolymorphicSubclasses, boolean hasHierarchySubclasses)
    throws IOException {

    String getter;

    if (!propertyInformationEntry.getValue().getComment().isEmpty()) {
      writer.write("  @Comment(\"");
      writer.write(propertyInformationEntry.getValue().getComment());
      writer.write("\")");
      writer.newLine();
    }
    if (propertyInformationEntry.getValue().getAs() != null) {
      writer.write("  @As(");
      writer.write(propertyInformationEntry.getValue().getAs().toString());
      writer.write(".class)");
      writer.newLine();
    }
    if (propertyInformationEntry.getValue().getNullifierMessage() != null) {
      writer.write("  @NullifiedBy(\"");
      writer.write(propertyInformationEntry.getValue().getNullifierMessage());
      writer.write("\")");
      writer.newLine();
    }
    if (propertyInformationEntry.getValue().getAdapter() != null) {
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
    writer.write(NameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()));
    writer.write(" ");
    writer.write(getter = TypeKind.BOOLEAN.equals(propertyInformationEntry.getValue().getType().getKind()) ? BeanUtility.asIsName(propertyInformationEntry.getKey()) : BeanUtility.asGetterName(propertyInformationEntry.getKey()));
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

    getterList.add(getter);

    writer.write("  public ");
    if (hasPolymorphicSubclasses || hasHierarchySubclasses) {
      writer.write("D");
    } else {
      writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
    }
    writer.write(" ");
    writer.write(BeanUtility.asSetterName(propertyInformationEntry.getKey()));
    writer.write(" (");
    writer.write(NameUtility.processTypeMirror(processingEnv, visibilityTracker, classTracker, purpose, direction, propertyInformationEntry.getValue().getType()));
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
    if (hasPolymorphicSubclasses || hasHierarchySubclasses) {
      writer.write("(D)");
    }
    writer.write("this;");
    writer.newLine();
    writer.write("  }");
    writer.newLine();
  }

  /**
   * Emits {@code hashCode} and {@code equals} implementations that consider the generated getters and optional superclass.
   *
   * @param writer         destination for source output
   * @param simpleClassName simple name of the view class
   * @param getterList     ordered getters to include in equality logic
   * @param hasSuperClass  whether the view extends another generated view
   * @throws IOException if writing fails
   */
  private void writeHashCodeAndEquals (BufferedWriter writer, String simpleClassName, LinkedList<String> getterList, boolean hasSuperClass)
    throws IOException {

    boolean first = true;

    writer.newLine();
    writer.write("  @Override");
    writer.newLine();
    writer.write("  public int hashCode () {");
    writer.newLine();
    writer.newLine();

    if (getterList.isEmpty()) {
      if (hasSuperClass) {
        writer.write("    return super.hashCode();");
        writer.newLine();
      } else {
        writer.write("    return 0;");
        writer.newLine();
      }
    } else {
      writer.write("    int h;");
      writer.newLine();
      writer.newLine();

      for (String getter : getterList) {
        if (first) {
          writer.write("    h = ");
          first = false;
        } else {
          writer.write("    h = (31 * h) + ");
        }

        writer.write("Objects.hashCode(");
        writer.write(getter);
        writer.write("());");
        writer.newLine();
      }

      if (hasSuperClass) {
        writer.newLine();
        writer.write("    h = (31 * h) + super.hashCode();");
        writer.newLine();
      }

      writer.newLine();
      writer.write("    return h;");
      writer.newLine();
    }

    writer.write("  }");
    writer.newLine();

    writer.newLine();
    writer.write("  @Override");
    writer.newLine();
    writer.write("  public boolean equals (Object obj) {");
    writer.newLine();
    writer.newLine();
    writer.write("    if (this == obj) {");
    writer.newLine();
    writer.write("      return true;");
    writer.newLine();
    writer.write("    } else if (!(obj instanceof ");
    writer.write(simpleClassName);
    writer.write(")) {");
    writer.newLine();
    writer.write("      return false;");
    writer.newLine();
    writer.write("    } else {");
    writer.newLine();

    for (String getter : getterList) {
      writer.write("      if (!Objects.equals(this.");
      writer.write(getter);
      writer.write("(), ((");
      writer.write(simpleClassName);
      writer.write(")obj).");
      writer.write(getter);
      writer.write("())) {");
      writer.newLine();
      writer.write("        return false;");
      writer.newLine();
      writer.write("      }");
      writer.newLine();
    }

    writer.newLine();
    if (hasSuperClass) {
      writer.write("      return super.equals(obj);");
    } else {
      writer.write("      return true;");
    }
    writer.newLine();
    writer.write("    }");
    writer.newLine();
    writer.write("  }");
    writer.newLine();
  }

  /**
   * Generates an XML adapter class to handle polymorphic serialization for the specified view.
   *
   * @param classElement            the polymorphic base class
   * @param purpose                 current purpose
   * @param direction               current direction
   * @param usePolymorphicAttribute whether the adapter should encode type information as an attribute
   * @throws IOException if source generation fails
   */
  private void writePolymorphicAdapter (TypeElement classElement, String purpose, Direction direction, boolean usePolymorphicAttribute)
    throws IOException {

    JavaFileObject sourceFile;
    StringBuilder sourceFileBuilder = new StringBuilder(processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName()).append('.').append(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));

    if (usePolymorphicAttribute) {
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
        writer.write("import jakarta.annotation.Generated;");
        writer.newLine();
        writer.write("import org.smallmind.web.json.scaffold.util.");
        if (usePolymorphicAttribute) {
          writer.write("Attributed");
        }
        writer.write("PolymorphicXmlAdapter;");
        writer.newLine();
        writer.newLine();

        // @Generated
        writer.write("@Generated(\"");
        writer.write(DoppelgangerAnnotationProcessor.class.getName());
        writer.write("\")");
        writer.newLine();

        // class declaration
        writer.write("public class ");
        writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        if (usePolymorphicAttribute) {
          writer.write("Attributed");
        }
        writer.write("PolymorphicXmlAdapter");
        writer.write(" extends ");
        if (usePolymorphicAttribute) {
          writer.write("Attributed");
        }
        writer.write("PolymorphicXmlAdapter<");
        writer.write(NameUtility.getSimpleName(processingEnv, purpose, direction, classElement));
        writer.write("> {");
        writer.newLine();
        writer.newLine();
        writer.write("}");
        writer.newLine();
      }
    }
  }
}
