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
package org.smallmind.claxon.emitter.message;

import java.util.ArrayList;
import java.util.List;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MessageEmitterTest {

  public void testRecordWithNullOrEmptyTagsProducesEmptyBracketSection () {

    List<String> output = new ArrayList<>();
    MessageEmitter emitter = new MessageEmitter(output::add);

    emitter.record("meter", null, new Quantity[] {new Quantity("count", 5.0)});
    emitter.record("meter", new Tag[0], new Quantity[] {new Quantity("count", 7.0)});

    Assert.assertEquals(output, List.of("meter[].count=5.0", "meter[].count=7.0"));
  }

  public void testRecordWithSingleTag () {

    List<String> output = new ArrayList<>();

    new MessageEmitter(output::add).record(
      "meter",
      new Tag[] {new Tag("env", "prod")},
      new Quantity[] {new Quantity("count", 3.0)});

    Assert.assertEquals(output.size(), 1);
    Assert.assertEquals(output.getFirst(), "meter[env=prod].count=3.0");
  }

  public void testRecordWithMultipleTagsAreCommaSeparated () {

    List<String> output = new ArrayList<>();

    new MessageEmitter(output::add).record(
      "meter",
      new Tag[] {new Tag("env", "prod"), new Tag("region", "us-east-1")},
      new Quantity[] {new Quantity("count", 1.0)});

    Assert.assertEquals(output.size(), 1);
    Assert.assertEquals(output.getFirst(), "meter[env=prod, region=us-east-1].count=1.0");
  }

  public void testRecordEmitsOneLinePerQuantity () {

    List<String> output = new ArrayList<>();

    new MessageEmitter(output::add).record(
      "meter",
      new Tag[] {new Tag("env", "prod")},
      new Quantity[] {new Quantity("count", 2.0), new Quantity("rate", 4.0)});

    Assert.assertEquals(output.size(), 2);
    Assert.assertEquals(output.get(0), "meter[env=prod].count=2.0");
    Assert.assertEquals(output.get(1), "meter[env=prod].rate=4.0");
  }
}
