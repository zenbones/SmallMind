/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.quorum.namespace;

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
import org.smallmind.quorum.namespace.backingStore.NameTranslator;

/**
 * JNDI {@link DirContext} wrapper that translates names between internal and external representations
 * and optionally wraps nested contexts. Supports pooled and modifiable configurations.
 */
public class JavaContext implements DirContext {

  public static final String CONNECTION_DETAILS = "org.smallmind.quorum.namespace.java.connection details";
  public static final String CONTEXT_STORE = "org.smallmind.quorum.namespace.java.store";
  public static final String CONTEXT_MODIFIABLE = "org.smallmind.quorum.namespace.java.modifiable";
  public static final String POOLED_CONNECTION = "org.smallmind.quorum.namespace.java.pooled";

  private final Hashtable<String, Object> environment;
  private final DirContext internalContext;
  private final NameTranslator nameTranslator;
  private final JavaNameParser nameParser;
  private final boolean modifiable;
  private final boolean pooled;

  /**
   * Constructs a top-level JavaContext without an existing internal context.
   *
   * @param nameTranslator translator for converting names
   * @param environment    JNDI environment
   * @param modifiable     whether the backing store allows mutations
   * @param pooled         whether created child contexts should be pooled
   */
  protected JavaContext (NameTranslator nameTranslator, Hashtable<String, Object> environment, boolean modifiable, boolean pooled) {

    this.nameTranslator = nameTranslator;
    this.environment = environment;
    this.modifiable = modifiable;
    this.pooled = pooled;

    internalContext = null;
    nameParser = new JavaNameParser(nameTranslator);
  }

  /**
   * Constructs a JavaContext that wraps an existing internal {@link DirContext}.
   *
   * @param environment     JNDI environment
   * @param internalContext backing context
   * @param nameTranslator  translator used for name conversions
   * @param nameParser      parser to use for string names
   * @param modifiable      whether the backing store allows mutations
   */
  protected JavaContext (Hashtable<String, Object> environment, DirContext internalContext, NameTranslator nameTranslator, JavaNameParser nameParser, boolean modifiable) {

    this.environment = environment;
    this.internalContext = internalContext;
    this.nameTranslator = nameTranslator;
    this.nameParser = nameParser;
    this.modifiable = modifiable;

    pooled = false;
  }

  /**
   * Ensures that each element of the path exists by creating subcontexts when necessary.
   *
   * @param javaContext context used to resolve/create the path
   * @param namingPath  slash-delimited path
   * @return the deepest existing/created context
   * @throws NamingException if lookup or creation fails
   */
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
      } catch (NameNotFoundException n) {
        lastContext = (JavaContext)javaContext.createSubcontext(pathSoFar.toString());
      }
    }

    return lastContext;
  }

  /**
   * Looks up an object using a {@link Name}, converting to the underlying context.
   *
   * @param name name to resolve
   * @return located object or wrapped context
   * @throws NamingException if lookup fails
   */
  public Object lookup (Name name)
    throws NamingException {

    ContextNamePair contextNamePair;
    Object lookupObject;

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
    lookupObject = contextNamePair.getContext().lookup(contextNamePair.getName());
    if (lookupObject.getClass().equals(contextNamePair.getContext().getClass())) {
      if (pooled) {
        return new PooledJavaContext(environment, (DirContext)lookupObject, nameTranslator, nameParser, modifiable);
      } else {
        return new JavaContext(environment, (DirContext)lookupObject, nameTranslator, nameParser, modifiable);
      }
    }

    return lookupObject;
  }

  /**
   * Looks up an object using a string name.
   *
   * @param name string representation of the name
   * @return located object or wrapped context
   * @throws NamingException if lookup fails
   */
  public Object lookup (String name)
    throws NamingException {

    return lookup(nameParser.parse(name));
  }

  /**
   * Binds an object to a name.
   *
   * @param name name to bind
   * @param obj  object to bind
   * @throws NamingException if binding fails or store is not modifiable
   */
  public void bind (Name name, Object obj)
    throws NamingException {

    ContextNamePair contextNamePair;

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
    contextNamePair.getContext().bind(contextNamePair.getName(), obj);
  }

  /**
   * Binds an object to a string name.
   *
   * @param name name to bind
   * @param obj  object to bind
   * @throws NamingException if binding fails or store is not modifiable
   */
  public void bind (String name, Object obj)
    throws NamingException {

    bind(nameParser.parse(name), obj);
  }

  /**
   * Rebinds an object to a name, replacing any existing binding.
   *
   * @param name name to bind
   * @param obj  object to bind
   * @throws NamingException if rebinding fails or store is not modifiable
   */
  public void rebind (Name name, Object obj)
    throws NamingException {

    ContextNamePair contextNamePair;

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
    contextNamePair.getContext().rebind(contextNamePair.getName(), obj);
  }

  /**
   * Rebinds an object to a string name, replacing any existing binding.
   *
   * @param name name to bind
   * @param obj  object to bind
   * @throws NamingException if rebinding fails or store is not modifiable
   */
  public void rebind (String name, Object obj)
    throws NamingException {

    rebind(nameParser.parse(name), obj);
  }

  /**
   * Unbinds an object from the provided name.
   *
   * @param name name to unbind
   * @throws NamingException if unbinding fails or store is not modifiable
   */
  public void unbind (Name name)
    throws NamingException {

    ContextNamePair contextNamePair;

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
    contextNamePair.getContext().unbind(contextNamePair.getName());
  }

  /**
   * Unbinds an object from the provided string name.
   *
   * @param name name to unbind
   * @throws NamingException if unbinding fails or store is not modifiable
   */
  public void unbind (String name)
    throws NamingException {

    unbind(nameParser.parse(name));
  }

  /**
   * Renames an entry.
   *
   * @param oldName existing name
   * @param newName new name
   * @throws NamingException if renaming fails or store is not modifiable
   */
  public void rename (Name oldName, Name newName)
    throws NamingException {

    ContextNamePair contextNamePair;

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, oldName);
    contextNamePair.getContext().rename(contextNamePair.getName(), nameTranslator.fromInternalNameToExternalName(newName));
  }

  /**
   * Renames an entry using string names.
   *
   * @param oldName existing name
   * @param newName new name
   * @throws NamingException if renaming fails or store is not modifiable
   */
  public void rename (String oldName, String newName)
    throws NamingException {

    rename(nameParser.parse(oldName), nameParser.parse(newName));
  }

  /**
   * Lists name/class pairs beneath the supplied name.
   *
   * @param name base name
   * @return enumeration of name/class pairs or {@code null}
   * @throws NamingException if listing fails
   */
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

  /**
   * Lists name/class pairs beneath a string name.
   *
   * @param name base name
   * @return enumeration of name/class pairs or {@code null}
   * @throws NamingException if listing fails
   */
  public NamingEnumeration<NameClassPair> list (String name)
    throws NamingException {

    return list(nameParser.parse(name));
  }

  /**
   * Lists bindings beneath the supplied name.
   *
   * @param name base name
   * @return enumeration of bindings or {@code null}
   * @throws NamingException if listing fails
   */
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

  /**
   * Lists bindings beneath the supplied string name.
   *
   * @param name base name
   * @return enumeration of bindings or {@code null}
   * @throws NamingException if listing fails
   */
  public NamingEnumeration<Binding> listBindings (String name)
    throws NamingException {

    return listBindings(nameParser.parse(name));
  }

  /**
   * Destroys a subcontext.
   *
   * @param name name of subcontext to destroy
   * @throws NamingException if destruction fails or store is not modifiable
   */
  public void destroySubcontext (Name name)
    throws NamingException {

    ContextNamePair contextNamePair;

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
    contextNamePair.getContext().destroySubcontext(contextNamePair.getName());
  }

  /**
   * Destroys a subcontext referenced by string name.
   *
   * @param name name of subcontext to destroy
   * @throws NamingException if destruction fails or store is not modifiable
   */
  public void destroySubcontext (String name)
    throws NamingException {

    destroySubcontext(nameParser.parse(name));
  }

  /**
   * Creates a subcontext.
   *
   * @param name name of subcontext to create
   * @return created context wrapped in {@link JavaContext}
   * @throws NamingException if creation fails or store is not modifiable
   */
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

  /**
   * Creates a subcontext using a string name.
   *
   * @param name name of subcontext to create
   * @return created context wrapped in {@link JavaContext}
   * @throws NamingException if creation fails or store is not modifiable
   */
  public Context createSubcontext (String name)
    throws NamingException {

    return createSubcontext(nameParser.parse(name));
  }

  /**
   * Looks up a link reference.
   *
   * @param name name to resolve
   * @return linked object or wrapped context
   * @throws NamingException if lookup fails
   */
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

  /**
   * Looks up a link reference using a string name.
   *
   * @param name name to resolve
   * @return linked object or wrapped context
   * @throws NamingException if lookup fails
   */
  public Object lookupLink (String name)
    throws NamingException {

    return lookupLink(nameParser.parse(name));
  }

  /**
   * Returns the parser used for this context.
   *
   * @param name name whose parser is requested
   * @return name parser
   * @throws NamingException if retrieval fails
   */
  public NameParser getNameParser (Name name)
    throws NamingException {

    return nameParser;
  }

  /**
   * Returns the parser used for this context using a string name.
   *
   * @param name name whose parser is requested
   * @return name parser
   * @throws NamingException if retrieval fails
   */
  public NameParser getNameParser (String name)
    throws NamingException {

    return getNameParser(nameParser.parse(name));
  }

  /**
   * Composes two names.
   *
   * @param name   name to add
   * @param prefix prefix to prepend
   * @return composed name
   * @throws NamingException if composition fails
   */
  public Name composeName (Name name, Name prefix)
    throws NamingException {

    return ((Name)prefix.clone()).addAll(name);
  }

  /**
   * Composes two string names.
   *
   * @param name   name to add
   * @param prefix prefix to prepend
   * @return composed name string
   * @throws NamingException if composition fails
   */
  public String composeName (String name, String prefix)
    throws NamingException {

    return nameParser.unparse(composeName(nameParser.parse(name), nameParser.parse(prefix)));
  }

  /**
   * Adds a property to the environment.
   *
   * @param propName property name
   * @param propVal  property value
   * @return previous value associated with the property
   * @throws NamingException if the store is not modifiable
   */
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

  /**
   * Removes a property from the environment.
   *
   * @param propName property name
   * @return previous value associated with the property
   * @throws NamingException if the store is not modifiable
   */
  public Object removeFromEnvironment (String propName)
    throws NamingException {

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }
    return environment.remove(propName);
  }

  /**
   * Returns the backing environment hashtable.
   *
   * @return environment
   * @throws NamingException never thrown
   */
  public Hashtable getEnvironment ()
    throws NamingException {

    return environment;
  }

  /**
   * Closes the internal context if present.
   *
   * @throws NamingException if close fails
   */
  public void close ()
    throws NamingException {

    if (internalContext != null) {
      internalContext.close();
    }
  }

  /**
   * Returns the fully qualified name of this context in the namespace.
   *
   * @return full name
   * @throws NamingException if retrieval fails
   */
  public String getNameInNamespace ()
    throws NamingException {

    return nameTranslator.fromAbsoluteExternalStringToInternalString(internalContext.getNameInNamespace());
  }

  /**
   * Retrieves attributes associated with a name.
   *
   * @param name name whose attributes to fetch
   * @return attributes
   * @throws NamingException if retrieval fails
   */
  public Attributes getAttributes (Name name)
    throws NamingException {

    ContextNamePair contextNamePair;

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

    return contextNamePair.getContext().getAttributes(contextNamePair.getName());
  }

  /**
   * Retrieves attributes associated with a string name.
   *
   * @param name name whose attributes to fetch
   * @return attributes
   * @throws NamingException if retrieval fails
   */
  public Attributes getAttributes (String name)
    throws NamingException {

    return getAttributes(nameParser.parse(name));
  }

  /**
   * Retrieves specific attributes associated with a name.
   *
   * @param name    name whose attributes to fetch
   * @param attrIds attribute identifiers to return
   * @return attributes
   * @throws NamingException if retrieval fails
   */
  public Attributes getAttributes (Name name, String[] attrIds)
    throws NamingException {

    ContextNamePair contextNamePair;

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

    return contextNamePair.getContext().getAttributes(contextNamePair.getName(), attrIds);
  }

  /**
   * Retrieves specific attributes associated with a string name.
   *
   * @param name    name whose attributes to fetch
   * @param attrIds attribute identifiers to return
   * @return attributes
   * @throws NamingException if retrieval fails
   */
  public Attributes getAttributes (String name, String[] attrIds)
    throws NamingException {

    return getAttributes(nameParser.parse(name), attrIds);
  }

  /**
   * Modifies attributes using the supplied operation.
   *
   * @param name   name whose attributes to modify
   * @param mod_op modification operation
   * @param attrs  attributes to apply
   * @throws NamingException if modification fails or store is not modifiable
   */
  public void modifyAttributes (Name name, int mod_op, Attributes attrs)
    throws NamingException {

    ContextNamePair contextNamePair;

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
    contextNamePair.getContext().modifyAttributes(contextNamePair.getName(), mod_op, attrs);
  }

  /**
   * Modifies attributes using the supplied operation.
   *
   * @param name   name whose attributes to modify
   * @param mod_op modification operation
   * @param attrs  attributes to apply
   * @throws NamingException if modification fails or store is not modifiable
   */
  public void modifyAttributes (String name, int mod_op, Attributes attrs)
    throws NamingException {

    modifyAttributes(nameParser.parse(name), mod_op, attrs);
  }

  /**
   * Applies an array of modifications to attributes.
   *
   * @param name name whose attributes to modify
   * @param mods modifications to apply
   * @throws NamingException if modification fails or store is not modifiable
   */
  public void modifyAttributes (Name name, ModificationItem[] mods)
    throws NamingException {

    ContextNamePair contextNamePair;

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
    contextNamePair.getContext().modifyAttributes(contextNamePair.getName(), mods);
  }

  /**
   * Applies an array of modifications to attributes using a string name.
   *
   * @param name name whose attributes to modify
   * @param mods modifications to apply
   * @throws NamingException if modification fails or store is not modifiable
   */
  public void modifyAttributes (String name, ModificationItem[] mods)
    throws NamingException {

    modifyAttributes(nameParser.parse(name), mods);
  }

  /**
   * Binds an object with attributes to a name.
   *
   * @param name  name to bind
   * @param obj   object to bind
   * @param attrs attributes to associate
   * @throws NamingException if binding fails or store is not modifiable
   */
  public void bind (Name name, Object obj, Attributes attrs)
    throws NamingException {

    ContextNamePair contextNamePair;

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
    contextNamePair.getContext().bind(contextNamePair.getName(), obj, attrs);
  }

  /**
   * Binds an object with attributes to a string name.
   *
   * @param name  name to bind
   * @param obj   object to bind
   * @param attrs attributes to associate
   * @throws NamingException if binding fails or store is not modifiable
   */
  public void bind (String name, Object obj, Attributes attrs)
    throws NamingException {

    bind(nameParser.parse(name), obj, attrs);
  }

  /**
   * Rebinds an object with attributes to a name.
   *
   * @param name  name to bind
   * @param obj   object to bind
   * @param attrs attributes to associate
   * @throws NamingException if rebinding fails or store is not modifiable
   */
  public void rebind (Name name, Object obj, Attributes attrs)
    throws NamingException {

    ContextNamePair contextNamePair;

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);
    contextNamePair.getContext().rebind(contextNamePair.getName(), obj, attrs);
  }

  /**
   * Rebinds an object with attributes to a string name.
   *
   * @param name  name to bind
   * @param obj   object to bind
   * @param attrs attributes to associate
   * @throws NamingException if rebinding fails or store is not modifiable
   */
  public void rebind (String name, Object obj, Attributes attrs)
    throws NamingException {

    rebind(nameParser.parse(name), obj, attrs);
  }

  /**
   * Creates a subcontext with attributes.
   *
   * @param name  name of subcontext
   * @param attrs attributes to apply
   * @return created context wrapped in {@link JavaContext}
   * @throws NamingException if creation fails or store is not modifiable
   */
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

  /**
   * Creates a subcontext with attributes using a string name.
   *
   * @param name  name of subcontext
   * @param attrs attributes to apply
   * @return created context wrapped in {@link JavaContext}
   * @throws NamingException if creation fails or store is not modifiable
   */
  public DirContext createSubcontext (String name, Attributes attrs)
    throws NamingException {

    return createSubcontext(nameParser.parse(name), attrs);
  }

  /**
   * Retrieves schema information for a name.
   *
   * @param name name whose schema to fetch
   * @return schema context
   * @throws NamingException if retrieval fails
   */
  public DirContext getSchema (Name name)
    throws NamingException {

    ContextNamePair contextNamePair;

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

    return contextNamePair.getContext().getSchema(contextNamePair.getName());
  }

  /**
   * Retrieves schema information for a string name.
   *
   * @param name name whose schema to fetch
   * @return schema context
   * @throws NamingException if retrieval fails
   */
  public DirContext getSchema (String name)
    throws NamingException {

    return getSchema(nameParser.parse(name));
  }

  /**
   * Retrieves schema class definition for a name.
   *
   * @param name name whose class definition to fetch
   * @return schema class definition context
   * @throws NamingException if retrieval fails
   */
  public DirContext getSchemaClassDefinition (Name name)
    throws NamingException {

    ContextNamePair contextNamePair;

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

    return contextNamePair.getContext().getSchemaClassDefinition(contextNamePair.getName());
  }

  /**
   * Retrieves schema class definition for a string name.
   *
   * @param name name whose class definition to fetch
   * @return schema class definition context
   * @throws NamingException if retrieval fails
   */
  public DirContext getSchemaClassDefinition (String name)
    throws NamingException {

    return getSchemaClassDefinition(nameParser.parse(name));
  }

  /**
   * Searches the directory with attributes and an attribute filter.
   *
   * @param name               base name
   * @param matchingAttributes attributes to match
   * @param attributesToReturn attributes to return
   * @return search results
   * @throws NamingException if search fails
   */
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

  /**
   * Searches the directory with attributes and an attribute filter using a string name.
   *
   * @param name               base name
   * @param matchingAttributes attributes to match
   * @param attributesToReturn attributes to return
   * @return search results
   * @throws NamingException if search fails
   */
  public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes, String[] attributesToReturn)
    throws NamingException {

    return search(nameParser.parse(name), matchingAttributes, attributesToReturn);
  }

  /**
   * Searches the directory with attributes.
   *
   * @param name               base name
   * @param matchingAttributes attributes to match
   * @return search results
   * @throws NamingException if search fails
   */
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

  /**
   * Searches the directory with attributes using a string name.
   *
   * @param name               base name
   * @param matchingAttributes attributes to match
   * @return search results
   * @throws NamingException if search fails
   */
  public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes)
    throws NamingException {

    return search(nameParser.parse(name), matchingAttributes);
  }

  /**
   * Searches the directory using a filter expression.
   *
   * @param name   base name
   * @param filter search filter
   * @param cons   search controls
   * @return search results
   * @throws NamingException if search fails
   */
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

  /**
   * Searches the directory using a filter expression and string name.
   *
   * @param name   base name
   * @param filter search filter
   * @param cons   search controls
   * @return search results
   * @throws NamingException if search fails
   */
  public NamingEnumeration<SearchResult> search (String name, String filter, SearchControls cons)
    throws NamingException {

    return search(nameParser.parse(name), filter, cons);
  }

  /**
   * Searches the directory using a filter expression with parameters.
   *
   * @param name       base name
   * @param filterExpr filter expression
   * @param filterArgs filter arguments
   * @param cons       search controls
   * @return search results
   * @throws NamingException if search fails
   */
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

  /**
   * Searches the directory using a filter expression with parameters and string name.
   *
   * @param name       base name
   * @param filterExpr filter expression
   * @param filterArgs filter arguments
   * @param cons       search controls
   * @return search results
   * @throws NamingException if search fails
   */
  public NamingEnumeration<SearchResult> search (String name, String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException {

    return search(nameParser.parse(name), filterExpr, filterArgs, cons);
  }
}
