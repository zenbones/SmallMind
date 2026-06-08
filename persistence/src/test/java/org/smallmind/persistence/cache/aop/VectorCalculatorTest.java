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
package org.smallmind.persistence.cache.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.mockito.Mockito;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.cache.VectorArtifact;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link VectorCalculator}, which composes a {@link VectorArtifact} from {@link Vector}
 * annotation metadata and runtime values, and for {@link Classifications}, which resolves the classifier
 * segment of a vector key. Annotation instances are built programmatically through
 * {@link VectorLiteral}/{@link KeyLiteral}/{@link ClassifierLiteral}, so no woven aspect is required. The
 * durable path reads bean properties directly; the join-point path is exercised with constant keys (so no
 * parameter extraction occurs) over a Mockito-mocked {@link JoinPoint}, to verify namespace defaulting. The
 * parameter-based classifier path is exercised over a Mockito-mocked {@link MethodSignature}, since
 * {@link org.smallmind.nutsnbolts.reflection.aop.AOPUtility} casts the join-point signature to that type.
 */
@Test(groups = "unit")
public class VectorCalculatorTest {

  public void testGetValueReadsBeanPropertyFromDurable () {

    Assert.assertEquals(VectorCalculator.getValue(new Widget(1L, "alpha"), "name", false), "alpha");
  }

  public void testGetValueReturnsNullForNullablePropertyThatIsNull () {

    Assert.assertNull(VectorCalculator.getValue(new Widget(1L, null), "name", true), "a null value is permitted when the key is nullable");
  }

  public void testGetVectorArtifactFromDurableMixesConstantAndPropertyKeys () {

    Vector vector = new VectorLiteral("widgets", new Key[] {new KeyLiteral("name", "byName", false, false), new KeyLiteral("ACTIVE", "state", true, false)});

    VectorArtifact artifact = VectorCalculator.getVectorArtifact(vector, new Widget(1L, "alpha"));

    Assert.assertEquals(artifact.getVectorNamespace(), "widgets");
    Assert.assertEquals(artifact.getVectorIndices().length, 2);

    Assert.assertEquals(artifact.getVectorIndices()[0].getIndexField(), "name");
    Assert.assertEquals(artifact.getVectorIndices()[0].getIndexValue(), "alpha", "a non-constant key reads the durable's bean property");
    Assert.assertEquals(artifact.getVectorIndices()[0].getIndexAlias(), "byName");

    Assert.assertEquals(artifact.getVectorIndices()[1].getIndexField(), "ACTIVE");
    Assert.assertEquals(artifact.getVectorIndices()[1].getIndexValue(), "ACTIVE", "a constant key uses its literal value");
    Assert.assertEquals(artifact.getVectorIndices()[1].getIndexAlias(), "state");
  }

  public void testGetVectorArtifactFromJoinPointDefaultsNamespaceToMethodName () {

    Signature signature = Mockito.mock(Signature.class);
    JoinPoint joinPoint = Mockito.mock(JoinPoint.class);

    Mockito.when(joinPoint.getSignature()).thenReturn(signature);
    Mockito.when(signature.getName()).thenReturn("lookupActiveWidgets");

    Vector vector = new VectorLiteral("", new Key[] {new KeyLiteral("ACTIVE", "state", true, false)});

    VectorArtifact artifact = VectorCalculator.getVectorArtifact(vector, joinPoint);

    Assert.assertEquals(artifact.getVectorNamespace(), "lookupActiveWidgets", "an empty namespace should default to the join-point method name");
    Assert.assertEquals(artifact.getVectorIndices().length, 1);
    Assert.assertEquals(artifact.getVectorIndices()[0].getIndexValue(), "ACTIVE");
  }

  public void testGetVectorArtifactFromJoinPointKeepsExplicitNamespace () {

    JoinPoint joinPoint = Mockito.mock(JoinPoint.class);

    Vector vector = new VectorLiteral("explicit", new Key[] {new KeyLiteral("ACTIVE", "", true, false)});

    VectorArtifact artifact = VectorCalculator.getVectorArtifact(vector, joinPoint);

    Assert.assertEquals(artifact.getVectorNamespace(), "explicit", "a non-empty namespace should be used verbatim");
    Mockito.verify(joinPoint, Mockito.never()).getSignature();
  }

  public void testClassificationsReturnsLiteralValueWhenNotParameterBased () {

    Vector vector = new VectorLiteral("widgets", new Key[] {new KeyLiteral("id")}, new ClassifierLiteral("REGULAR"));

    Assert.assertEquals(Classifications.get(CacheAs.class, null, vector), "REGULAR", "a non-parameter classifier returns its literal value verbatim");
  }

  public void testClassificationsReturnsEmptyLiteralValueWhenNotParameterBased () {

    Vector vector = new VectorLiteral("widgets", new Key[] {new KeyLiteral("id")}, new ClassifierLiteral(""));

    Assert.assertEquals(Classifications.get(CachedWith.class, null, vector), "", "an empty literal classifier is returned as-is, even outside a @CacheAs context");
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testClassificationsRejectsParameterClassifierOutsideCacheAs () {

    Vector vector = new VectorLiteral("widgets", new Key[] {new KeyLiteral("id")}, new ClassifierLiteral("kind", true));

    Classifications.get(CachedWith.class, null, vector);
  }

  public void testClassificationsRejectsParameterClassifierOutsideCacheAsCarriesMessage () {

    Vector vector = new VectorLiteral("widgets", new Key[] {new KeyLiteral("id")}, new ClassifierLiteral("kind", true));

    try {
      Classifications.get(CachedWith.class, null, vector);
      Assert.fail("a parameter-based classifier outside of @CacheAs should raise an error");
    } catch (CacheAutomationError cacheAutomationError) {
      Assert.assertEquals(cacheAutomationError.getMessage(), "Parameter based classifiers can only be used to annotate method executions (@CacheAs)");
    }
  }

  public void testClassificationsReadsParameterValueFromJoinPoint () {

    MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
    JoinPoint joinPoint = Mockito.mock(JoinPoint.class);

    Mockito.when(joinPoint.getSignature()).thenReturn(methodSignature);
    Mockito.when(methodSignature.getParameterNames()).thenReturn(new String[] {"kind"});
    Mockito.when(methodSignature.getParameterTypes()).thenReturn(new Class[] {String.class});
    Mockito.when(joinPoint.getArgs()).thenReturn(new Object[] {"PREMIUM"});

    Vector vector = new VectorLiteral("widgets", new Key[] {new KeyLiteral("id")}, new ClassifierLiteral("kind", true));

    Assert.assertEquals(Classifications.get(CacheAs.class, joinPoint, vector), "PREMIUM", "a parameter-based classifier reads the named argument's value");
  }

  public void testClassificationsConvertsParameterValueToString () {

    MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
    JoinPoint joinPoint = Mockito.mock(JoinPoint.class);

    Mockito.when(joinPoint.getSignature()).thenReturn(methodSignature);
    Mockito.when(methodSignature.getParameterNames()).thenReturn(new String[] {"kind"});
    Mockito.when(methodSignature.getParameterTypes()).thenReturn(new Class[] {Integer.class});
    Mockito.when(joinPoint.getArgs()).thenReturn(new Object[] {7});

    Vector vector = new VectorLiteral("widgets", new Key[] {new KeyLiteral("id")}, new ClassifierLiteral("kind", true));

    Assert.assertEquals(Classifications.get(CacheAs.class, joinPoint, vector), "7", "a non-string parameter value is rendered through toString()");
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testClassificationsWrapsMissingParameterAsCacheAutomationError () {

    MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
    JoinPoint joinPoint = Mockito.mock(JoinPoint.class);

    Mockito.when(joinPoint.getSignature()).thenReturn(methodSignature);
    Mockito.when(methodSignature.getName()).thenReturn("lookupWidgets");
    Mockito.when(methodSignature.getParameterNames()).thenReturn(new String[] {"other"});
    Mockito.when(methodSignature.getParameterTypes()).thenReturn(new Class[] {String.class});
    Mockito.when(joinPoint.getArgs()).thenReturn(new Object[] {"value"});

    Vector vector = new VectorLiteral("widgets", new Key[] {new KeyLiteral("id")}, new ClassifierLiteral("missing", true));

    Classifications.get(CacheAs.class, joinPoint, vector);
  }

  public static class Widget extends AbstractDurable<Long, Widget> {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;

    public Widget () {

    }

    public Widget (Long id, String name) {

      this.id = id;
      this.name = name;
    }

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }
  }
}
