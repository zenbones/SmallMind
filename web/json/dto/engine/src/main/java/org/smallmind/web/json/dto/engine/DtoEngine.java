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

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;

public class DtoEngine {

  private final HashMap<Class<?>, Visibility> generatedMap = new HashMap<>();
  private final Path rootPath;
  private final String prefix;

  public DtoEngine (Path rootPath) {

    this(rootPath, "");
  }

  public DtoEngine (Path rootPath, String prefix) {

    this.rootPath = rootPath;
    this.prefix = prefix;
  }

  public void generate (Class<?> clazz)
    throws IOException, BeanAccessException, DataDefinitionException {

    generate(clazz, (direction, name) -> new StringBuilder(prefix).append(name).append(direction.getCode()).append("Dto").toString());
  }

  public void generate (Class<?> clazz, BiFunction<Direction, String, String> namingFunction)
    throws IOException, BeanAccessException, DataDefinitionException {

    if (clazz.getAnnotation(DtoGenerator.class) != null) {
      if (clazz.isInterface() || clazz.isAnnotation() || clazz.isAnonymousClass() || clazz.isEnum() || clazz.isLocalClass() || clazz.isMemberClass()) {
        throw new DataDefinitionException("The class(%s) must be a root implementations of type 'class'", clazz.getName());
      } else {
        walk(clazz, rootPath, namingFunction);
      }
    }
  }

  private void walk (Class<?> clazz, Path rootPath, BiFunction<Direction, String, String> namingFunction)
    throws IOException, BeanAccessException, DataDefinitionException {

    if (!generatedMap.containsKey(clazz)) {

      DtoGenerator dtoGenerator;
      Class<?> parentClass;

      if ((parentClass = clazz.getSuperclass()) != null) {
        walk(parentClass, rootPath, namingFunction);
      }

      if ((dtoGenerator = clazz.getAnnotation(DtoGenerator.class)) != null) {

        HashMap<String, DataField> inMap = new HashMap<>();
        HashMap<String, DataField> outMap = new HashMap<>();
        Path generatedPath = rootPath;
        Package clazzPackage;
        boolean written = false;

        for (Field field : clazz.getDeclaredFields()) {

          DtoProperty dtoProperty;

          if ((dtoProperty = field.getAnnotation(DtoProperty.class)) != null) {
            switch (dtoProperty.visibility()) {
              case IN:
                hasSetter(clazz, field);
                walk(field.getType(), rootPath, namingFunction);
                inMap.put(field.getName(), new DataField(field.getType(), dtoProperty));
                break;
              case OUT:
                hasGetter(clazz, field);
                walk(field.getType(), rootPath, namingFunction);
                outMap.put(field.getName(), new DataField(field.getType(), dtoProperty));
                break;
              case BOTH:
                hasSetter(clazz, field);
                hasGetter(clazz, field);
                walk(field.getType(), rootPath, namingFunction);
                inMap.put(field.getName(), new DataField(field.getType(), dtoProperty));
                outMap.put(field.getName(), new DataField(field.getType(), dtoProperty));
                break;
              default:
                throw new UnknownSwitchCaseException(dtoProperty.visibility().name());
            }
          }
        }

        for (Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isPublic(method.getModifiers())) {

            DtoProperty dtoProperty;

            if ((dtoProperty = method.getAnnotation(DtoProperty.class)) != null) {
              if ((method.getName().length() > 3) && method.getName().startsWith("get") && (method.getParameterCount() == 0) && Character.isUpperCase(method.getName().charAt(3))) {
                if (Visibility.IN.equals(dtoProperty.visibility())) {
                  throw new DataDefinitionException("The 'getter' method(%s) found in class(%s) can't be annotated as 'IN' only", method.getName(), clazz.getName());
                } else {

                  String fieldName = Character.toUpperCase(method.getName().charAt(3)) + method.getName().substring(4);

                  if (Visibility.BOTH.equals(dtoProperty.visibility())) {
                    if (!hasSetter(clazz, fieldName, method.getReturnType())) {
                      throw new DataDefinitionException("Missing 'setter' method(%s) in class(%s)", BeanUtility.asSetterName(fieldName), clazz.getName());
                    } else {
                      inMap.put(fieldName, new DataField(method.getReturnType(), dtoProperty));
                    }
                  }

                  walk(method.getReturnType(), rootPath, namingFunction);
                  outMap.put(fieldName, new DataField(method.getReturnType(), dtoProperty));
                }
              } else if ((method.getName().length() > 2) && method.getName().startsWith("is") && (method.getParameterCount() == 0) && Character.isUpperCase(method.getName().charAt(2)) && boolean.class.equals(method.getReturnType())) {
                if (Visibility.IN.equals(dtoProperty.visibility())) {
                  throw new DataDefinitionException("The 'getter' method(%s) found in class(%s) can't be annotated as 'IN' only", method.getName(), clazz.getName());
                } else {

                  String fieldName = Character.toUpperCase(method.getName().charAt(2)) + method.getName().substring(3);

                  if (Visibility.BOTH.equals(dtoProperty.visibility())) {
                    if (!hasSetter(clazz, fieldName, method.getReturnType())) {
                      throw new DataDefinitionException("Missing 'setter' method(%s) in class(%s)", BeanUtility.asSetterName(fieldName), clazz.getName());
                    } else {
                      inMap.put(fieldName, new DataField(method.getReturnType(), dtoProperty));
                    }
                  }

                  walk(method.getReturnType(), rootPath, namingFunction);
                  outMap.put(fieldName, new DataField(method.getReturnType(), dtoProperty));
                }
              } else if ((method.getName().length() > 3) && method.getName().startsWith("set") && (method.getParameterCount() == 1) && Character.isUpperCase(method.getName().charAt(3))) {
                if (!Visibility.IN.equals(dtoProperty.visibility())) {
                  throw new DataDefinitionException("The 'setter' method(%s) found in class(%s) must be annotated as 'IN' only", method.getName(), clazz.getName());
                } else {
                  walk(method.getReturnType(), rootPath, namingFunction);
                  inMap.put(Character.toUpperCase(method.getName().charAt(3)) + method.getName().substring(4), new DataField(method.getParameterTypes()[0], dtoProperty));
                }
              }
            }
          }
        }

        if ((clazzPackage = clazz.getPackage()) != null) {

          String packageName;

          if (((packageName = clazzPackage.getName()) != null) && (!packageName.isEmpty())) {
            for (String packagePart : packageName.split("\\.", -1)) {
              generatedPath = generatedPath.resolve(packagePart);
            }
          }
        }

        Files.createDirectories(generatedPath);

        if (!inMap.isEmpty()) {
          write(clazz, generatedPath, namingFunction, dtoGenerator, Direction.IN, inMap);

          generatedMap.put(clazz, Visibility.IN);
          written = true;
        }
        if (!outMap.isEmpty()) {
          write(clazz, generatedPath, namingFunction, dtoGenerator, Direction.OUT, outMap);

          if (Visibility.IN.equals(generatedMap.get(clazz))) {
            generatedMap.put(clazz, Visibility.BOTH);
          } else {
            generatedMap.put(clazz, Visibility.OUT);
          }
          written = true;
        }

        if (!written) {
          throw new DataDefinitionException("The class(%s) was annotated as '%s' but contained no properties", clazz.getName(), DtoGenerator.class.getSimpleName());
        }
      }
    }
  }

  private void write (Class<?> clazz, Path generatedPath, BiFunction<Direction, String, String> namingFunction, DtoGenerator dtoGenerator, Direction direction, HashMap<String, DataField> fieldMap)
    throws IOException {

    String name;

    try (BufferedWriter writer = Files.newBufferedWriter(generatedPath.resolve((name = namingFunction.apply(direction, clazz.getSimpleName())) + ".java"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

      Class<?> nearestAncestor = getNearestAncestor(clazz, direction);
      boolean firstPair = true;

      // package
      writer.write("package ");
      writer.write(clazz.getPackage().getName());
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

      if (nearestAncestor != null) {
        if (Objects.equals(clazz.getPackage(), nearestAncestor.getPackage())) {
          writer.write("import ");
          writer.write(namingFunction.apply(direction, nearestAncestor.getSimpleName()));
          writer.write(";");
          writer.newLine();
        }
      }
      writer.newLine();

      // @Generated
      writer.write("@Generated(\"");
      writer.write(DtoEngine.class.getName());
      writer.write("\")");
      writer.newLine();

      // @XmlRootElement
      writer.write("@XmlRootElement(name = \"");
      writer.write(dtoGenerator.name().isEmpty() ? Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1) : dtoGenerator.name());
      writer.write("\")");
      writer.newLine();

      // XmlAccessorType
      writer.write("@XmlAccessorType(XmlAccessType.PROPERTY)");
      writer.newLine();

      // class declaration
      writer.write("public ");
      if (Modifier.isAbstract(clazz.getModifiers())) {
        writer.write("abstract ");
      }
      writer.write("class ");
      writer.write(name);
      if (nearestAncestor != null) {
        writer.write(" extends ");
        writer.write(namingFunction.apply(direction, nearestAncestor.getSimpleName()));
      }
      writer.write(" {");
      writer.newLine();
      writer.newLine();

      // field declarations
      for (Map.Entry<String, DataField> fieldEntry : fieldMap.entrySet()) {
        writer.write("  private ");
        writer.write(getCompatibleClassName(fieldEntry.getValue().getType(), namingFunction, direction));
        writer.write(" ");
        writer.write(fieldEntry.getKey());
        writer.write(";");
        writer.newLine();
      }
      writer.newLine();

      // constructors
      writer.write("  public ");
      writer.write(name);
      writer.write(" () {");
      writer.newLine();
      writer.write("  }");
      writer.newLine();
      writer.newLine();

      writer.write("  public ");
      writer.write(name);
      writer.write(" (");
      writer.write(clazz.getName());
      writer.write(" ");
      writer.write(Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1));
      writer.write(") {");
      writer.newLine();
      writer.newLine();
      for (Map.Entry<String, DataField> fieldEntry : fieldMap.entrySet()) {
        writer.write("    this.");
        writer.write(fieldEntry.getKey());
        writer.write(" = ");
        writer.write(Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1));
        writer.write(".");
        writer.write(boolean.class.equals(fieldEntry.getValue()) ? BeanUtility.asIsName(fieldEntry.getKey()) : BeanUtility.asGetterName(fieldEntry.getKey()));
        writer.write("();");
        writer.newLine();
      }
      writer.write("  }");
      writer.newLine();
      writer.newLine();

      // entity factory
      writer.write("  public ");
      writer.write(" ");
      writer.write(clazz.getName());
      writer.write(" construct() {");
      writer.newLine();
      writer.newLine();
      writer.write("    ");
      writer.write(clazz.getName());
      writer.write("  ");
      writer.write(Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1));
      writer.write(" = new ");
      writer.write(clazz.getSimpleName());
      writer.write("();");
      writer.newLine();
      writer.newLine();
      for (Map.Entry<String, DataField> fieldEntry : fieldMap.entrySet()) {
        writer.write("    ");
        writer.write(Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1));
        writer.write(".");
        writer.write(BeanUtility.asSetterName(fieldEntry.getKey()));
        writer.write("(");
        writer.write(fieldEntry.getKey());
        writer.write(");");
        writer.newLine();
      }
      writer.newLine();
      writer.write("    return ");
      writer.write(Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1));
      writer.write(";");
      writer.newLine();
      writer.write("  }");
      writer.newLine();
      writer.newLine();

      // getters and setters
      for (Map.Entry<String, DataField> fieldEntry : fieldMap.entrySet()) {

        if (!firstPair) {
          writer.newLine();
        }

        writer.write("  @XmlElement(name = \"");
        writer.write(fieldEntry.getValue().getDtoProperty().name().isEmpty() ? fieldEntry.getKey() : dtoGenerator.name());
        writer.write(fieldEntry.getValue().getDtoProperty().required() ? "\", required = true)" : "\")");
        writer.newLine();
        writer.write("  public ");
        writer.write(getCompatibleClassName(fieldEntry.getValue().getType(), namingFunction, direction));
        writer.write(" ");
        writer.write(boolean.class.equals(fieldEntry.getValue()) ? BeanUtility.asIsName(fieldEntry.getKey()) : BeanUtility.asGetterName(fieldEntry.getKey()));
        writer.write("() {");
        writer.newLine();
        writer.newLine();
        writer.write("    return ");
        writer.write(fieldEntry.getKey());
        writer.write(";");
        writer.newLine();
        writer.write("  }");
        writer.newLine();
        writer.newLine();

        writer.write("  public void ");
        writer.write(BeanUtility.asSetterName(fieldEntry.getKey()));
        writer.write("(");
        writer.write(getCompatibleClassName(fieldEntry.getValue().getType(), namingFunction, direction));
        writer.write(" ");
        writer.write(fieldEntry.getKey());
        writer.write(") {");
        writer.newLine();
        writer.newLine();
        writer.write("    this.");
        writer.write(fieldEntry.getKey());
        writer.write(" = ");
        writer.write(fieldEntry.getKey());
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

  private String getCompatibleClassName (Class<?> clazz, BiFunction<Direction, String, String> namingFunction, Direction direction) {

    Visibility visibility;

    if (((visibility = generatedMap.get(clazz)) != null) && visibility.matches(direction)) {
      return clazz.getPackage().getName() + '.' + namingFunction.apply(direction, clazz.getSimpleName());
    }

    return clazz.getName();
  }

  private Class<?> getNearestAncestor (Class<?> clazz, Direction direction) {

    Class<?> currentClass = clazz;

    while ((currentClass = currentClass.getSuperclass()) != null) {

      Visibility visibility;

      if (((visibility = generatedMap.get(currentClass)) != null) && visibility.matches(direction)) {

        return currentClass;
      }
    }

    return null;
  }

  private void hasGetter (Class<?> clazz, Field field)
    throws BeanAccessException {

    try {

      Method getterMethod;

      getterMethod = clazz.getDeclaredMethod(BeanUtility.asGetterName(field.getName()));
      if (!Modifier.isPublic(getterMethod.getModifiers()) || (!getterMethod.getReturnType().isAssignableFrom(field.getType()))) {
        if (!hasIs(clazz, field)) {
          throw new BeanAccessException("No public 'getter' method(%s or %s) found in class(%s)", BeanUtility.asGetterName(field.getName()), BeanUtility.asIsName(field.getName()), clazz.getName());
        }
      }
    } catch (NoSuchMethodException noSuchMethodException) {
      if (!hasIs(clazz, field)) {
        throw new BeanAccessException("No 'getter' method(%s or %s) found in class(%s)", BeanUtility.asGetterName(field.getName()), BeanUtility.asIsName(field.getName()), clazz.getName());
      }
    }
  }

  private boolean hasIs (Class<?> clazz, Field field)
    throws BeanAccessException {

    if (boolean.class.equals(field.getType())) {
      try {

        Method isMethod;

        isMethod = clazz.getDeclaredMethod(BeanUtility.asIsName(field.getName()));
        if (Modifier.isPublic(isMethod.getModifiers())) {
          if (!boolean.class.equals(isMethod.getReturnType())) {
            throw new BeanAccessException("Found an 'is' method(%s) in class(%s), but it doesn't return a 'boolean' type", isMethod.getName(), clazz.getName());
          } else {

            return true;
          }
        }
      } catch (NoSuchMethodException otherNoSuchMethodException) {
        // do nothing
      }
    }

    return false;
  }

  private void hasSetter (Class<?> clazz, Field field)
    throws BeanAccessException {

    try {

      Method setterMethod;

      setterMethod = clazz.getDeclaredMethod(BeanUtility.asSetterName(field.getName()), field.getType());
      if (!Modifier.isPublic(setterMethod.getModifiers())) {
        throw new BeanAccessException("No public 'setter' method(%s) found in class(%s)", BeanUtility.asSetterName(field.getName()), clazz.getName());
      }
    } catch (NoSuchMethodException otherNoSuchMethodException) {
      throw new BeanAccessException("No 'setter' method(%s) found in class(%s)", BeanUtility.asSetterName(field.getName()), clazz.getName());
    }
  }

  private boolean hasSetter (Class<?> clazz, String fieldName, Class<?> fieldType) {

    try {

      Method setterMethod;

      setterMethod = clazz.getDeclaredMethod(BeanUtility.asSetterName(fieldName), fieldType);
      if (Modifier.isPublic(setterMethod.getModifiers())) {

        return true;
      }
    } catch (NoSuchMethodException otherNoSuchMethodException) {
      // do nothing
    }

    return false;
  }

  private class DataField {

    private DtoProperty dtoProperty;
    private Class<?> clazz;

    public DataField (Class<?> clazz, DtoProperty dtoProperty) {

      this.clazz = clazz;
      this.dtoProperty = dtoProperty;
    }

    public Class<?> getType () {

      return clazz;
    }

    public DtoProperty getDtoProperty () {

      return dtoProperty;
    }
  }
}
