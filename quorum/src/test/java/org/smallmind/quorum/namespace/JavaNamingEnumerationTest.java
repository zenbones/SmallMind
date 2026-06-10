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
import java.util.NoSuchElementException;
import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import org.smallmind.quorum.namespace.NamespaceTestSupport.FakeNamingEnumeration;
import org.smallmind.quorum.namespace.NamespaceTestSupport.RecordingDirContext;
import org.smallmind.quorum.namespace.NamespaceTestSupport.StubNameTranslator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the reflective element-rebuilding in {@link JavaNamingEnumeration}: each backing-store
 * element is reconstructed in translated form, with its name lower-cased by the stub translator and
 * any backing directory-context class name rewritten to {@link JavaContext}. Also covers the
 * exception-bridging contract of the legacy {@code hasMoreElements}/{@code nextElement} pair.
 */
@Test(groups = "unit")
public class JavaNamingEnumerationTest {

  private final StubNameTranslator translator = new StubNameTranslator();

  private <T> JavaNamingEnumeration<T> wrap (Class<T> typeClass, FakeNamingEnumeration<T> backing) {

    return new JavaNamingEnumeration<>(typeClass, backing, RecordingDirContext.class, new Hashtable<>(), translator, new JavaNameParser(translator), false);
  }

  public void testNameClassPairNameIsTranslatedAndClassNameRewritten ()
    throws NamingException {

    NameClassPair element = new NameClassPair("CN=Alpha", RecordingDirContext.class.getName(), true);
    JavaNamingEnumeration<NameClassPair> enumeration = wrap(NameClassPair.class, new FakeNamingEnumeration<>(Collections.singletonList(element)));

    Assert.assertTrue(enumeration.hasMore());

    NameClassPair translated = enumeration.next();

    Assert.assertEquals(translated.getName(), "cn=alpha", "the element name should be translated to internal form");
    Assert.assertEquals(translated.getClassName(), JavaContext.class.getName(), "a backing context class name should be rewritten to JavaContext");
    Assert.assertTrue(translated.isRelative(), "the relative flag should be preserved");
    Assert.assertFalse(enumeration.hasMore());
  }

  public void testBindingObjectIsRebuiltAndForeignClassNamePreserved ()
    throws NamingException {

    Binding element = new Binding("CN=Beta", "java.lang.String", "a-value", false);
    JavaNamingEnumeration<Binding> enumeration = wrap(Binding.class, new FakeNamingEnumeration<>(Collections.singletonList(element)));

    Binding translated = enumeration.next();

    Assert.assertEquals(translated.getName(), "cn=beta");
    Assert.assertEquals(translated.getClassName(), "java.lang.String", "a non-context class name should pass through unchanged");
    Assert.assertEquals(translated.getObject(), "a-value", "a non-context bound object should pass through unchanged");
    Assert.assertFalse(translated.isRelative());
  }

  public void testSearchResultCarriesAttributesThroughTranslation ()
    throws NamingException {

    BasicAttributes attributes = new BasicAttributes("cn", "gamma");
    SearchResult element = new SearchResult("CN=Gamma", "java.lang.String", "obj", attributes, true);
    JavaNamingEnumeration<SearchResult> enumeration = wrap(SearchResult.class, new FakeNamingEnumeration<>(Collections.singletonList(element)));

    SearchResult translated = enumeration.next();

    Assert.assertEquals(translated.getName(), "cn=gamma");
    Assert.assertSame(translated.getAttributes(), attributes, "the attribute set should be carried through to the rebuilt result");
  }

  public void testHasMoreElementsSwallowsNamingExceptionAsFalse () {

    FakeNamingEnumeration<NameClassPair> backing = new FakeNamingEnumeration<>(Collections.<NameClassPair>emptyList(), new NamingException("boom"));
    JavaNamingEnumeration<NameClassPair> enumeration = wrap(NameClassPair.class, backing);

    Assert.assertFalse(enumeration.hasMoreElements(), "a NamingException from the backing store should surface as no-more-elements");
  }

  public void testReflectiveRebuildFailureSurfacesAsNamingException () {

    // Asking a NameClassPair enumeration to carry a SearchResult forces next() to look up a
    // SearchResult-shaped constructor on NameClassPair, which does not exist; the resulting
    // reflective failure must be wrapped as a NamingException.
    SearchResult element = new SearchResult("CN=Delta", "java.lang.String", "obj", new BasicAttributes(), true);
    JavaNamingEnumeration<NameClassPair> enumeration = wrap(NameClassPair.class, new FakeNamingEnumeration<>(Collections.singletonList((NameClassPair)element)));

    Assert.assertThrows(NamingException.class, enumeration::next);
  }

  public void testNextElementMapsNamingExceptionToNoSuchElement () {

    SearchResult element = new SearchResult("CN=Delta", "java.lang.String", "obj", new BasicAttributes(), true);
    JavaNamingEnumeration<NameClassPair> enumeration = wrap(NameClassPair.class, new FakeNamingEnumeration<>(Collections.singletonList((NameClassPair)element)));

    // The reflective NamingException from next() must be re-presented as a NoSuchElementException.
    Assert.assertThrows(NoSuchElementException.class, enumeration::nextElement);
  }

  public void testCloseDelegatesToTheBackingEnumeration ()
    throws NamingException {

    FakeNamingEnumeration<NameClassPair> backing = new FakeNamingEnumeration<>(Collections.<NameClassPair>emptyList());
    JavaNamingEnumeration<NameClassPair> enumeration = wrap(NameClassPair.class, backing);

    enumeration.close();

    Assert.assertTrue(backing.isClosed(), "closing the wrapper should close the underlying enumeration");
  }
}
