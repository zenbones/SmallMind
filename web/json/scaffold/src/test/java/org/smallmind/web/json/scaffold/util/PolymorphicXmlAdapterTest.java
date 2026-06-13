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

import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;

/**
 * Round-trips a polymorphic value through {@link PolymorphicXmlAdapter}: marshalling wraps the payload
 * in a single-key object named for the subclass {@code @XmlRootElement}, and unmarshalling resolves the
 * discriminator back to the concrete subclass. Also covers the malformed-node and unknown-discriminator
 * error paths.
 *
 * <p>The fixture types (Animal/Dog/Cat) are top-level, not {@code static} nested classes, because the
 * underlying {@code ProxyGenerator} refuses to subclass a {@code static} nested type.
 */
@Test(groups = "unit")
public class PolymorphicXmlAdapterTest {

  public void testMarshalWrapsPayloadUnderRootElementName ()
    throws Exception {

    PolymorphicAnimalDog dog = new PolymorphicAnimalDog();
    dog.setLegs(4);

    JsonNode node = new PolymorphicAnimalAdapter().marshal(dog);

    Assert.assertEquals(node.size(), 1);
    Assert.assertTrue(node.has("dog"), node.toString());
    Assert.assertEquals(node.get("dog").get("legs").intValue(), 4);
  }

  public void testRoundTripResolvesConcreteSubclass ()
    throws Exception {

    PolymorphicAnimalAdapter adapter = new PolymorphicAnimalAdapter();

    PolymorphicAnimalCat cat = new PolymorphicAnimalCat();
    cat.setName("Felix");

    PolymorphicAnimal recovered = adapter.unmarshal(adapter.marshal(cat));

    Assert.assertTrue(recovered instanceof PolymorphicAnimalCat, recovered.getClass().getName());
    Assert.assertEquals(((PolymorphicAnimalCat)recovered).getName(), "Felix");
  }

  @Test(expectedExceptions = JAXBProcessingException.class)
  public void testUnmarshalMalformedNodeFails () {

    new PolymorphicAnimalAdapter().unmarshal(JsonCodec.readAsJsonNode("{\"dog\":1,\"cat\":2}"));
  }

  @Test(expectedExceptions = JAXBProcessingException.class)
  public void testUnmarshalUnknownDiscriminatorFails () {

    new PolymorphicAnimalAdapter().unmarshal(JsonCodec.readAsJsonNode("{\"fish\":{}}"));
  }

  @Test(expectedExceptions = JAXBProcessingException.class)
  public void testMarshalMissingRootElementFails () {

    new PolymorphicAnimalAdapter().marshal(new PolymorphicAnimalUnannotated());
  }
}
