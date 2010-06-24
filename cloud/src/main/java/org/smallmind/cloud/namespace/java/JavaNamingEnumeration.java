package org.smallmind.cloud.namespace.java;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.smallmind.cloud.namespace.java.backingStore.NameTranslator;

public class JavaNamingEnumeration<T> implements NamingEnumeration<T> {

   private static final Class[] SEARCH_RESULT_SIGNATURE = new Class[] {String.class, String.class, Object.class, Attributes.class, Boolean.class};
   private static final Class[] BINDING_SIGNATURE = new Class[] {String.class, String.class, Object.class, Boolean.class};
   private static final Class[] NAME_CLASS_PAIR_SIGNATURE = new Class[] {String.class, String.class, Boolean.class};

   private NamingEnumeration internalEnumeration;
   private Class<T> typeClass;
   private Class internalDirContextClass;
   private Hashtable<String, Object> environment;
   private NameTranslator nameTranslator;
   private JavaNameParser nameParser;
   private boolean modifiable;

   public JavaNamingEnumeration (Class<T> typeClass, NamingEnumeration<T> internalEnumeration, Class internalDirContextClass, Hashtable<String, Object> environment, NameTranslator nameTranslator, JavaNameParser nameParser, boolean modifiable) {

      this.typeClass = typeClass;
      this.internalEnumeration = internalEnumeration;
      this.internalDirContextClass = internalDirContextClass;
      this.environment = environment;
      this.nameTranslator = nameTranslator;
      this.nameParser = nameParser;
      this.modifiable = modifiable;
   }

   public boolean hasMoreElements () {

      try {
         return hasMore();
      }
      catch (NamingException n) {
         return false;
      }
   }

   public T nextElement () {

      try {
         return next();
      }
      catch (NamingException n) {
         throw new NoSuchElementException("There are no more items within this Enumeration");
      }
   }

   public T next ()
      throws NamingException {

      NamingException namingException;
      Constructor<T> typeConstructor;
      Object enumObject;
      Object boundObject;
      Attributes resultAttributes;
      String name;
      String className;
      boolean isRelative;

      enumObject = internalEnumeration.next();
      isRelative = ((NameClassPair)enumObject).isRelative();
      name = NamingEnumerationUtilities.convertName(((NameClassPair)enumObject).getName(), nameTranslator);
      className = NamingEnumerationUtilities.convertClassName(((NameClassPair)enumObject).getClassName(), internalDirContextClass);

      try {
         if (enumObject instanceof SearchResult) {
            boundObject = NamingEnumerationUtilities.convertObject(((SearchResult)enumObject).getObject(), internalDirContextClass, environment, nameTranslator, nameParser, modifiable);
            resultAttributes = ((SearchResult)enumObject).getAttributes();

            typeConstructor = typeClass.getConstructor(SEARCH_RESULT_SIGNATURE);

            return typeConstructor.newInstance(name, className, boundObject, resultAttributes, isRelative);
         }
         if (enumObject instanceof Binding) {
            boundObject = NamingEnumerationUtilities.convertObject(((Binding)enumObject).getObject(), internalDirContextClass, environment, nameTranslator, nameParser, modifiable);

            typeConstructor = typeClass.getConstructor(BINDING_SIGNATURE);

            return typeConstructor.newInstance(name, className, boundObject, isRelative);
         }
         else if (enumObject instanceof NameClassPair) {
            typeConstructor = typeClass.getConstructor(NAME_CLASS_PAIR_SIGNATURE);

            return typeConstructor.newInstance(name, className, isRelative);
         }
      }
      catch (Exception e) {
         namingException = new NamingException(e.getMessage());
         namingException.setRootCause(e);
         throw namingException;
      }

      return typeClass.cast(enumObject);
   }

   public boolean hasMore ()
      throws NamingException {

      return internalEnumeration.hasMore();
   }

   public void close ()
      throws NamingException {

      internalEnumeration.close();
   }

}
