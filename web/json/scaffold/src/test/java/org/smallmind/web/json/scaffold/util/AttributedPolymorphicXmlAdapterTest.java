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
package org.smallmind.web.json.scaffold.util;

import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import tools.jackson.databind.node.ObjectNode;

/**
 * Round-trips a polymorphic value through {@link AttributedPolymorphicXmlAdapter}: marshalling writes
 * the subclass payload directly into an object node and injects a discriminator attribute named for the
 * subclass {@code @XmlRootElement}, and unmarshalling reads that attribute back to the concrete subclass.
 * Also covers the missing-attribute, unknown-discriminator, missing-{@code @XmlRootElement}, and
 * serialize-the-root-class error paths.
 *
 * <p>A fresh {@link PerApplicationContext} is attached per method because the attribute name is resolved
 * through {@link PolymorphicAttributeManager} and the per-application data registry. The fixture types
 * (Animal/Dog/Cat) are public and top-level, since the underlying {@code ProxyGenerator} refuses to
 * subclass a {@code static} nested type.
 */
@Test(groups = "unit")
public class AttributedPolymorphicXmlAdapterTest {

  @BeforeMethod
  public void beforeMethod () {

    new PerApplicationContext();
  }

  public void testDefaultPolymorphicAttributeName () {

    Assert.assertEquals(AttributedPolymorphicXmlAdapter.getDefaultPolymorphicAttributeName(), "java/object");
  }

  public void testMarshalInjectsDiscriminatorAttribute () {

    PolymorphicAnimalDog dog = new PolymorphicAnimalDog();
    dog.setLegs(4);

    ObjectNode node = new AttributedAnimalAdapter().marshal(dog);

    Assert.assertEquals(node.get("legs").intValue(), 4);
    Assert.assertEquals(node.get(PolymorphicAttributeManager.getPolymorphicAttributeName()).asString(), "dog");
  }

  public void testRoundTripResolvesConcreteSubclass () {

    AttributedAnimalAdapter adapter = new AttributedAnimalAdapter();

    PolymorphicAnimalCat cat = new PolymorphicAnimalCat();
    cat.setName("Felix");

    PolymorphicAnimal recovered = adapter.unmarshal(adapter.marshal(cat));

    Assert.assertTrue(recovered instanceof PolymorphicAnimalCat, recovered.getClass().getName());
    Assert.assertEquals(((PolymorphicAnimalCat)recovered).getName(), "Felix");
  }

  public void testRoundTripWithCustomAttributeName () {

    PolymorphicAttributeManager.setPolymorphicAttributeName("@kind");

    AttributedAnimalAdapter adapter = new AttributedAnimalAdapter();

    PolymorphicAnimalDog dog = new PolymorphicAnimalDog();
    dog.setLegs(3);

    ObjectNode node = adapter.marshal(dog);

    Assert.assertEquals(node.get("@kind").asString(), "dog");

    PolymorphicAnimal recovered = adapter.unmarshal(node);

    Assert.assertTrue(recovered instanceof PolymorphicAnimalDog, recovered.getClass().getName());
    Assert.assertEquals(((PolymorphicAnimalDog)recovered).getLegs(), 3);
  }

  @Test(expectedExceptions = JAXBProcessingException.class)
  public void testUnmarshalMissingAttributeFails () {

    new AttributedAnimalAdapter().unmarshal((ObjectNode)JsonCodec.readAsJsonNode("{\"legs\":4}"));
  }

  @Test(expectedExceptions = JAXBProcessingException.class)
  public void testUnmarshalUnknownDiscriminatorFails () {

    new AttributedAnimalAdapter().unmarshal((ObjectNode)JsonCodec.readAsJsonNode("{\"" + PolymorphicAttributeManager.getPolymorphicAttributeName() + "\":\"fish\"}"));
  }

  @Test(expectedExceptions = JAXBProcessingException.class)
  public void testMarshalMissingRootElementFails () {

    new AttributedAnimalAdapter().marshal(new PolymorphicAnimalUnannotated());
  }
}
