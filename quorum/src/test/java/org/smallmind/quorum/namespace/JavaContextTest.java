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

import java.util.Collections;
import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.smallmind.quorum.namespace.NamespaceTestSupport.FakeNamingEnumeration;
import org.smallmind.quorum.namespace.NamespaceTestSupport.RecordingDirContext;
import org.smallmind.quorum.namespace.NamespaceTestSupport.StubNameTranslator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the nested-context path of {@link JavaContext} — the construction shape used when the
 * backing store hands back a child directory context — against a recording {@link DirContext} fake
 * and a name translator that upper-cases each component. The translator turns lower-case internal
 * components into upper-case external ones, so a test can prove the translated (not the raw) name is
 * what reaches the backing context.
 */
@Test(groups = "unit")
public class JavaContextTest {

  private final StubNameTranslator translator = new StubNameTranslator();

  private JavaContext modifiableContext (RecordingDirContext backing) {

    return new JavaContext(new Hashtable<>(), backing, translator, new JavaNameParser(translator), true);
  }

  private JavaContext readOnlyContext (RecordingDirContext backing) {

    return new JavaContext(new Hashtable<>(), backing, translator, new JavaNameParser(translator), false);
  }

  public void testLookupForwardsTheTranslatedNameAndReturnsAPlainBinding ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setLookupResult("a-plain-value");

    Object result = readOnlyContext(backing).lookup("alpha/beta");

    Assert.assertEquals(result, "a-plain-value", "a non-context result should be returned verbatim");
    Assert.assertEquals(backing.getOperations(), Collections.singletonList("lookup"));

    Name forwarded = backing.getLastName();
    Assert.assertEquals(forwarded.size(), 2);
    Assert.assertEquals(forwarded.get(0), "ALPHA", "the upper-cased (translated) component should reach the backing store");
    Assert.assertEquals(forwarded.get(1), "BETA");
  }

  public void testLookupWrapsANestedDirectoryContext ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    // A result whose class matches the backing context's class is treated as a nested context.
    backing.setLookupResult(new RecordingDirContext());

    Object result = readOnlyContext(backing).lookup("child");

    Assert.assertTrue(result instanceof JavaContext, "a nested directory context should be wrapped in a JavaContext");
    Assert.assertFalse(result instanceof PooledJavaContext, "an unpooled context must not wrap children as pooled");
  }

  public void testReadOnlyContextRejectsEveryMutation () {

    RecordingDirContext backing = new RecordingDirContext();
    JavaContext context = readOnlyContext(backing);

    Assert.assertThrows(OperationNotSupportedException.class, () -> context.bind("x", "v"));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.rebind("x", "v"));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.unbind("x"));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.rename("x", "y"));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.destroySubcontext("x"));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.createSubcontext("x"));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.modifyAttributes("x", DirContext.ADD_ATTRIBUTE, new BasicAttributes()));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.modifyAttributes("x", new ModificationItem[0]));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.addToEnvironment("k", "v"));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.removeFromEnvironment("k"));

    Assert.assertTrue(backing.getOperations().isEmpty(), "a rejected mutation must never touch the backing store");
  }

  public void testModifiableContextForwardsMutationsToTheBackingStore ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    JavaContext context = modifiableContext(backing);
    Object bound = new Object();

    context.bind("name", bound);

    Assert.assertEquals(backing.getOperations(), Collections.singletonList("bind"));
    Assert.assertSame(backing.getLastBoundObject(), bound);
    Assert.assertEquals(backing.getLastName().get(0), "NAME", "the translated name should be forwarded");
  }

  public void testRenameForwardsTheTranslatedOldName ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();

    modifiableContext(backing).rename("from", "to");

    Assert.assertEquals(backing.getOperations(), Collections.singletonList("rename"));
    Assert.assertEquals(backing.getLastName().get(0), "FROM");
  }

  public void testCreateSubcontextReturnsAWrappingJavaContext ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();

    Object created = modifiableContext(backing).createSubcontext("child");

    Assert.assertTrue(created instanceof JavaContext, "a created subcontext should be wrapped as a JavaContext");
    Assert.assertEquals(backing.getOperations(), Collections.singletonList("createSubcontext"));
  }

  public void testModifyAttributesForwardsOperationCodeAndAttributes ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    BasicAttributes attributes = new BasicAttributes("cn", "value");

    modifiableContext(backing).modifyAttributes("entry", DirContext.REPLACE_ATTRIBUTE, attributes);

    Assert.assertEquals(backing.getOperations(), Collections.singletonList("modifyAttributes(op)"));
    Assert.assertEquals(backing.getLastModOp(), DirContext.REPLACE_ATTRIBUTE);
    Assert.assertSame(backing.getLastAttributes(), attributes);
  }

  public void testListWrapsTheBackingEnumerationWhenPresent ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(new FakeNamingEnumeration<>(Collections.<NameClassPair>emptyList()));

    NamingEnumeration<NameClassPair> enumeration = readOnlyContext(backing).list("dir");

    Assert.assertTrue(enumeration instanceof JavaNamingEnumeration, "a non-null backing enumeration should be wrapped");
  }

  public void testListReturnsNullWhenTheBackingStoreReturnsNull ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(null);

    Assert.assertNull(readOnlyContext(backing).list("dir"), "a null backing enumeration should pass straight through");
  }

  public void testListBindingsWrapsTheBackingEnumerationWhenPresent ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(new FakeNamingEnumeration<>(Collections.<Binding>emptyList()));

    NamingEnumeration<Binding> enumeration = readOnlyContext(backing).listBindings("dir");

    Assert.assertTrue(enumeration instanceof JavaNamingEnumeration);
  }

  public void testSearchWrapsTheBackingEnumerationWhenPresent ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(new FakeNamingEnumeration<>(Collections.emptyList()));

    Assert.assertTrue(readOnlyContext(backing).search("dir", "(cn=*)", new SearchControls()) instanceof JavaNamingEnumeration);
  }

  public void testGetNameInNamespaceTranslatesTheBackingResult ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();

    // The stub translator lower-cases and prepends java: to the backing namespace string.
    Assert.assertEquals(readOnlyContext(backing).getNameInNamespace(), "java:cn=node,cn=root");
  }

  public void testCloseClosesTheBackingContext ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();

    readOnlyContext(backing).close();

    Assert.assertEquals(backing.getCloseCount(), 1, "closing a nested context should close its backing context");
  }

  public void testTopLevelCloseDoesNotTouchAnyBackingContext ()
    throws Exception {

    // A top-level context holds no backing context, so close must be a silent no-op.
    new JavaContext(translator, new Hashtable<>(), false, false).close();
  }

  public void testComposeNameConcatenatesPrefixAndName ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();

    Assert.assertEquals(readOnlyContext(backing).composeName("c", "a/b"), "a/b/c");
  }

  public void testGetEnvironmentReturnsTheBackingEnvironment ()
    throws Exception {

    Hashtable<String, Object> environment = new Hashtable<>();
    environment.put("k", "v");

    JavaContext context = new JavaContext(environment, new RecordingDirContext(), translator, new JavaNameParser(translator), true);

    Assert.assertSame(context.getEnvironment(), environment);
  }

  public void testModifiableContextEnvironmentMutationsRoundTrip ()
    throws Exception {

    JavaContext context = new JavaContext(new Hashtable<>(), new RecordingDirContext(), translator, new JavaNameParser(translator), true);

    Assert.assertNull(context.addToEnvironment("alpha", "one"), "adding a fresh key returns no previous value");
    Assert.assertEquals(context.addToEnvironment("alpha", "two"), "one", "replacing returns the previous value");
    Assert.assertEquals(context.removeFromEnvironment("alpha"), "two", "removal returns the last value");
  }

  public void testLookupLinkWrapsANestedDirectoryContext ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setLookupResult(new RecordingDirContext());

    Object result = readOnlyContext(backing).lookupLink("child");

    Assert.assertTrue(result instanceof JavaContext, "a directory-context link target should be wrapped");
    Assert.assertEquals(backing.getOperations(), Collections.singletonList("lookupLink"));
    Assert.assertEquals(backing.getLastName().get(0), "CHILD", "the translated name should reach the backing store");
  }

  public void testLookupLinkReturnsAPlainValueVerbatim ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setLookupResult("a-link-value");

    Assert.assertEquals(readOnlyContext(backing).lookupLink("alias"), "a-link-value", "a non-context link target should pass straight through");
  }

  public void testGetSchemaDelegatesToTheBackingStore ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();

    Assert.assertNotNull(readOnlyContext(backing).getSchema("entry"), "the backing schema context should be returned");
    Assert.assertEquals(backing.getOperations(), Collections.singletonList("getSchema"));
    Assert.assertEquals(backing.getLastName().get(0), "ENTRY");
  }

  public void testGetSchemaClassDefinitionDelegatesToTheBackingStore ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();

    Assert.assertNotNull(readOnlyContext(backing).getSchemaClassDefinition("entry"));
    Assert.assertEquals(backing.getOperations(), Collections.singletonList("getSchemaClassDefinition"));
  }

  public void testSearchByMatchingAttributesWrapsTheBackingEnumeration ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(new FakeNamingEnumeration<>(Collections.<SearchResult>emptyList()));

    Assert.assertTrue(readOnlyContext(backing).search("dir", new BasicAttributes()) instanceof JavaNamingEnumeration);
    Assert.assertEquals(backing.getOperations(), Collections.singletonList("search(attrs)"));
  }

  public void testSearchByMatchingAttributesReturnsNullWhenBackingReturnsNull ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(null);

    Assert.assertNull(readOnlyContext(backing).search("dir", new BasicAttributes()), "a null backing enumeration should pass straight through");
  }

  public void testSearchByMatchingAttributesAndIdsWrapsTheBackingEnumeration ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(new FakeNamingEnumeration<>(Collections.<SearchResult>emptyList()));

    Assert.assertTrue(readOnlyContext(backing).search("dir", new BasicAttributes(), new String[] {"cn"}) instanceof JavaNamingEnumeration);
    Assert.assertEquals(backing.getOperations(), Collections.singletonList("search(attrs,ids)"));
  }

  public void testSearchByFilterExpressionWrapsTheBackingEnumeration ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(new FakeNamingEnumeration<>(Collections.<SearchResult>emptyList()));

    Assert.assertTrue(readOnlyContext(backing).search("dir", "(cn={0})", new Object[] {"x"}, new SearchControls()) instanceof JavaNamingEnumeration);
    Assert.assertEquals(backing.getOperations(), Collections.singletonList("search(args)"));
  }

  public void testSearchByFilterReturnsNullWhenBackingReturnsNull ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(null);

    Assert.assertNull(readOnlyContext(backing).search("dir", "(cn=*)", new SearchControls()), "a null filter-search result should pass straight through");
  }

  public void testListBindingsReturnsNullWhenBackingReturnsNull ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    backing.setNextEnumeration(null);

    Assert.assertNull(readOnlyContext(backing).listBindings("dir"));
  }

  public void testGetAttributesDelegatesForBothOverloads ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    JavaContext context = readOnlyContext(backing);

    context.getAttributes("entry");
    context.getAttributes("entry", new String[] {"cn"});

    Assert.assertEquals(backing.getOperations(), java.util.Arrays.asList("getAttributes", "getAttributes(ids)"));
    Assert.assertEquals(backing.getLastName().get(0), "ENTRY", "the translated name should reach the backing store");
  }

  public void testUnbindForwardsTheTranslatedName ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();

    modifiableContext(backing).unbind("victim");

    Assert.assertEquals(backing.getOperations(), Collections.singletonList("unbind"));
    Assert.assertEquals(backing.getLastName().get(0), "VICTIM");
  }

  public void testDestroySubcontextForwardsTheTranslatedName ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();

    modifiableContext(backing).destroySubcontext("gone");

    Assert.assertEquals(backing.getOperations(), Collections.singletonList("destroySubcontext"));
    Assert.assertEquals(backing.getLastName().get(0), "GONE");
  }

  public void testRebindForwardsToTheBackingStore ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    Object bound = new Object();

    modifiableContext(backing).rebind("name", bound);

    Assert.assertEquals(backing.getOperations(), Collections.singletonList("rebind"));
    Assert.assertSame(backing.getLastBoundObject(), bound);
  }

  public void testModifyAttributesByItemsForwardsToTheBackingStore ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    ModificationItem[] items = new ModificationItem[0];

    modifiableContext(backing).modifyAttributes("entry", items);

    Assert.assertEquals(backing.getOperations(), Collections.singletonList("modifyAttributes(items)"));
    Assert.assertSame(backing.getLastModificationItems(), items);
  }

  public void testCreateSubcontextWithAttributesReturnsAWrappingJavaContext ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    BasicAttributes attributes = new BasicAttributes("cn", "child");

    Object created = modifiableContext(backing).createSubcontext("child", attributes);

    Assert.assertTrue(created instanceof JavaContext, "a subcontext created with attributes should be wrapped");
    Assert.assertEquals(backing.getOperations(), Collections.singletonList("createSubcontext(attrs)"));
    Assert.assertSame(backing.getLastAttributes(), attributes);
  }

  public void testBindWithAttributesForwardsToTheBackingStore ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    Object bound = new Object();
    BasicAttributes attributes = new BasicAttributes("cn", "x");

    modifiableContext(backing).bind("name", bound, attributes);

    Assert.assertEquals(backing.getOperations(), Collections.singletonList("bind(attrs)"));
    Assert.assertSame(backing.getLastBoundObject(), bound);
    Assert.assertSame(backing.getLastAttributes(), attributes);
  }

  public void testRebindWithAttributesForwardsToTheBackingStore ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    Object bound = new Object();
    BasicAttributes attributes = new BasicAttributes("cn", "x");

    modifiableContext(backing).rebind("name", bound, attributes);

    Assert.assertEquals(backing.getOperations(), Collections.singletonList("rebind(attrs)"));
    Assert.assertSame(backing.getLastAttributes(), attributes);
  }

  public void testReadOnlyContextRejectsAttributeAwareMutations () {

    RecordingDirContext backing = new RecordingDirContext();
    JavaContext context = readOnlyContext(backing);

    Assert.assertThrows(OperationNotSupportedException.class, () -> context.createSubcontext("x", new BasicAttributes()));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.bind("x", "v", new BasicAttributes()));
    Assert.assertThrows(OperationNotSupportedException.class, () -> context.rebind("x", "v", new BasicAttributes()));

    Assert.assertTrue(backing.getOperations().isEmpty(), "a rejected mutation must never touch the backing store");
  }

  public void testGetNameParserIsReturnedForBothOverloads ()
    throws Exception {

    JavaContext context = readOnlyContext(new RecordingDirContext());
    NameParser parser = context.getNameParser("ignored");

    Assert.assertNotNull(parser);
    Assert.assertSame(context.getNameParser(new JavaName(translator)), parser, "the same parser should back both overloads");
  }

  public void testComposeNameViaTheNameOverloadConcatenates ()
    throws Exception {

    JavaContext context = readOnlyContext(new RecordingDirContext());
    NameParser parser = context.getNameParser("ignored");
    Name composed = context.composeName(parser.parse("c"), parser.parse("a/b"));

    Assert.assertEquals(composed.size(), 3, "composing prefix a/b with name c should yield three components");
    Assert.assertEquals(composed.get(0), "a");
    Assert.assertEquals(composed.get(1), "b");
    Assert.assertEquals(composed.get(2), "c");
  }
}
