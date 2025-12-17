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
 * {@link JavaContext} variant that emits events when connections are closed or aborted, primarily
 * for pooled usage where clients must be notified to remove broken contexts from the pool.
 */
public class PooledJavaContext extends JavaContext {

  private final WeakEventListenerList<JavaContextListener> listenerList;

  /**
   * Constructs a pooled context wrapper.
   *
   * @param environment     JNDI environment
   * @param internalContext backing directory context
   * @param nameTranslator  translator for names
   * @param nameParser      name parser
   * @param modifiable      whether the backing store is modifiable
   */
  protected PooledJavaContext (Hashtable<String, Object> environment, DirContext internalContext, NameTranslator nameTranslator, JavaNameParser nameParser, boolean modifiable) {

    super(environment, internalContext, nameTranslator, nameParser, modifiable);

    listenerList = new WeakEventListenerList<JavaContextListener>();
  }

  /**
   * Registers a listener for context lifecycle events.
   *
   * @param listener listener to add
   */
  public void addJavaContextListener (JavaContextListener listener) {

    synchronized (listenerList) {
      listenerList.addListener(listener);
    }
  }

  /**
   * Removes a previously registered listener.
   *
   * @param listener listener to remove
   */
  public void removeJavaContextListener (JavaContextListener listener) {

    synchronized (listenerList) {
      listenerList.removeListener(listener);
    }
  }

  /**
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * Closes the context logically, notifying listeners instead of closing immediately.
   */
  public void close ()
    throws NamingException {

    close(false);
  }

  /**
   * Closes the context, optionally forcing underlying closure or notifying listeners.
   *
   * @param forced {@code true} to close immediately, {@code false} to notify listeners
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * {@inheritDoc} Triggers {@link #fireContextAborted(JavaContextEvent)} on communication errors.
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
   * Fires a context-closed event to listeners.
   *
   * @param javaContextEvent event describing the closure
   */
  public void fireContextClosed (JavaContextEvent javaContextEvent) {

    Iterator<JavaContextListener> listenerIter = listenerList.getListeners();

    while (listenerIter.hasNext()) {
      listenerIter.next().contextClosed(javaContextEvent);
    }
  }

  /**
   * Fires a context-aborted event to listeners.
   *
   * @param javaContextEvent event describing the abort
   */
  public void fireContextAborted (JavaContextEvent javaContextEvent) {

    Iterator<JavaContextListener> listenerIter = listenerList.getListeners();

    while (listenerIter.hasNext()) {
      listenerIter.next().contextAborted(javaContextEvent);
    }
  }
}
