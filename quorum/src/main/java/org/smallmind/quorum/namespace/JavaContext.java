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
 * JNDI {@link DirContext} implementation that fronts an arbitrary backing-store directory context
 * (for example, LDAP) and translates between the {@code java:} namespace's internal name format
 * and the backing store's native name format on every operation.
 * <p>
 * Two construction paths exist:
 * <ul>
 *   <li>Top-level context (no backing context): created by {@link javaURLContextFactory} when no
 *       existing context is available. The first component of any name supplied to this context
 *       must be {@code java:}; the translator opens a new initial context for each such call.</li>
 *   <li>Nested context (wraps an existing {@link DirContext}): created when {@link #lookup} or
 *       {@link #createSubcontext} returns a directory context. The backing context is used directly
 *       without opening a new connection.</li>
 * </ul>
 * <p>
 * Mutation operations ({@link #bind}, {@link #rebind}, {@link #unbind}, {@link #rename},
 * {@link #destroySubcontext}, {@link #createSubcontext}, {@link #modifyAttributes},
 * {@link #addToEnvironment}, {@link #removeFromEnvironment}) throw
 * {@link OperationNotSupportedException} when the context is not modifiable.
 * <p>
 * Enumeration operations ({@link #list}, {@link #listBindings}, {@link #search}) wrap the
 * backing-store result in a {@link JavaNamingEnumeration} that translates each element on the fly.
 * <p>
 * Every {@code Name}-typed method has a {@code String}-typed overload that parses the string
 * with {@link JavaNameParser#parse} before delegating.
 */
public class JavaContext implements DirContext {

  /**
   * Environment key whose value is a {@link org.smallmind.quorum.namespace.backingStore.NamingConnectionDetails} instance.
   */
  public static final String CONNECTION_DETAILS = "org.smallmind.quorum.namespace.java.connection details";
  /**
   * Environment key whose value is the short backing-store identifier string (e.g., {@code ldap}).
   */
  public static final String CONTEXT_STORE = "org.smallmind.quorum.namespace.java.store";
  /**
   * Environment key whose value is {@code "true"} to allow mutations on the backing store.
   */
  public static final String CONTEXT_MODIFIABLE = "org.smallmind.quorum.namespace.java.modifiable";
  /**
   * Environment key whose value is {@code "true"} to return pooled child contexts from {@link #lookup}.
   */
  public static final String POOLED_CONNECTION = "org.smallmind.quorum.namespace.java.pooled";

  private final Hashtable<String, Object> environment;
  private final DirContext internalContext;
  private final NameTranslator nameTranslator;
  private final JavaNameParser nameParser;
  private final boolean modifiable;
  private final boolean pooled;

  /**
   * Creates a top-level context with no pre-existing backing context.
   * <p>
   * Used by {@link javaURLContextFactory} when constructing the initial context. Every name
   * resolved through this instance must begin with {@code java:}; the translator will open a new
   * initial context from the {@link org.smallmind.quorum.namespace.backingStore.ContextCreator}
   * on each such call.
   *
   * @param nameTranslator the translator that converts between internal and external name forms
   * @param environment    the JNDI environment for this context
   * @param modifiable     {@code true} to allow mutations on the backing store
   * @param pooled         {@code true} to wrap child contexts returned by {@link #lookup} in
   *                       {@link PooledJavaContext} instances
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
   * Creates a nested context that wraps an existing backing-store {@link DirContext}.
   * <p>
   * Used when {@link #lookup} or {@link #createSubcontext} returns a directory context from the
   * backing store. The supplied context is used directly for all operations without opening a
   * new connection. Pooling is always disabled for nested contexts.
   *
   * @param environment     the JNDI environment for this context
   * @param internalContext the backing-store context to wrap
   * @param nameTranslator  the translator shared with the parent context
   * @param nameParser      the name parser shared with the parent context
   * @param modifiable      {@code true} to allow mutations on the backing store
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
   * Ensures that every path component of {@code namingPath} exists as a subcontext of
   * {@code javaContext}, creating missing nodes one at a time from the outermost inward.
   * <p>
   * The path is split on {@code /} and accumulated from the end; each accumulated candidate is
   * looked up, and a {@link NameNotFoundException} triggers a {@link #createSubcontext} call.
   *
   * @param javaContext the context relative to which the path is resolved and created
   * @param namingPath  the slash-delimited path whose nodes should all exist
   * @return the deepest context that was either found or created
   * @throws NamingException if any lookup or subcontext creation fails for a reason other than the
   *                         node not yet existing
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
   * Looks up the object bound to {@code name}.
   * <p>
   * If the backing store returns a directory context of the same class as the current context, it is
   * wrapped in a new {@link JavaContext} (or {@link PooledJavaContext} when pooling is enabled).
   *
   * @param name the internal name to look up
   * @return the bound object, or a {@link JavaContext} wrapping a nested directory context
   * @throws NamingException if the name is not bound or the backing store lookup fails
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
   * Looks up the object bound to the string name {@code name}.
   *
   * @param name the slash-delimited internal name to look up
   * @return the bound object, or a {@link JavaContext} wrapping a nested directory context
   * @throws NamingException if the name is not bound or the backing store lookup fails
   */
  public Object lookup (String name)
    throws NamingException {

    return lookup(nameParser.parse(name));
  }

  /**
   * Binds {@code obj} to {@code name} in the backing store.
   *
   * @param name the internal name to bind
   * @param obj  the object to bind
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the bind fails for any other reason
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
   * Binds {@code obj} to the string name {@code name} in the backing store.
   *
   * @param name the slash-delimited internal name to bind
   * @param obj  the object to bind
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the bind fails for any other reason
   */
  public void bind (String name, Object obj)
    throws NamingException {

    bind(nameParser.parse(name), obj);
  }

  /**
   * Binds {@code obj} to {@code name}, replacing any existing binding.
   *
   * @param name the internal name to rebind
   * @param obj  the object to bind
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the rebind fails for any other reason
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
   * Binds {@code obj} to the string name {@code name}, replacing any existing binding.
   *
   * @param name the slash-delimited internal name to rebind
   * @param obj  the object to bind
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the rebind fails for any other reason
   */
  public void rebind (String name, Object obj)
    throws NamingException {

    rebind(nameParser.parse(name), obj);
  }

  /**
   * Removes the binding for {@code name} from the backing store.
   *
   * @param name the internal name whose binding is to be removed
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the unbind fails for any other reason
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
   * Removes the binding for the string name {@code name} from the backing store.
   *
   * @param name the slash-delimited internal name whose binding is to be removed
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the unbind fails for any other reason
   */
  public void unbind (String name)
    throws NamingException {

    unbind(nameParser.parse(name));
  }

  /**
   * Renames the entry at {@code oldName} to {@code newName} in the backing store.
   *
   * @param oldName the current internal name of the entry
   * @param newName the new internal name for the entry
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the rename fails for any other reason
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
   * Renames the entry at the string name {@code oldName} to the string name {@code newName}.
   *
   * @param oldName the slash-delimited current internal name
   * @param newName the slash-delimited new internal name
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the rename fails for any other reason
   */
  public void rename (String oldName, String newName)
    throws NamingException {

    rename(nameParser.parse(oldName), nameParser.parse(newName));
  }

  /**
   * Returns an enumeration of the names and class names of all bindings under {@code name}.
   * <p>
   * The returned enumeration translates each element on the fly; directory context class names
   * are replaced with {@code JavaContext}.
   *
   * @param name the internal name of the context to list
   * @return a {@link JavaNamingEnumeration} of {@link NameClassPair}s, or {@code null} if the
   * backing store returns {@code null}
   * @throws NamingException if the list operation fails
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
   * Returns an enumeration of the names and class names of all bindings under the string name
   * {@code name}.
   *
   * @param name the slash-delimited internal name of the context to list
   * @return a {@link JavaNamingEnumeration} of {@link NameClassPair}s, or {@code null}
   * @throws NamingException if the list operation fails
   */
  public NamingEnumeration<NameClassPair> list (String name)
    throws NamingException {

    return list(nameParser.parse(name));
  }

  /**
   * Returns an enumeration of the name-object bindings under {@code name}.
   * <p>
   * The returned enumeration translates each element on the fly; directory context objects are
   * wrapped in new {@link JavaContext} instances.
   *
   * @param name the internal name of the context whose bindings to list
   * @return a {@link JavaNamingEnumeration} of {@link Binding}s, or {@code null} if the backing
   * store returns {@code null}
   * @throws NamingException if the list-bindings operation fails
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
   * Returns an enumeration of the name-object bindings under the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the context whose bindings to list
   * @return a {@link JavaNamingEnumeration} of {@link Binding}s, or {@code null}
   * @throws NamingException if the list-bindings operation fails
   */
  public NamingEnumeration<Binding> listBindings (String name)
    throws NamingException {

    return listBindings(nameParser.parse(name));
  }

  /**
   * Destroys the subcontext identified by {@code name}.
   *
   * @param name the internal name of the subcontext to destroy
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if destruction fails for any other reason
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
   * Destroys the subcontext identified by the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the subcontext to destroy
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if destruction fails for any other reason
   */
  public void destroySubcontext (String name)
    throws NamingException {

    destroySubcontext(nameParser.parse(name));
  }

  /**
   * Creates a new subcontext with the given name and returns it wrapped in a {@link JavaContext}.
   *
   * @param name the internal name of the subcontext to create
   * @return a new {@link JavaContext} wrapping the created backing-store subcontext
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if creation fails for any other reason
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
   * Creates a new subcontext with the string name {@code name} and returns it wrapped in a
   * {@link JavaContext}.
   *
   * @param name the slash-delimited internal name of the subcontext to create
   * @return a new {@link JavaContext} wrapping the created backing-store subcontext
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if creation fails for any other reason
   */
  public Context createSubcontext (String name)
    throws NamingException {

    return createSubcontext(nameParser.parse(name));
  }

  /**
   * Looks up the object bound to {@code name} without following link references.
   * <p>
   * If the result is a directory context it is wrapped in a new {@link JavaContext}.
   *
   * @param name the internal name to look up
   * @return the bound object or a {@link JavaContext} wrapping a nested directory context
   * @throws NamingException if the lookup fails
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
   * Looks up the object bound to the string name {@code name} without following link references.
   *
   * @param name the slash-delimited internal name to look up
   * @return the bound object or a {@link JavaContext} wrapping a nested directory context
   * @throws NamingException if the lookup fails
   */
  public Object lookupLink (String name)
    throws NamingException {

    return lookupLink(nameParser.parse(name));
  }

  /**
   * Returns the {@link NameParser} associated with this context.
   * <p>
   * The {@code name} parameter is accepted for API compatibility but is not used.
   *
   * @param name not used
   * @return the {@link JavaNameParser} held by this context
   * @throws NamingException never thrown by this implementation
   */
  public NameParser getNameParser (Name name)
    throws NamingException {

    return nameParser;
  }

  /**
   * Returns the {@link NameParser} associated with this context.
   * <p>
   * The {@code name} parameter is accepted for API compatibility but is not used.
   *
   * @param name not used
   * @return the {@link JavaNameParser} held by this context
   * @throws NamingException never thrown by this implementation
   */
  public NameParser getNameParser (String name)
    throws NamingException {

    return getNameParser(nameParser.parse(name));
  }

  /**
   * Composes two names by appending {@code name} to a clone of {@code prefix}.
   *
   * @param name   the name to append
   * @param prefix the prefix to prepend
   * @return a new {@link Name} equal to {@code prefix + name}
   * @throws NamingException if composition fails
   */
  public Name composeName (Name name, Name prefix)
    throws NamingException {

    return ((Name)prefix.clone()).addAll(name);
  }

  /**
   * Composes two string names and returns the result as a slash-delimited string.
   *
   * @param name   the name portion to append
   * @param prefix the prefix portion to prepend
   * @return the composed name string
   * @throws NamingException if parsing or composition fails
   */
  public String composeName (String name, String prefix)
    throws NamingException {

    return nameParser.unparse(composeName(nameParser.parse(name), nameParser.parse(prefix)));
  }

  /**
   * Adds a property to the JNDI environment, returning the previous value if any.
   *
   * @param propName the name of the environment property to add or replace
   * @param propVal  the new value for the property
   * @return the previous value associated with {@code propName}, or {@code null} if none
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                never thrown by this implementation beyond the
   *                                        modifiability check
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
   * Removes a property from the JNDI environment, returning its previous value if any.
   *
   * @param propName the name of the environment property to remove
   * @return the value that was associated with {@code propName}, or {@code null} if none
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                never thrown by this implementation beyond the
   *                                        modifiability check
   */
  public Object removeFromEnvironment (String propName)
    throws NamingException {

    if (!modifiable) {
      throw new OperationNotSupportedException("This backing store is not modifiable");
    }
    return environment.remove(propName);
  }

  /**
   * Returns the JNDI environment hashtable for this context.
   *
   * @return the environment; never {@code null}
   * @throws NamingException never thrown by this implementation
   */
  public Hashtable getEnvironment ()
    throws NamingException {

    return environment;
  }

  /**
   * Closes the backing-store context if one is held; does nothing for top-level contexts.
   *
   * @throws NamingException if the backing-store context's {@code close} method throws
   */
  public void close ()
    throws NamingException {

    if (internalContext != null) {
      internalContext.close();
    }
  }

  /**
   * Returns the fully qualified name of this context in the {@code java:} namespace.
   * <p>
   * Delegates to the backing context's {@link DirContext#getNameInNamespace()} and converts the
   * result from its external form back to the internal form using the name translator.
   *
   * @return the internal qualified name string
   * @throws NamingException if the backing context's {@code getNameInNamespace} throws or if the
   *                         returned string cannot be translated
   */
  public String getNameInNamespace ()
    throws NamingException {

    return nameTranslator.fromAbsoluteExternalStringToInternalString(internalContext.getNameInNamespace());
  }

  /**
   * Returns all attributes associated with the entry at {@code name}.
   *
   * @param name the internal name of the entry
   * @return all attributes for the entry
   * @throws NamingException if the backing-store operation fails
   */
  public Attributes getAttributes (Name name)
    throws NamingException {

    ContextNamePair contextNamePair;

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

    return contextNamePair.getContext().getAttributes(contextNamePair.getName());
  }

  /**
   * Returns all attributes associated with the entry at the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the entry
   * @return all attributes for the entry
   * @throws NamingException if the backing-store operation fails
   */
  public Attributes getAttributes (String name)
    throws NamingException {

    return getAttributes(nameParser.parse(name));
  }

  /**
   * Returns the specified attributes associated with the entry at {@code name}.
   *
   * @param name    the internal name of the entry
   * @param attrIds the identifiers of the attributes to return; {@code null} returns all attributes
   * @return the requested attributes for the entry
   * @throws NamingException if the backing-store operation fails
   */
  public Attributes getAttributes (Name name, String[] attrIds)
    throws NamingException {

    ContextNamePair contextNamePair;

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

    return contextNamePair.getContext().getAttributes(contextNamePair.getName(), attrIds);
  }

  /**
   * Returns the specified attributes associated with the entry at the string name {@code name}.
   *
   * @param name    the slash-delimited internal name of the entry
   * @param attrIds the identifiers of the attributes to return
   * @return the requested attributes for the entry
   * @throws NamingException if the backing-store operation fails
   */
  public Attributes getAttributes (String name, String[] attrIds)
    throws NamingException {

    return getAttributes(nameParser.parse(name), attrIds);
  }

  /**
   * Modifies the attributes of the entry at {@code name} using the given operation code and
   * attribute set.
   *
   * @param name   the internal name of the entry to modify
   * @param mod_op the modification operation ({@link DirContext#ADD_ATTRIBUTE},
   *               {@link DirContext#REPLACE_ATTRIBUTE}, or {@link DirContext#REMOVE_ATTRIBUTE})
   * @param attrs  the attributes to apply
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the modification fails for any other reason
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
   * Modifies the attributes of the entry at the string name {@code name} using the given operation
   * code and attribute set.
   *
   * @param name   the slash-delimited internal name of the entry to modify
   * @param mod_op the modification operation
   * @param attrs  the attributes to apply
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the modification fails for any other reason
   */
  public void modifyAttributes (String name, int mod_op, Attributes attrs)
    throws NamingException {

    modifyAttributes(nameParser.parse(name), mod_op, attrs);
  }

  /**
   * Applies an ordered sequence of modification items to the entry at {@code name}.
   *
   * @param name the internal name of the entry to modify
   * @param mods the ordered modifications to apply
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the modification fails for any other reason
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
   * Applies an ordered sequence of modification items to the entry at the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the entry to modify
   * @param mods the ordered modifications to apply
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the modification fails for any other reason
   */
  public void modifyAttributes (String name, ModificationItem[] mods)
    throws NamingException {

    modifyAttributes(nameParser.parse(name), mods);
  }

  /**
   * Binds {@code obj} to {@code name} in the backing store, associating the given attributes.
   *
   * @param name  the internal name to bind
   * @param obj   the object to bind
   * @param attrs the attributes to associate with the binding
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the bind fails for any other reason
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
   * Binds {@code obj} to the string name {@code name}, associating the given attributes.
   *
   * @param name  the slash-delimited internal name to bind
   * @param obj   the object to bind
   * @param attrs the attributes to associate with the binding
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the bind fails for any other reason
   */
  public void bind (String name, Object obj, Attributes attrs)
    throws NamingException {

    bind(nameParser.parse(name), obj, attrs);
  }

  /**
   * Binds {@code obj} to {@code name}, replacing any existing binding and associating the given
   * attributes.
   *
   * @param name  the internal name to rebind
   * @param obj   the object to bind
   * @param attrs the attributes to associate with the binding
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the rebind fails for any other reason
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
   * Binds {@code obj} to the string name {@code name}, replacing any existing binding and
   * associating the given attributes.
   *
   * @param name  the slash-delimited internal name to rebind
   * @param obj   the object to bind
   * @param attrs the attributes to associate with the binding
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if the rebind fails for any other reason
   */
  public void rebind (String name, Object obj, Attributes attrs)
    throws NamingException {

    rebind(nameParser.parse(name), obj, attrs);
  }

  /**
   * Creates a subcontext with the given name and initial attributes, returning it wrapped in a
   * {@link JavaContext}.
   *
   * @param name  the internal name of the subcontext to create
   * @param attrs the initial attributes to associate with the new subcontext
   * @return a new {@link JavaContext} wrapping the created backing-store subcontext
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if creation fails for any other reason
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
   * Creates a subcontext with the string name {@code name} and initial attributes, returning it
   * wrapped in a {@link JavaContext}.
   *
   * @param name  the slash-delimited internal name of the subcontext to create
   * @param attrs the initial attributes to associate with the new subcontext
   * @return a new {@link JavaContext} wrapping the created backing-store subcontext
   * @throws OperationNotSupportedException if this context is not modifiable
   * @throws NamingException                if creation fails for any other reason
   */
  public DirContext createSubcontext (String name, Attributes attrs)
    throws NamingException {

    return createSubcontext(nameParser.parse(name), attrs);
  }

  /**
   * Returns the schema context associated with the entry at {@code name}.
   *
   * @param name the internal name of the entry
   * @return the schema context from the backing store
   * @throws NamingException if the backing-store operation fails
   */
  public DirContext getSchema (Name name)
    throws NamingException {

    ContextNamePair contextNamePair;

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

    return contextNamePair.getContext().getSchema(contextNamePair.getName());
  }

  /**
   * Returns the schema context associated with the entry at the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the entry
   * @return the schema context from the backing store
   * @throws NamingException if the backing-store operation fails
   */
  public DirContext getSchema (String name)
    throws NamingException {

    return getSchema(nameParser.parse(name));
  }

  /**
   * Returns the schema class definition context for the entry at {@code name}.
   *
   * @param name the internal name of the entry
   * @return the schema class definition context from the backing store
   * @throws NamingException if the backing-store operation fails
   */
  public DirContext getSchemaClassDefinition (Name name)
    throws NamingException {

    ContextNamePair contextNamePair;

    contextNamePair = nameTranslator.fromInternalNameToExternalContext(internalContext, name);

    return contextNamePair.getContext().getSchemaClassDefinition(contextNamePair.getName());
  }

  /**
   * Returns the schema class definition context for the entry at the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the entry
   * @return the schema class definition context from the backing store
   * @throws NamingException if the backing-store operation fails
   */
  public DirContext getSchemaClassDefinition (String name)
    throws NamingException {

    return getSchemaClassDefinition(nameParser.parse(name));
  }

  /**
   * Searches under {@code name} for entries matching {@code matchingAttributes}, returning only
   * the requested attributes.
   *
   * @param name               the internal base name for the search
   * @param matchingAttributes the attributes to match; may be empty
   * @param attributesToReturn the attribute identifiers to include in each result; {@code null}
   *                           returns all attributes
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null} if the
   * backing store returns {@code null}
   * @throws NamingException if the search fails
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
   * Searches under the string name {@code name} for entries matching {@code matchingAttributes},
   * returning only the requested attributes.
   *
   * @param name               the slash-delimited internal base name for the search
   * @param matchingAttributes the attributes to match
   * @param attributesToReturn the attribute identifiers to include in each result
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null}
   * @throws NamingException if the search fails
   */
  public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes, String[] attributesToReturn)
    throws NamingException {

    return search(nameParser.parse(name), matchingAttributes, attributesToReturn);
  }

  /**
   * Searches under {@code name} for entries matching all attributes in {@code matchingAttributes}.
   *
   * @param name               the internal base name for the search
   * @param matchingAttributes the attributes to match
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null} if the
   * backing store returns {@code null}
   * @throws NamingException if the search fails
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
   * Searches under the string name {@code name} for entries matching all attributes in
   * {@code matchingAttributes}.
   *
   * @param name               the slash-delimited internal base name for the search
   * @param matchingAttributes the attributes to match
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null}
   * @throws NamingException if the search fails
   */
  public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes)
    throws NamingException {

    return search(nameParser.parse(name), matchingAttributes);
  }

  /**
   * Searches under {@code name} using the RFC 2254 filter string {@code filter} and the given
   * search controls.
   *
   * @param name   the internal base name for the search
   * @param filter the RFC 2254 search filter string
   * @param cons   the search controls (scope, size limit, time limit, etc.)
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null} if the
   * backing store returns {@code null}
   * @throws NamingException if the search fails
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
   * Searches under the string name {@code name} using the RFC 2254 filter string {@code filter}
   * and the given search controls.
   *
   * @param name   the slash-delimited internal base name for the search
   * @param filter the RFC 2254 search filter string
   * @param cons   the search controls
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null}
   * @throws NamingException if the search fails
   */
  public NamingEnumeration<SearchResult> search (String name, String filter, SearchControls cons)
    throws NamingException {

    return search(nameParser.parse(name), filter, cons);
  }

  /**
   * Searches under {@code name} using a parameterised filter expression and the given search
   * controls.
   *
   * @param name       the internal base name for the search
   * @param filterExpr the RFC 2254 filter expression, which may contain substitution variables
   *                   ({@code {0}}, {@code {1}}, etc.)
   * @param filterArgs the values to substitute into the filter expression
   * @param cons       the search controls
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null} if the
   * backing store returns {@code null}
   * @throws NamingException if the search fails
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
   * Searches under the string name {@code name} using a parameterised filter expression and the
   * given search controls.
   *
   * @param name       the slash-delimited internal base name for the search
   * @param filterExpr the RFC 2254 filter expression with optional substitution variables
   * @param filterArgs the values to substitute into the filter expression
   * @param cons       the search controls
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null}
   * @throws NamingException if the search fails
   */
  public NamingEnumeration<SearchResult> search (String name, String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException {

    return search(nameParser.parse(name), filterExpr, filterArgs, cons);
  }
}
