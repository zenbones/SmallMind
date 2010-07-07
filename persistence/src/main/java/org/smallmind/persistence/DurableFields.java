package org.smallmind.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class DurableFields {

   private static final ConcurrentHashMap<Class<? extends Durable>, Field[]> FIELD_MAP = new ConcurrentHashMap<Class<? extends Durable>, Field[]>();

   public static Field[] getFields(Class<? extends Durable> durableClass) {

      Field[] fields;

      if ((fields = FIELD_MAP.get(durableClass)) == null) {

         Class<?> currentClass = durableClass;
         LinkedList<Field> fieldList = new LinkedList<Field>();

         do {
            for (Field field : currentClass.getDeclaredFields()) {
               if (!Modifier.isStatic(field.getModifiers())) {
                  field.setAccessible(true);
                  fieldList.add(field);
               }
            }

         } while ((currentClass = currentClass.getSuperclass()) != null);

         Collections.sort(fieldList, new Comparator<Field>() {

            public int compare(Field field1, Field field2) {

               return field1.getName().compareToIgnoreCase(field2.getName());
            }
         });

         fields = new Field[fieldList.size()];
         fieldList.toArray(fields);

         FIELD_MAP.put(durableClass, fields);
      }

      return fields;
   }
}
