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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InvalidNameException;
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
import org.smallmind.quorum.namespace.backingStore.NameTranslator;

/**
 * Shared test fakes for the {@code java:} namespace unit tests. Not a test class itself — it carries
 * no {@code @Test} methods, so TestNG ignores it.
 */
final class NamespaceTestSupport {

  private NamespaceTestSupport () {

  }

  /**
   * A {@link NameTranslator} that needs no backing connection. {@code fromInternalNameToExternalName}
   * upper-cases each component so a test can prove the translated (not the raw) name reached the
   * backing context; the string conversions are simple, reversible transforms.
   */
  static class StubNameTranslator extends NameTranslator {

    StubNameTranslator () {

      super(null);
    }

    @Override
    public JavaName fromInternalNameToExternalName (Name internalName)
      throws InvalidNameException {

      JavaName translatedName = new JavaName(this);

      for (int index = 0; index < internalName.size(); index++) {
        translatedName.add(((String)internalName.get(index)).toUpperCase());
      }

      return translatedName;
    }

    @Override
    public String fromExternalNameToExternalString (JavaName externalName) {

      StringBuilder builder = new StringBuilder();

      for (int index = 0; index < externalName.size(); index++) {
        if (index > 0) {
          builder.append(',');
        }
        builder.append(externalName.get(index));
      }

      return builder.toString();
    }

    @Override
    public String fromAbsoluteExternalStringToInternalString (String externalName) {

      return "java:" + externalName.toLowerCase();
    }

    @Override
    public String fromExternalStringToInternalString (String externalName) {

      return externalName.toLowerCase();
    }
  }

  /**
   * A {@link DirContext} that records the {@link Name}-typed call it last received and returns
   * programmable results. Only the {@code Name} overloads exercised by {@link JavaContext} are
   * functional; every other method throws {@link UnsupportedOperationException}.
   */
  static class RecordingDirContext implements DirContext {

    private final List<String> operations = new ArrayList<>();
    private final Hashtable<String, Object> environment = new Hashtable<>();
    private Name lastName;
    private Object lastBoundObject;
    private Attributes lastAttributes;
    private ModificationItem[] lastModificationItems;
    private NamingEnumeration<?> nextEnumeration;
    private Object lookupResult = "bound-object";
    private String nameInNamespace = "cn=node,cn=root";
    private int modOp = -1;
    private int closeCount = 0;

    List<String> getOperations () {

      return operations;
    }

    Name getLastName () {

      return lastName;
    }

    Object getLastBoundObject () {

      return lastBoundObject;
    }

    Attributes getLastAttributes () {

      return lastAttributes;
    }

    ModificationItem[] getLastModificationItems () {

      return lastModificationItems;
    }

    int getLastModOp () {

      return modOp;
    }

    int getCloseCount () {

      return closeCount;
    }

    void setLookupResult (Object lookupResult) {

      this.lookupResult = lookupResult;
    }

    void setNextEnumeration (NamingEnumeration<?> nextEnumeration) {

      this.nextEnumeration = nextEnumeration;
    }

    @Override
    public Object lookup (Name name)
      throws NamingException {

      operations.add("lookup");
      lastName = name;

      return lookupResult;
    }

    @Override
    public void bind (Name name, Object obj) {

      operations.add("bind");
      lastName = name;
      lastBoundObject = obj;
    }

    @Override
    public void rebind (Name name, Object obj) {

      operations.add("rebind");
      lastName = name;
      lastBoundObject = obj;
    }

    @Override
    public void unbind (Name name) {

      operations.add("unbind");
      lastName = name;
    }

    @Override
    public void rename (Name oldName, Name newName) {

      operations.add("rename");
      lastName = oldName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NamingEnumeration<NameClassPair> list (Name name) {

      operations.add("list");
      lastName = name;

      return (NamingEnumeration<NameClassPair>)nextEnumeration;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NamingEnumeration<Binding> listBindings (Name name) {

      operations.add("listBindings");
      lastName = name;

      return (NamingEnumeration<Binding>)nextEnumeration;
    }

    @Override
    public void destroySubcontext (Name name) {

      operations.add("destroySubcontext");
      lastName = name;
    }

    @Override
    public Context createSubcontext (Name name) {

      operations.add("createSubcontext");
      lastName = name;

      return new RecordingDirContext();
    }

    @Override
    public DirContext createSubcontext (Name name, Attributes attrs) {

      operations.add("createSubcontext(attrs)");
      lastName = name;
      lastAttributes = attrs;

      return new RecordingDirContext();
    }

    @Override
    public Attributes getAttributes (Name name) {

      operations.add("getAttributes");
      lastName = name;

      return lastAttributes;
    }

    @Override
    public Attributes getAttributes (Name name, String[] attrIds) {

      operations.add("getAttributes(ids)");
      lastName = name;

      return lastAttributes;
    }

    @Override
    public void modifyAttributes (Name name, int mod_op, Attributes attrs) {

      operations.add("modifyAttributes(op)");
      lastName = name;
      modOp = mod_op;
      lastAttributes = attrs;
    }

    @Override
    public void modifyAttributes (Name name, ModificationItem[] mods) {

      operations.add("modifyAttributes(items)");
      lastName = name;
      lastModificationItems = mods;
    }

    @Override
    public void bind (Name name, Object obj, Attributes attrs) {

      operations.add("bind(attrs)");
      lastName = name;
      lastBoundObject = obj;
      lastAttributes = attrs;
    }

    @Override
    public void rebind (Name name, Object obj, Attributes attrs) {

      operations.add("rebind(attrs)");
      lastName = name;
      lastBoundObject = obj;
      lastAttributes = attrs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NamingEnumeration<SearchResult> search (Name name, String filter, SearchControls cons) {

      operations.add("search");
      lastName = name;

      return (NamingEnumeration<SearchResult>)nextEnumeration;
    }

    @Override
    public String getNameInNamespace () {

      return nameInNamespace;
    }

    @Override
    public void close () {

      closeCount++;
    }

    // Unused portions of the DirContext contract.

    @Override
    public Object lookup (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public void bind (String name, Object obj) {

      throw new UnsupportedOperationException();
    }

    @Override
    public void rebind (String name, Object obj) {

      throw new UnsupportedOperationException();
    }

    @Override
    public void unbind (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public void rename (String oldName, String newName) {

      throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<NameClassPair> list (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<Binding> listBindings (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public void destroySubcontext (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public Context createSubcontext (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public Object lookupLink (Name name) {

      operations.add("lookupLink");
      lastName = name;

      return lookupResult;
    }

    @Override
    public Object lookupLink (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public NameParser getNameParser (Name name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public NameParser getNameParser (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public Name composeName (Name name, Name prefix) {

      throw new UnsupportedOperationException();
    }

    @Override
    public String composeName (String name, String prefix) {

      throw new UnsupportedOperationException();
    }

    @Override
    public Object addToEnvironment (String propName, Object propVal) {

      return environment.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment (String propName) {

      return environment.remove(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment () {

      return environment;
    }

    @Override
    public Attributes getAttributes (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public Attributes getAttributes (String name, String[] attrIds) {

      throw new UnsupportedOperationException();
    }

    @Override
    public void modifyAttributes (String name, int mod_op, Attributes attrs) {

      throw new UnsupportedOperationException();
    }

    @Override
    public void modifyAttributes (String name, ModificationItem[] mods) {

      throw new UnsupportedOperationException();
    }

    @Override
    public void bind (String name, Object obj, Attributes attrs) {

      throw new UnsupportedOperationException();
    }

    @Override
    public void rebind (String name, Object obj, Attributes attrs) {

      throw new UnsupportedOperationException();
    }

    @Override
    public DirContext createSubcontext (String name, Attributes attrs) {

      throw new UnsupportedOperationException();
    }

    @Override
    public DirContext getSchema (Name name) {

      operations.add("getSchema");
      lastName = name;

      return new RecordingDirContext();
    }

    @Override
    public DirContext getSchema (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    public DirContext getSchemaClassDefinition (Name name) {

      operations.add("getSchemaClassDefinition");
      lastName = name;

      return new RecordingDirContext();
    }

    @Override
    public DirContext getSchemaClassDefinition (String name) {

      throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NamingEnumeration<SearchResult> search (Name name, Attributes matchingAttributes, String[] attributesToReturn) {

      operations.add("search(attrs,ids)");
      lastName = name;
      lastAttributes = matchingAttributes;

      return (NamingEnumeration<SearchResult>)nextEnumeration;
    }

    @Override
    public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes, String[] attributesToReturn) {

      throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NamingEnumeration<SearchResult> search (Name name, Attributes matchingAttributes) {

      operations.add("search(attrs)");
      lastName = name;
      lastAttributes = matchingAttributes;

      return (NamingEnumeration<SearchResult>)nextEnumeration;
    }

    @Override
    public NamingEnumeration<SearchResult> search (String name, Attributes matchingAttributes) {

      throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<SearchResult> search (String name, String filter, SearchControls cons) {

      throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NamingEnumeration<SearchResult> search (Name name, String filterExpr, Object[] filterArgs, SearchControls cons) {

      operations.add("search(args)");
      lastName = name;

      return (NamingEnumeration<SearchResult>)nextEnumeration;
    }

    @Override
    public NamingEnumeration<SearchResult> search (String name, String filterExpr, Object[] filterArgs, SearchControls cons) {

      throw new UnsupportedOperationException();
    }
  }

  /**
   * A backing-store {@link NamingEnumeration} over a fixed list, optionally programmed to throw a
   * {@link NamingException} from {@link #hasMore()} so the wrapping enumeration's swallow/translate
   * paths can be exercised.
   */
  static class FakeNamingEnumeration<T> implements NamingEnumeration<T> {

    private final Iterator<T> iterator;
    private final NamingException throwOnHasMore;
    private boolean closed = false;

    FakeNamingEnumeration (List<T> elements) {

      this(elements, null);
    }

    FakeNamingEnumeration (List<T> elements, NamingException throwOnHasMore) {

      this.iterator = elements.iterator();
      this.throwOnHasMore = throwOnHasMore;
    }

    boolean isClosed () {

      return closed;
    }

    @Override
    public T next () {

      return iterator.next();
    }

    @Override
    public boolean hasMore ()
      throws NamingException {

      if (throwOnHasMore != null) {
        throw throwOnHasMore;
      }

      return iterator.hasNext();
    }

    @Override
    public boolean hasMoreElements () {

      return iterator.hasNext();
    }

    @Override
    public T nextElement () {

      return iterator.next();
    }

    @Override
    public void close () {

      closed = true;
    }
  }
}
