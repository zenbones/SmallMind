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
package org.smallmind.web.jersey.data;

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

public class DataGenerator {

  private HashMap<Class<?>, Visibility> generatedMap = new HashMap<>();

  public void generate (Class<?> clazz, Path rootPath, String prefix)
    throws IOException, BeanAccessException, DataDefinitionException {

    generate(clazz, rootPath, (direction, name) -> new StringBuilder(prefix).append(name).append(direction.getCode()).append("Dto").toString());
  }

  public void generate (Class<?> clazz, Path rootPath, BiFunction<Direction, String, String> namingFunction)
    throws IOException, BeanAccessException, DataDefinitionException {

    if (clazz.getAnnotation(Data.class) != null) {
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

      Data data;
      Class<?> parentClass;

      if ((parentClass = clazz.getSuperclass()) != null) {
        walk(parentClass, rootPath, namingFunction);
      }

      if ((data = clazz.getAnnotation(Data.class)) != null) {

        HashMap<String, Class<?>> inMap = new HashMap<>();
        HashMap<String, Class<?>> outMap = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {

          Property property;

          if ((property = field.getAnnotation(Property.class)) != null) {
            switch (property.visibility()) {
              case IN:
                hasSetter(clazz, field);
                walk(field.getType(), rootPath, namingFunction);
                inMap.put(field.getName(), field.getType());
                break;
              case OUT:
                hasGetter(clazz, field);
                walk(field.getType(), rootPath, namingFunction);
                outMap.put(field.getName(), field.getType());
                break;
              case BOTH:
                hasSetter(clazz, field);
                hasGetter(clazz, field);
                walk(field.getType(), rootPath, namingFunction);
                inMap.put(field.getName(), field.getType());
                outMap.put(field.getName(), field.getType());
                break;
              default:
                throw new UnknownSwitchCaseException(property.visibility().name());
            }
          }
        }

        for (Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isPublic(method.getModifiers())) {

            Property property;

            if ((property = method.getAnnotation(Property.class)) != null) {
              if ((method.getName().length() > 3) && method.getName().startsWith("get") && (method.getParameterCount() == 0) && Character.isUpperCase(method.getName().charAt(3))) {
                if (Visibility.IN.equals(property.visibility())) {
                  throw new DataDefinitionException("The 'getter' method(%s) found in class(%s) can't be annotated as 'IN' only", method.getName(), clazz.getName());
                } else {

                  String fieldName = Character.toUpperCase(method.getName().charAt(3)) + method.getName().substring(4);

                  if (Visibility.BOTH.equals(property.visibility())) {
                    if (!hasSetter(clazz, fieldName, method.getReturnType())) {
                      throw new DataDefinitionException("Missing 'setter' method(%s) in class(%s)", BeanUtility.asSetterName(fieldName), clazz.getName());
                    } else {
                      inMap.put(fieldName, method.getReturnType());
                    }
                  }

                  walk(method.getReturnType(), rootPath, namingFunction);
                  outMap.put(fieldName, method.getReturnType());
                }
              } else if ((method.getName().length() > 2) && method.getName().startsWith("is") && (method.getParameterCount() == 0) && Character.isUpperCase(method.getName().charAt(2)) && boolean.class.equals(method.getReturnType())) {
                if (Visibility.IN.equals(property.visibility())) {
                  throw new DataDefinitionException("The 'getter' method(%s) found in class(%s) can't be annotated as 'IN' only", method.getName(), clazz.getName());
                } else {

                  String fieldName = Character.toUpperCase(method.getName().charAt(2)) + method.getName().substring(3);

                  if (Visibility.BOTH.equals(property.visibility())) {
                    if (!hasSetter(clazz, fieldName, method.getReturnType())) {
                      throw new DataDefinitionException("Missing 'setter' method(%s) in class(%s)", BeanUtility.asSetterName(fieldName), clazz.getName());
                    } else {
                      inMap.put(fieldName, method.getReturnType());
                    }
                  }

                  walk(method.getReturnType(), rootPath, namingFunction);
                  outMap.put(fieldName, method.getReturnType());
                }
              } else if ((method.getName().length() > 3) && method.getName().startsWith("set") && (method.getParameterCount() == 1) && Character.isUpperCase(method.getName().charAt(3))) {
                if (!Visibility.IN.equals(property.visibility())) {
                  throw new DataDefinitionException("The 'setter' method(%s) found in class(%s) must be annotated as 'IN' only", method.getName(), clazz.getName());
                } else {
                  walk(method.getReturnType(), rootPath, namingFunction);
                  inMap.put(Character.toUpperCase(method.getName().charAt(3)) + method.getName().substring(4), method.getParameterTypes()[0]);
                }
              }
            }
          }
        }

        Path generatedPath = rootPath;
        Package clazzPackage;

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
          write(clazz, generatedPath, namingFunction, data, Direction.IN, inMap);

          generatedMap.put(clazz, Visibility.IN);
        }
        if (!outMap.isEmpty()) {
          write(clazz, generatedPath, namingFunction, data, Direction.OUT, outMap);

          if (Visibility.IN.equals(generatedMap.get(clazz))) {
            generatedMap.put(clazz, Visibility.BOTH);
          } else {
            generatedMap.put(clazz, Visibility.OUT);
          }
        }
      }
    }
  }

  private void write (Class<?> clazz, Path generatedPath, BiFunction<Direction, String, String> namingFunction, Data data, Direction direction, HashMap<String, Class<?>> fieldMap)
    throws IOException {

    String name;

    try (BufferedWriter writer = Files.newBufferedWriter(generatedPath.resolve((name = namingFunction.apply(direction, clazz.getSimpleName())) + ".java"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

      Class<?> nearestAncestor = getNearestAncestor(clazz, direction);

      // package
      writer.write("package ");
      writer.write(clazz.getPackage().getName());
      writer.write(";");
      writer.newLine();
      writer.newLine();

      // imports
      if (nearestAncestor != null) {
        if (Objects.equals(clazz.getPackage(), nearestAncestor.getPackage())) {
          writer.write("import ");
          writer.write(namingFunction.apply(direction, nearestAncestor.getSimpleName()));
          writer.write(";");
          writer.newLine();
          writer.newLine();
        }
      }

      // @Generated
      writer.write("@Generated(\"");
      writer.write(DataGenerator.class.getName());
      writer.write("\")");
      writer.newLine();

      // @XmlRootElement
      writer.write("@XmlRootElement(name = \"");
      writer.write(data.name().isEmpty() ? Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1) : data.name());
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
      for (Map.Entry<String, Class<?>> fieldEntry : fieldMap.entrySet()) {
        writer.write("  private ");
        writer.write(getCompatibleClassName(fieldEntry.getValue(), namingFunction, direction));
        writer.write(" ");
        writer.write(fieldEntry.getKey());
        writer.write(";");
        writer.newLine();
      }
      writer.newLine();

      for (Map.Entry<String, Class<?>> fieldEntry : fieldMap.entrySet()) {

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
}
