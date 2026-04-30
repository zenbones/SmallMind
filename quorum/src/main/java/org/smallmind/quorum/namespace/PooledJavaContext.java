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
import java.util.Iterator;
import javax.naming.Binding;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.quorum.namespace.backingStore.NameTranslator;
import org.smallmind.quorum.namespace.event.JavaContextEvent;
import org.smallmind.quorum.namespace.event.JavaContextListener;

/**
 * {@link JavaContext} subclass that adds lifecycle event notification for use in connection pools.
 * <p>
 * Every {@link DirContext} method is overridden to catch {@link CommunicationException}s. When one
 * is detected the method fires a {@link JavaContextListener#contextAborted} event to all registered
 * listeners (typically a {@link org.smallmind.quorum.namespace.pool.JavaContextComponentInstance}
 * that will remove the broken context from the pool), then re-throws the exception.
 * <p>
 * The two {@link #close} overloads differ in intent:
 * <ul>
 *   <li>{@link #close()} — logically releases the context back to the pool by firing a
 *       {@link JavaContextListener#contextClosed} event without physically closing the connection.</li>
 *   <li>{@link #close(boolean) close(true)} — physically closes the underlying backing context,
 *       used by the pool when permanently retiring the connection.</li>
 * </ul>
 * Listeners are held via a {@link WeakEventListenerList} so that garbage-collected listeners are
 * silently skipped.
 */
public class PooledJavaContext extends JavaContext {

  private final WeakEventListenerList<JavaContextListener> listenerList;

  /**
   * Creates a pooled context wrapping an existing backing-store {@link DirContext}.
   *
   * @param environment     the JNDI environment for this context
   * @param internalContext the backing directory context to wrap
   * @param nameTranslator  the translator used to convert between internal and external name forms
   * @param nameParser      the name parser used to parse string names
   * @param modifiable      {@code true} to allow mutations on the backing store
   */
  protected PooledJavaContext (Hashtable<String, Object> environment, DirContext internalContext, NameTranslator nameTranslator, JavaNameParser nameParser, boolean modifiable) {

    super(environment, internalContext, nameTranslator, nameParser, modifiable);

    listenerList = new WeakEventListenerList<JavaContextListener>();
  }

  /**
   * Registers a listener to receive context lifecycle events from this context.
   *
   * @param listener the listener to add; held weakly so garbage collection silently removes it
   */
  public void addJavaContextListener (JavaContextListener listener) {

    synchronized (listenerList) {
      listenerList.addListener(listener);
    }
  }

  /**
   * Removes a previously registered lifecycle listener from this context.
   *
   * @param listener the listener to remove
   */
  public void removeJavaContextListener (JavaContextListener listener) {

    synchronized (listenerList) {
      listenerList.removeListener(listener);
    }
  }

  /**
   * Looks up the object bound to {@code name}.
   * <p>
   * If the backing store returns a directory context of the same class as the current context,
   * it is wrapped in a new {@link JavaContext} (or {@link PooledJavaContext} when pooling is
   * enabled).
   *
   * @param name the internal name to look up
   * @return the bound object, or a {@link JavaContext} wrapping a nested directory context
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the name is not bound or the backing store lookup fails
   */
  public Object lookup (Name name)
    throws NamingException {

    try {
      return super.lookup(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Looks up the object bound to the string name {@code name}.
   *
   * @param name the slash-delimited internal name to look up
   * @return the bound object, or a {@link JavaContext} wrapping a nested directory context
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the name is not bound or the backing store lookup fails
   */
  public Object lookup (String name)
    throws NamingException {

    try {
      return super.lookup(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Binds {@code obj} to {@code name} in the backing store.
   *
   * @param name the internal name to bind
   * @param obj  the object to bind
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the bind fails for any other reason
   */
  public void bind (Name name, Object obj)
    throws NamingException {

    try {
      super.bind(name, obj);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Binds {@code obj} to the string name {@code name} in the backing store.
   *
   * @param name the slash-delimited internal name to bind
   * @param obj  the object to bind
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the bind fails for any other reason
   */
  public void bind (String name, Object obj)
    throws NamingException {

    try {
      super.bind(name, obj);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Binds {@code obj} to {@code name}, replacing any existing binding.
   *
   * @param name the internal name to rebind
   * @param obj  the object to bind
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the rebind fails for any other reason
   */
  public void rebind (Name name, Object obj)
    throws NamingException {

    try {
      super.rebind(name, obj);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Binds {@code obj} to the string name {@code name}, replacing any existing binding.
   *
   * @param name the slash-delimited internal name to rebind
   * @param obj  the object to bind
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the rebind fails for any other reason
   */
  public void rebind (String name, Object obj)
    throws NamingException {

    try {
      super.rebind(name, obj);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Removes the binding for {@code name} from the backing store.
   *
   * @param name the internal name whose binding is to be removed
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the unbind fails for any other reason
   */
  public void unbind (Name name)
    throws NamingException {

    try {
      super.unbind(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Removes the binding for the string name {@code name} from the backing store.
   *
   * @param name the slash-delimited internal name whose binding is to be removed
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the unbind fails for any other reason
   */
  public void unbind (String name)
    throws NamingException {

    try {
      super.unbind(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Renames the entry at {@code oldName} to {@code newName} in the backing store.
   *
   * @param oldName the current internal name of the entry
   * @param newName the new internal name for the entry
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the rename fails for any other reason
   */
  public void rename (Name oldName, Name newName)
    throws NamingException {

    try {
      super.rename(oldName, newName);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Renames the entry at the string name {@code oldName} to the string name {@code newName}.
   *
   * @param oldName the slash-delimited current internal name
   * @param newName the slash-delimited new internal name
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the rename fails for any other reason
   */
  public void rename (String oldName, String newName)
    throws NamingException {

    try {
      super.rename(oldName, newName);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
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
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the list operation fails
   */
  public NamingEnumeration<NameClassPair> list (Name name)
    throws NamingException {

    try {
      return super.list(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns an enumeration of the names and class names of all bindings under the string name
   * {@code name}.
   *
   * @param name the slash-delimited internal name of the context to list
   * @return a {@link JavaNamingEnumeration} of {@link NameClassPair}s, or {@code null}
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the list operation fails
   */
  public NamingEnumeration<NameClassPair> list (String name)
    throws NamingException {

    try {
      return super.list(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
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
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the list-bindings operation fails
   */
  public NamingEnumeration<Binding> listBindings (Name name)
    throws NamingException {

    try {
      return super.listBindings(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns an enumeration of the name-object bindings under the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the context whose bindings to list
   * @return a {@link JavaNamingEnumeration} of {@link Binding}s, or {@code null}
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the list-bindings operation fails
   */
  public NamingEnumeration<Binding> listBindings (String name)
    throws NamingException {

    try {
      return super.listBindings(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Destroys the subcontext identified by {@code name}.
   *
   * @param name the internal name of the subcontext to destroy
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if destruction fails for any other reason
   */
  public void destroySubcontext (Name name)
    throws NamingException {

    try {
      super.destroySubcontext(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Destroys the subcontext identified by the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the subcontext to destroy
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if destruction fails for any other reason
   */
  public void destroySubcontext (String name)
    throws NamingException {

    try {
      super.destroySubcontext(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Creates a new subcontext with the given name and returns it wrapped in a
   * {@link JavaContext}.
   *
   * @param name the internal name of the subcontext to create
   * @return a new {@link JavaContext} wrapping the created backing-store subcontext
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if creation fails for any other reason
   */
  public Context createSubcontext (Name name)
    throws NamingException {

    try {
      return super.createSubcontext(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Creates a new subcontext with the string name {@code name} and returns it wrapped in a
   * {@link JavaContext}.
   *
   * @param name the slash-delimited internal name of the subcontext to create
   * @return a new {@link JavaContext} wrapping the created backing-store subcontext
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if creation fails for any other reason
   */
  public Context createSubcontext (String name)
    throws NamingException {

    try {
      return super.createSubcontext(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Looks up the object bound to {@code name} without following link references.
   * <p>
   * If the result is a directory context it is wrapped in a new {@link JavaContext}.
   *
   * @param name the internal name to look up
   * @return the bound object or a {@link JavaContext} wrapping a nested directory context
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the lookup fails
   */
  public Object lookupLink (Name name)
    throws NamingException {

    try {
      return super.lookupLink(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Looks up the object bound to the string name {@code name} without following link references.
   *
   * @param name the slash-delimited internal name to look up
   * @return the bound object or a {@link JavaContext} wrapping a nested directory context
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the lookup fails
   */
  public Object lookupLink (String name)
    throws NamingException {

    try {
      return super.lookupLink(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns the {@link NameParser} associated with this context.
   * <p>
   * The {@code name} parameter is accepted for API compatibility but is not used.
   *
   * @param name not used
   * @return the {@link JavaNameParser} held by this context
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        never thrown by this implementation
   */
  public NameParser getNameParser (Name name)
    throws NamingException {

    try {
      return super.getNameParser(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns the {@link NameParser} associated with this context.
   * <p>
   * The {@code name} parameter is accepted for API compatibility but is not used.
   *
   * @param name not used
   * @return the {@link JavaNameParser} held by this context
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        never thrown by this implementation
   */
  public NameParser getNameParser (String name)
    throws NamingException {

    try {
      return super.getNameParser(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Composes two names by appending {@code name} to a clone of {@code prefix}.
   *
   * @param name   the name to append
   * @param prefix the prefix to prepend
   * @return a new {@link Name} equal to {@code prefix + name}
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if composition fails
   */
  public Name composeName (Name name, Name prefix)
    throws NamingException {

    try {
      return super.composeName(name, prefix);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Composes two string names and returns the result as a slash-delimited string.
   *
   * @param name   the name portion to append
   * @param prefix the prefix portion to prepend
   * @return the composed name string
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if parsing or composition fails
   */
  public String composeName (String name, String prefix)
    throws NamingException {

    try {
      return super.composeName(name, prefix);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Logically closes the context by firing a {@link JavaContextListener#contextClosed} event,
   * which causes the owning pool component instance to return this context to the pool.
   * The underlying backing-store connection is not physically closed.
   *
   * @throws NamingException never thrown by this implementation
   */
  public void close ()
    throws NamingException {

    close(false);
  }

  /**
   * Closes the context either physically or logically depending on {@code forced}.
   * <p>
   * When {@code forced} is {@code true}, the parent {@link JavaContext#close()} is called to
   * release the backing-store connection. When {@code false}, a context-closed event is fired to
   * notify the pool that the context is available for re-use.
   *
   * @param forced {@code true} to physically close the underlying connection;
   *               {@code false} to fire a context-closed event and return to the pool
   * @throws NamingException if a forced close of the backing-store context throws
   */
  public void close (boolean forced)
    throws NamingException {

    if (forced) {
      super.close();
    } else {
      fireContextClosed(new JavaContextEvent(this));
    }
  }

  /**
   * Returns the fully qualified name of this context in the {@code java:} namespace.
   * <p>
   * Delegates to the backing context's {@link DirContext#getNameInNamespace()} and converts the
   * result from its external form back to the internal form using the name translator.
   *
   * @return the internal qualified name string
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the backing context's {@code getNameInNamespace} throws or if the
   *                                returned string cannot be translated
   */
  public String getNameInNamespace ()
    throws NamingException {

    try {
      return super.getNameInNamespace();
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns all attributes associated with the entry at {@code name}.
   *
   * @param name the internal name of the entry
   * @return all attributes for the entry
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the backing-store operation fails
   */
  public Attributes getAttributes (Name name)
    throws NamingException {

    try {
      return super.getAttributes(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns all attributes associated with the entry at the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the entry
   * @return all attributes for the entry
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the backing-store operation fails
   */
  public Attributes getAttributes (String name)
    throws NamingException {

    try {
      return super.getAttributes(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns the specified attributes associated with the entry at {@code name}.
   *
   * @param name    the internal name of the entry
   * @param attrIds the identifiers of the attributes to return; {@code null} returns all
   *                attributes
   * @return the requested attributes for the entry
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the backing-store operation fails
   */
  public Attributes getAttributes (Name name, String[] attrIds)
    throws NamingException {

    try {
      return super.getAttributes(name, attrIds);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns the specified attributes associated with the entry at the string name {@code name}.
   *
   * @param name    the slash-delimited internal name of the entry
   * @param attrIds the identifiers of the attributes to return
   * @return the requested attributes for the entry
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the backing-store operation fails
   */
  public Attributes getAttributes (String name, String[] attrIds)
    throws NamingException {

    try {
      return super.getAttributes(name, attrIds);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Modifies the attributes of the entry at {@code name} using the given operation code and
   * attribute set.
   *
   * @param name   the internal name of the entry to modify
   * @param mod_op the modification operation ({@link DirContext#ADD_ATTRIBUTE},
   *               {@link DirContext#REPLACE_ATTRIBUTE}, or {@link DirContext#REMOVE_ATTRIBUTE})
   * @param attrs  the attributes to apply
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the modification fails for any other reason
   */
  public void modifyAttributes (Name name, int mod_op, Attributes attrs)
    throws NamingException {

    try {
      super.modifyAttributes(name, mod_op, attrs);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Modifies the attributes of the entry at the string name {@code name} using the given
   * operation code and attribute set.
   *
   * @param name   the slash-delimited internal name of the entry to modify
   * @param mod_op the modification operation
   * @param attrs  the attributes to apply
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the modification fails for any other reason
   */
  public void modifyAttributes (String name, int mod_op, Attributes attrs)
    throws NamingException {

    try {
      super.modifyAttributes(name, mod_op, attrs);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Applies an ordered sequence of modification items to the entry at {@code name}.
   *
   * @param name the internal name of the entry to modify
   * @param mods the ordered modifications to apply
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the modification fails for any other reason
   */
  public void modifyAttributes (Name name, ModificationItem[] mods)
    throws NamingException {

    try {
      super.modifyAttributes(name, mods);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Applies an ordered sequence of modification items to the entry at the string name
   * {@code name}.
   *
   * @param name the slash-delimited internal name of the entry to modify
   * @param mods the ordered modifications to apply
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the modification fails for any other reason
   */
  public void modifyAttributes (String name, ModificationItem[] mods)
    throws NamingException {

    try {
      super.modifyAttributes(name, mods);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Binds {@code obj} to {@code name} in the backing store, associating the given attributes.
   *
   * @param name  the internal name to bind
   * @param obj   the object to bind
   * @param attrs the attributes to associate with the binding
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the bind fails for any other reason
   */
  public void bind (Name name, Object obj, Attributes attrs)
    throws NamingException {

    try {
      super.bind(name, obj, attrs);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Binds {@code obj} to the string name {@code name}, associating the given attributes.
   *
   * @param name  the slash-delimited internal name to bind
   * @param obj   the object to bind
   * @param attrs the attributes to associate with the binding
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the bind fails for any other reason
   */
  public void bind (String name, Object obj, Attributes attrs)
    throws NamingException {

    try {
      super.bind(name, obj, attrs);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Binds {@code obj} to {@code name}, replacing any existing binding and associating the given
   * attributes.
   *
   * @param name  the internal name to rebind
   * @param obj   the object to bind
   * @param attrs the attributes to associate with the binding
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the rebind fails for any other reason
   */
  public void rebind (Name name, Object obj, Attributes attrs)
    throws NamingException {

    try {
      super.rebind(name, obj, attrs);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Binds {@code obj} to the string name {@code name}, replacing any existing binding and
   * associating the given attributes.
   *
   * @param name  the slash-delimited internal name to rebind
   * @param obj   the object to bind
   * @param attrs the attributes to associate with the binding
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the rebind fails for any other reason
   */
  public void rebind (String name, Object obj, Attributes attrs)
    throws NamingException {

    try {
      super.rebind(name, obj, attrs);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Creates a subcontext with the given name and initial attributes, returning it wrapped in a
   * {@link JavaContext}.
   *
   * @param name  the internal name of the subcontext to create
   * @param attrs the initial attributes to associate with the new subcontext
   * @return a new {@link JavaContext} wrapping the created backing-store subcontext
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if creation fails for any other reason
   */
  public DirContext createSubcontext (Name name, Attributes attrs)
    throws NamingException {

    try {
      return super.createSubcontext(name, attrs);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Creates a subcontext with the string name {@code name} and initial attributes, returning it
   * wrapped in a {@link JavaContext}.
   *
   * @param name  the slash-delimited internal name of the subcontext to create
   * @param attrs the initial attributes to associate with the new subcontext
   * @return a new {@link JavaContext} wrapping the created backing-store subcontext
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if creation fails for any other reason
   */
  public DirContext createSubcontext (String name, Attributes attrs)
    throws NamingException {

    try {
      return super.createSubcontext(name, attrs);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns the schema context associated with the entry at {@code name}.
   *
   * @param name the internal name of the entry
   * @return the schema context from the backing store
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the backing-store operation fails
   */
  public DirContext getSchema (Name name)
    throws NamingException {

    try {
      return super.getSchema(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns the schema context associated with the entry at the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the entry
   * @return the schema context from the backing store
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the backing-store operation fails
   */
  public DirContext getSchema (String name)
    throws NamingException {

    try {
      return super.getSchema(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns the schema class definition context for the entry at {@code name}.
   *
   * @param name the internal name of the entry
   * @return the schema class definition context from the backing store
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the backing-store operation fails
   */
  public DirContext getSchemaClassDefinition (Name name)
    throws NamingException {

    try {
      return super.getSchemaClassDefinition(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Returns the schema class definition context for the entry at the string name {@code name}.
   *
   * @param name the slash-delimited internal name of the entry
   * @return the schema class definition context from the backing store
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the backing-store operation fails
   */
  public DirContext getSchemaClassDefinition (String name)
    throws NamingException {

    try {
      return super.getSchemaClassDefinition(name);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
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
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the search fails
   */
  public NamingEnumeration<SearchResult> search (Name name, Attributes matchingAttributes, String[] attributesToReturn)
    throws NamingException {

    try {
      return super.search(name, matchingAttributes, attributesToReturn);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Searches under the string name {@code name} for entries matching {@code matchingAttributes},
   * returning only the requested attributes.
   *
   * @param name               the slash-delimited internal base name for the search
   * @param matchingAttributes the attributes to match
   * @param attributesToReturn the attribute identifiers to include in each result
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null}
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the search fails
   */
  public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes, String[] attributesToReturn)
    throws NamingException {

    try {
      return super.search(name, matchingAttributes, attributesToReturn);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Searches under {@code name} for entries matching all attributes in
   * {@code matchingAttributes}.
   *
   * @param name               the internal base name for the search
   * @param matchingAttributes the attributes to match
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null} if the
   * backing store returns {@code null}
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the search fails
   */
  public NamingEnumeration<SearchResult> search (Name name, Attributes matchingAttributes)
    throws NamingException {

    try {
      return super.search(name, matchingAttributes);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Searches under the string name {@code name} for entries matching all attributes in
   * {@code matchingAttributes}.
   *
   * @param name               the slash-delimited internal base name for the search
   * @param matchingAttributes the attributes to match
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null}
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the search fails
   */
  public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes)
    throws NamingException {

    try {
      return super.search(name, matchingAttributes);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
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
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the search fails
   */
  public NamingEnumeration<SearchResult> search (Name name, String filter, SearchControls cons)
    throws NamingException {

    try {
      return super.search(name, filter, cons);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Searches under the string name {@code name} using the RFC 2254 filter string {@code filter}
   * and the given search controls.
   *
   * @param name   the slash-delimited internal base name for the search
   * @param filter the RFC 2254 search filter string
   * @param cons   the search controls
   * @return a {@link JavaNamingEnumeration} of {@link SearchResult}s, or {@code null}
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the search fails
   */
  public NamingEnumeration<SearchResult> search (String name, String filter, SearchControls cons)
    throws NamingException {

    try {
      return super.search(name, filter, cons);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
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
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the search fails
   */
  public NamingEnumeration<SearchResult> search (Name name, String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException {

    try {
      return super.search(name, filterExpr, filterArgs, cons);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
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
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
   * @throws NamingException        if the search fails
   */
  public NamingEnumeration<SearchResult> search (String name, String filterExpr, Object[] filterArgs, SearchControls cons)
    throws NamingException {

    try {
      return super.search(name, filterExpr, filterArgs, cons);
    } catch (CommunicationException communicationException) {
      fireContextAborted(new JavaContextEvent(this, communicationException));
      throw communicationException;
    }
  }

  /**
   * Notifies all registered listeners that this context has been logically closed.
   * <p>
   * Each listener's {@link JavaContextListener#contextClosed} method is called in iteration order.
   * Weakly-referenced listeners that have been garbage collected are silently skipped.
   *
   * @param javaContextEvent the event describing the closure
   */
  public void fireContextClosed (JavaContextEvent javaContextEvent) {

    Iterator<JavaContextListener> listenerIter = listenerList.getListeners();

    while (listenerIter.hasNext()) {
      listenerIter.next().contextClosed(javaContextEvent);
    }
  }

  /**
   * Notifies all registered listeners that this context has been aborted due to a communication
   * failure.
   * <p>
   * Each listener's {@link JavaContextListener#contextAborted} method is called in iteration order.
   * Weakly-referenced listeners that have been garbage collected are silently skipped.
   *
   * @param javaContextEvent the event carrying the originating {@link CommunicationException}
   */
  public void fireContextAborted (JavaContextEvent javaContextEvent) {

    Iterator<JavaContextListener> listenerIter = listenerList.getListeners();

    while (listenerIter.hasNext()) {
      listenerIter.next().contextAborted(javaContextEvent);
    }
  }
}
