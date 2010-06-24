package org.smallmind.cloud.namespace.java;

import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.smallmind.cloud.namespace.java.backingStore.NameTranslator;

public class JavaContext implements DirContext {

   public static final String CONNECTION_DETAILS = "org.smallmind.cloud.namespace.java.connection details";
   public static final String CONTEXT_STORE = "org.smallmind.cloud.namespace.java.store";
   public static final String CONTEXT_MODIFIABLE = "org.smallmind.cloud.namespace.java.modifiable";
   public static final String POOLED_CONNECTION = "org.smallmind.cloud.namespace.java.pooled";

   private Hashtable<String, Object> environment;
   private DirContext internalContext;
   private NameTranslator nameTranslator;
   private JavaNameParser nameParser;
   private boolean modifiable;
   private boolean pooled;

   public static JavaContext insureContext (JavaContext javaContext, String namingPath)
      throws NamingException {

      JavaContext lastContext = javaContext;
      StringBuilder pathSoFar;
      String[] pathArray;

      pathArray = namingPath.split("/", -1);
      pathSoFar = new StringBuilder();
      for (int count = pathArray.length - 1; count >= 0; count--) {
         if (pathSoFar.length() > 0) {
            pathSoFar.insert(0, '/');
         }
         pathSoFar.insert(0, pathArray[count]);
         try {
            lastContext = (JavaContext)javaContext.lookup(pathSoFar.toString());
         }
         catch (NameNotFoundException n) {
            lastContext = (JavaContext)javaContext.createSubcontext(pathSoFar.toString());
         }
      }

      return lastContext;
   }

   protected JavaContext (NameTranslator nameTranslator, Hashtable<String, Object> environment, boolean modifiable, boolean pooled) {

      this.nameTranslator = nameTranslator;
      this.environment = environment;
      this.modifiable = modifiable;
      this.pooled = pooled;

      internalContext = null;
      nameParser = new JavaNameParser(nameTranslator);
   }

   protected JavaContext (Hashtable<String, Object> environment, DirContext internalContext, NameTranslator nameTranslator, JavaNameParser nameParser, boolean modifiable) {

      this.environment = environment;
      this.internalContext = internalContext;
      this.nameTranslator = nameTranslator;
      this.nameParser = nameParser;
      this.modifiable = modifiable;

      pooled = false;
   }

   public Object lookup (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;
      Object lookupObject;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      lookupObject = contextNamePair.getContext().lookup(contextNamePair.getName());
      if (lookupObject.getClass().equals(contextNamePair.getContext().getClass())) {
         if (pooled) {
            return new PooledJavaContext(environment, (DirContext)lookupObject, nameTranslator, nameParser, modifiable);
         }
         else {
            return new JavaContext(environment, (DirContext)lookupObject, nameTranslator, nameParser, modifiable);
         }
      }

      return lookupObject;
   }

   public Object lookup (String name)
      throws NamingException {

      return lookup(nameParser.parse(name));
   }

   public void bind (Name name, Object obj)
      throws NamingException {

      ContextNamePair contextNamePair;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      contextNamePair.getContext().bind(contextNamePair.getName(), obj);
   }

   public void bind (String name, Object obj)
      throws NamingException {

      bind(nameParser.parse(name), obj);
   }

   public void rebind (Name name, Object obj)
      throws NamingException {

      ContextNamePair contextNamePair;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      contextNamePair.getContext().rebind(contextNamePair.getName(), obj);
   }

   public void rebind (String name, Object obj)
      throws NamingException {

      rebind(nameParser.parse(name), obj);
   }

   public void unbind (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      contextNamePair.getContext().unbind(contextNamePair.getName());
   }

   public void unbind (String name)
      throws NamingException {

      unbind(nameParser.parse(name));
   }

   public void rename (Name oldName, Name newName)
      throws NamingException {

      ContextNamePair contextNamePair;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, oldName);
      contextNamePair.getContext().rename(contextNamePair.getName(), nameTranslator.fromInternalNameToExternalName(newName));
   }

   public void rename (String oldName, String newName)
      throws NamingException {

      rename(nameParser.parse(oldName), nameParser.parse(newName));
   }

   public NamingEnumeration<NameClassPair> list (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;
      NamingEnumeration<NameClassPair> internalEnumeration;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      if ((internalEnumeration = contextNamePair.getContext().list(contextNamePair.getName())) != null) {

         return new JavaNamingEnumeration<NameClassPair>(NameClassPair.class, internalEnumeration, contextNamePair.getContext().getClass(), environment, nameTranslator, nameParser, modifiable);
      }

      return null;
   }

   public NamingEnumeration<NameClassPair> list (String name)
      throws NamingException {

      return list(nameParser.parse(name));
   }

   public NamingEnumeration<Binding> listBindings (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;
      NamingEnumeration<Binding> internalEnumeration;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      if ((internalEnumeration = contextNamePair.getContext().listBindings(contextNamePair.getName())) != null) {

         return new JavaNamingEnumeration<Binding>(Binding.class, internalEnumeration, contextNamePair.getContext().getClass(), environment, nameTranslator, nameParser, modifiable);
      }

      return null;
   }

   public NamingEnumeration<Binding> listBindings (String name)
      throws NamingException {

      return listBindings(nameParser.parse(name));
   }

   public void destroySubcontext (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      contextNamePair.getContext().destroySubcontext(contextNamePair.getName());
   }

   public void destroySubcontext (String name)
      throws NamingException {

      destroySubcontext(nameParser.parse(name));
   }

   public Context createSubcontext (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;
      Context createdContext;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      createdContext = contextNamePair.getContext().createSubcontext(contextNamePair.getName());

      return new JavaContext(environment, (DirContext)createdContext, nameTranslator, nameParser, modifiable);
   }

   public Context createSubcontext (String name)
      throws NamingException {

      return createSubcontext(nameParser.parse(name));
   }

   public Object lookupLink (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;
      Object lookupObject;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      lookupObject = contextNamePair.getContext().lookupLink(contextNamePair.getName());

      if (lookupObject.getClass().equals(contextNamePair.getContext().getClass())) {

         return new JavaContext(environment, (DirContext)lookupObject, nameTranslator, nameParser, modifiable);
      }

      return lookupObject;
   }

   public Object lookupLink (String name)
      throws NamingException {

      return lookupLink(nameParser.parse(name));
   }

   public NameParser getNameParser (Name name)
      throws NamingException {

      return nameParser;
   }

   public NameParser getNameParser (String name)
      throws NamingException {

      return getNameParser(nameParser.parse(name));
   }

   public Name composeName (Name name, Name prefix)
      throws NamingException {

      return ((Name)prefix.clone()).addAll(name);
   }

   public String composeName (String name, String prefix)
      throws NamingException {

      return nameParser.unparse(composeName(nameParser.parse(name), nameParser.parse(prefix)));
   }

   public Object addToEnvironment (String propName, Object propVal)
      throws NamingException {

      Object prevObject;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }
      prevObject = environment.get(propName);
      environment.put(propName, propVal);
      return prevObject;
   }

   public Object removeFromEnvironment (String propName)
      throws NamingException {

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }
      return environment.remove(propName);
   }

   public Hashtable getEnvironment ()
      throws NamingException {

      return environment;
   }

   public void close ()
      throws NamingException {

      if (internalContext != null) {
         internalContext.close();
      }
   }

   public void finalize ()
      throws NamingException {

      close();
   }

   public String getNameInNamespace ()
      throws NamingException {

      return nameTranslator.fromAbsoluteExternalStringToInternalString(internalContext.getNameInNamespace());
   }

   public Attributes getAttributes (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

      return contextNamePair.getContext().getAttributes(contextNamePair.getName());
   }

   public Attributes getAttributes (String name)
      throws NamingException {

      return getAttributes(nameParser.parse(name));
   }

   public Attributes getAttributes (Name name, String[] attrIds)
      throws NamingException {

      ContextNamePair contextNamePair;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

      return contextNamePair.getContext().getAttributes(contextNamePair.getName(), attrIds);
   }

   public Attributes getAttributes (String name, String[] attrIds)
      throws NamingException {

      return getAttributes(nameParser.parse(name), attrIds);
   }

   public void modifyAttributes (Name name, int mod_op, Attributes attrs)
      throws NamingException {

      ContextNamePair contextNamePair;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      contextNamePair.getContext().modifyAttributes(contextNamePair.getName(), mod_op, attrs);
   }

   public void modifyAttributes (String name, int mod_op, Attributes attrs)
      throws NamingException {

      modifyAttributes(nameParser.parse(name), mod_op, attrs);
   }

   public void modifyAttributes (Name name, ModificationItem[] mods)
      throws NamingException {

      ContextNamePair contextNamePair;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      contextNamePair.getContext().modifyAttributes(contextNamePair.getName(), mods);
   }

   public void modifyAttributes (String name, ModificationItem[] mods)
      throws NamingException {

      modifyAttributes(nameParser.parse(name), mods);
   }

   public void bind (Name name, Object obj, Attributes attrs)
      throws NamingException {

      ContextNamePair contextNamePair;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      contextNamePair.getContext().bind(contextNamePair.getName(), obj, attrs);
   }

   public void bind (String name, Object obj, Attributes attrs)
      throws NamingException {

      bind(nameParser.parse(name), obj, attrs);
   }

   public void rebind (Name name, Object obj, Attributes attrs)
      throws NamingException {

      ContextNamePair contextNamePair;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      contextNamePair.getContext().rebind(contextNamePair.getName(), obj, attrs);
   }

   public void rebind (String name, Object obj, Attributes attrs)
      throws NamingException {

      rebind(nameParser.parse(name), obj, attrs);
   }

   public DirContext createSubcontext (Name name, Attributes attrs)
      throws NamingException {

      ContextNamePair contextNamePair;
      Context createdContext;

      if (!modifiable) {
         throw new OperationNotSupportedException("This backing store is not modifiable");
      }

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      createdContext = contextNamePair.getContext().createSubcontext(contextNamePair.getName(), attrs);

      return new JavaContext(environment, (DirContext)createdContext, nameTranslator, nameParser, modifiable);
   }

   public DirContext createSubcontext (String name, Attributes attrs)
      throws NamingException {

      return createSubcontext(nameParser.parse(name), attrs);
   }

   public DirContext getSchema (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

      return contextNamePair.getContext().getSchema(contextNamePair.getName());
   }

   public DirContext getSchema (String name)
      throws NamingException {

      return getSchema(nameParser.parse(name));
   }

   public DirContext getSchemaClassDefinition (Name name)
      throws NamingException {

      ContextNamePair contextNamePair;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

      return contextNamePair.getContext().getSchemaClassDefinition(contextNamePair.getName());
   }

   public DirContext getSchemaClassDefinition (String name)
      throws NamingException {

      return getSchemaClassDefinition(nameParser.parse(name));
   }

   public NamingEnumeration<SearchResult> search (Name name, Attributes matchingAttributes, String[] attributesToReturn)
      throws NamingException {

      ContextNamePair contextNamePair;
      NamingEnumeration<SearchResult> internalEnumeration;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      if ((internalEnumeration = contextNamePair.getContext().search(contextNamePair.getName(), matchingAttributes, attributesToReturn)) != null) {

         return new JavaNamingEnumeration<SearchResult>(SearchResult.class, internalEnumeration, contextNamePair.getContext().getClass(), environment, nameTranslator, nameParser, modifiable);
      }

      return null;
   }

   public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes, String[] attributesToReturn)
      throws NamingException {

      return search(nameParser.parse(name), matchingAttributes, attributesToReturn);
   }

   public NamingEnumeration<SearchResult> search (Name name, Attributes matchingAttributes)
      throws NamingException {

      ContextNamePair contextNamePair;
      NamingEnumeration<SearchResult> internalEnumeration;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      if ((internalEnumeration = contextNamePair.getContext().search(contextNamePair.getName(), matchingAttributes)) != null) {

         return new JavaNamingEnumeration<SearchResult>(SearchResult.class, internalEnumeration, contextNamePair.getContext().getClass(), environment, nameTranslator, nameParser, modifiable);
      }

      return null;
   }

   public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes)
      throws NamingException {

      return search(nameParser.parse(name), matchingAttributes);
   }

   public NamingEnumeration<SearchResult> search (Name name, String filter, SearchControls cons)
      throws NamingException {

      ContextNamePair contextNamePair;
      NamingEnumeration<SearchResult> internalEnumeration;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
      if ((internalEnumeration = contextNamePair.getContext().search(contextNamePair.getName(), filter, cons)) != null) {

         return new JavaNamingEnumeration<SearchResult>(SearchResult.class, internalEnumeration, contextNamePair.getContext().getClass(), environment, nameTranslator, nameParser, modifiable);
      }

      return null;
   }

   public NamingEnumeration<SearchResult> search (String name, String filter, SearchControls cons)
      throws NamingException {

      return search(nameParser.parse(name), filter, cons);
   }

   public NamingEnumeration<SearchResult> search (Name name, String filterExpr, Object[] filterArgs, SearchControls cons)
      throws NamingException {

      ContextNamePair contextNamePair;
      NamingEnumeration<SearchResult> internalEnumeration;

      contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

      if ((internalEnumeration = contextNamePair.getContext().search(contextNamePair.getName(), filterExpr, filterArgs, cons)) != null) {

         return new JavaNamingEnumeration<SearchResult>(SearchResult.class, internalEnumeration, contextNamePair.getContext().getClass(), environment, nameTranslator, nameParser, modifiable);
      }

      return null;
   }

   public NamingEnumeration<SearchResult> search (String name, String filterExpr, Object[] filterArgs, SearchControls cons)
      throws NamingException {

      return search(nameParser.parse(name), filterExpr, filterArgs, cons);
   }
}
