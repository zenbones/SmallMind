package org.smallmind.web.jersey.data;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.BiFunction;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;

public class DataGenerator {

  private HashMap<Class<?>, Visibility> generatedMap = new HashMap<>();

  public void generate (Class<?> clazz, Path rootPath, String prefix)
    throws IOException, BeanAccessException, DataDefinitionException {

    generate(clazz, rootPath, (direction, name) -> new StringBuilder(prefix).append(clazz.getSimpleName()).append(direction.getCode()).append("Dto").toString());
  }

  public void generate (Class<?> clazz, Path rootPath, BiFunction<Direction, String, String> namingFunction)
    throws IOException, BeanAccessException, DataDefinitionException {

    if (clazz.getAnnotation(Data.class) != null) {
      if (clazz.isInterface() || clazz.isAnnotation() || clazz.isAnonymousClass() || clazz.isEnum() || clazz.isLocalClass() || clazz.isMemberClass()) {
        throw new DataDefinitionException("The class(%s) must be a root implementations of type 'class'", clazz.getName());
      } else {
        foob(clazz, rootPath, namingFunction);
      }
    }
  }

  private void foob (Class<?> clazz, Path rootPath, BiFunction<Direction, String, String> namingFunction)
    throws IOException, BeanAccessException, DataDefinitionException {

    if (!generatedMap.containsKey(clazz)) {

      Data data;
      Class<?> parentClass;

      if ((parentClass = clazz.getSuperclass()) != null) {
        foob(parentClass, rootPath, namingFunction);
      }

      if ((data = clazz.getAnnotation(Data.class)) != null) {

        HashMap<String, Class<?>> inMap = new HashMap<>();
        HashMap<String, Class<?>> outMap = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {

          Property property;

          if ((property = field.getAnnotation(Property.class)) != null) {
            switch (property.value()) {
              case IN:
                hasSetter(clazz, field);
                inMap.put(field.getName(), field.getType());
                break;
              case OUT:
                hasGetter(clazz, field);
                outMap.put(field.getName(), field.getType());
                break;
              case BOTH:
                hasSetter(clazz, field);
                hasGetter(clazz, field);
                inMap.put(field.getName(), field.getType());
                outMap.put(field.getName(), field.getType());
                break;
              default:
                throw new UnknownSwitchCaseException(property.value().name());
            }
          }
        }

        for (Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isPublic(method.getModifiers())) {

            Property property;

            if ((property = method.getAnnotation(Property.class)) != null) {
              if ((method.getName().length() > 3) && method.getName().startsWith("get") && (method.getParameterCount() == 0) && Character.isUpperCase(method.getName().charAt(3))) {
                if (Visibility.IN.equals(property.value())) {
                  throw new DataDefinitionException("The 'getter' method(%s) found in class(%s) can't be annotated as 'IN' only", method.getName(), clazz.getName());
                } else {

                  String fieldName = Character.toUpperCase(method.getName().charAt(3)) + method.getName().substring(4);

                  if (Visibility.BOTH.equals(property.value())) {
                    if (!hasSetter(clazz, fieldName, method.getReturnType())) {
                      throw new DataDefinitionException("Missing 'setter' method(%s) in class(%s)", BeanUtility.asSetterName(fieldName), clazz.getName());
                    } else {
                      inMap.put(fieldName, method.getReturnType());
                    }
                  }

                  outMap.put(fieldName, method.getReturnType());
                }
              } else if ((method.getName().length() > 2) && method.getName().startsWith("is") && (method.getParameterCount() == 0) && Character.isUpperCase(method.getName().charAt(2)) && boolean.class.equals(method.getReturnType())) {
                if (Visibility.IN.equals(property.value())) {
                  throw new DataDefinitionException("The 'getter' method(%s) found in class(%s) can't be annotated as 'IN' only", method.getName(), clazz.getName());
                } else {

                  String fieldName = Character.toUpperCase(method.getName().charAt(2)) + method.getName().substring(3);

                  if (Visibility.BOTH.equals(property.value())) {
                    if (!hasSetter(clazz, fieldName, method.getReturnType())) {
                      throw new DataDefinitionException("Missing 'setter' method(%s) in class(%s)", BeanUtility.asSetterName(fieldName), clazz.getName());
                    } else {
                      inMap.put(fieldName, method.getReturnType());
                    }
                  }

                  outMap.put(fieldName, method.getReturnType());
                }
              } else if ((method.getName().length() > 3) && method.getName().startsWith("set") && (method.getParameterCount() == 1) && Character.isUpperCase(method.getName().charAt(3))) {
                if (!Visibility.IN.equals(property.value())) {
                  throw new DataDefinitionException("The 'setter' method(%s) found in class(%s) must be annotated as 'IN' only", method.getName(), clazz.getName());
                } else {
                  inMap.put(Character.toUpperCase(method.getName().charAt(3)) + method.getName().substring(4), method.getParameterTypes()[0]);
                }
              }
            }
          }
        }

        Path generationPath = rootPath;
        Package clazzPackage;

        if ((clazzPackage = clazz.getPackage()) != null) {

          String packageName;

          if (((packageName = clazzPackage.getName()) != null) && (!packageName.isEmpty())) {
            for (String packagePart : packageName.split("\\.", -1)) {
              generationPath.resolve(packagePart);
            }
          }
        }

        if (!inMap.isEmpty()) {

          Class<?> nearestAncestor = getNearestAncestor(parentClass, Direction.IN);
          OutputStream outputStream = Files.newOutputStream(generationPath.resolve(namingFunction.apply(Direction.IN, clazz.getSimpleName())));

          generatedMap.put(clazz, Visibility.IN);
        }
        if (!outMap.isEmpty()) {

          Class<?> nearestAncestor = getNearestAncestor(parentClass, Direction.OUT);
          OutputStream outputStream = Files.newOutputStream(generationPath.resolve(namingFunction.apply(Direction.OUT, clazz.getSimpleName())));

          if (Visibility.IN.equals(generatedMap.get(clazz))) {
            generatedMap.put(clazz, Visibility.BOTH);
          } else {
            generatedMap.put(clazz, Visibility.OUT);
          }
        }
      }
    }
  }

  private Class<?> getNearestAncestor (Class<?> parentClass, Direction direction) {

    Class<?> currentClass = parentClass;

    while (currentClass != null) {

      Visibility visibility;

      if (((visibility = generatedMap.get(parentClass)) != null) && visibility.matches(direction)) {

        return currentClass;
      }

      currentClass = parentClass.getSuperclass();
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
