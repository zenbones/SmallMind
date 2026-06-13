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
package org.smallmind.spark.singularity.boot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

@org.testng.annotations.Test(groups = "unit")
public class SingularityIndexTest {

  private static final String PARENT = "file:/opt/app/application.jar";

  // The singularity: URLs emitted below can only be turned into URL instances once the synthetic protocol handler is
  // registered, which happens exactly once in SingularityClassLoader's static initializer. Triggering it here keeps
  // that JVM-wide, one-shot registration funneled through the single production registrar.
  @BeforeClass
  public void registerSingularityProtocol ()
    throws ClassNotFoundException {

    Class.forName(SingularityClassLoader.class.getName());
  }

  private static HashMap<String, String> collect (Iterable<SingularityIndex.URLEntry> iterable) {

    HashMap<String, String> nameToExternalForm = new HashMap<>();

    for (SingularityIndex.URLEntry urlEntry : iterable) {
      nameToExternalForm.put(urlEntry.entryName(), urlEntry.entryURL().toExternalForm());
    }

    return nameToExternalForm;
  }

  public void testBareFilesBecomeJarProtocolUrls () {

    SingularityIndex index = new SingularityIndex();

    index.addFileName("org/example/Widget.class");
    index.addFileName("config/app.properties");

    HashMap<String, String> entries = collect(index.getJarURLEntryIterable(PARENT));

    Assert.assertEquals(entries.size(), 2);
    Assert.assertEquals(entries.get("org/example/Widget.class"), "jar:" + PARENT + "!/org/example/Widget.class");
    Assert.assertEquals(entries.get("config/app.properties"), "jar:" + PARENT + "!/config/app.properties");
  }

  public void testLibraryEntriesBecomeSingularityProtocolUrls () {

    SingularityIndex index = new SingularityIndex();

    index.addInverseJarEntry("org/lib/Thing.class", "thing-1.0.jar");

    HashMap<String, String> entries = collect(index.getSingularityURLEntryIterable(PARENT));

    Assert.assertEquals(entries.size(), 1);
    Assert.assertEquals(entries.get("org/lib/Thing.class"), "singularity:" + PARENT + "@/META-INF/singularity/lib/thing-1.0.jar!/org/lib/Thing.class");
  }

  public void testEmptyIndexYieldsNothing () {

    SingularityIndex index = new SingularityIndex();

    Assert.assertFalse(index.getJarURLEntryIterable(PARENT).iterator().hasNext());
    Assert.assertFalse(index.getSingularityURLEntryIterable(PARENT).iterator().hasNext());
  }

  public void testFileAndLibraryPopulationsRemainSeparate () {

    SingularityIndex index = new SingularityIndex();

    index.addFileName("a.txt");
    index.addInverseJarEntry("b.txt", "lib.jar");

    Assert.assertEquals(collect(index.getJarURLEntryIterable(PARENT)).keySet(), java.util.Set.of("a.txt"));
    Assert.assertEquals(collect(index.getSingularityURLEntryIterable(PARENT)).keySet(), java.util.Set.of("b.txt"));
  }

  public void testJarIteratorRefusesRemoval () {

    SingularityIndex index = new SingularityIndex();

    index.addFileName("a.txt");

    Assert.assertThrows(UnsupportedOperationException.class, () -> index.getJarURLEntryIterable(PARENT).iterator().remove());
  }

  public void testSingularityIteratorRefusesRemoval () {

    SingularityIndex index = new SingularityIndex();

    index.addInverseJarEntry("a.txt", "lib.jar");

    Assert.assertThrows(UnsupportedOperationException.class, () -> index.getSingularityURLEntryIterable(PARENT).iterator().remove());
  }

  // The class loader recovers the index by deserializing it out of the bundle, so the surviving state must still
  // produce the same URLs after a round trip through the serialization machinery.
  public void testSurvivesSerializationRoundTrip ()
    throws Exception {

    SingularityIndex index = new SingularityIndex();

    index.addFileName("org/example/Widget.class");
    index.addInverseJarEntry("org/lib/Thing.class", "thing-1.0.jar");

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      objectOutputStream.writeObject(index);
    }

    SingularityIndex restored;

    try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
      restored = (SingularityIndex)objectInputStream.readObject();
    }

    Assert.assertEquals(collect(restored.getJarURLEntryIterable(PARENT)).get("org/example/Widget.class"), "jar:" + PARENT + "!/org/example/Widget.class");
    Assert.assertEquals(collect(restored.getSingularityURLEntryIterable(PARENT)).get("org/lib/Thing.class"), "singularity:" + PARENT + "@/META-INF/singularity/lib/thing-1.0.jar!/org/lib/Thing.class");
  }
}
