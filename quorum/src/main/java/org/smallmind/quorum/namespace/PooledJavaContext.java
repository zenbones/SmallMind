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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
   * {@inheritDoc}
   *
   * @throws CommunicationException after firing a context-aborted event if the backing store
   *                                becomes unreachable
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
