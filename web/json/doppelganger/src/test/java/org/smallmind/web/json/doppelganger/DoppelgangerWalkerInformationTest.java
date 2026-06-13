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
package org.smallmind.web.json.doppelganger;

import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Integration tests that drive the branch-heavy member-walking and annotation-aggregation paths of
 * {@link ClassWalker} and {@link DoppelgangerInformation} by compiling additional annotated fixture
 * classes through the in-process {@code javac} harness and asserting against the generated view sources.
 */
@Test(groups = "integration")
public class DoppelgangerWalkerInformationTest {

  private DoppelgangerCompilationFixture fixture;

  private DoppelgangerCompilationFixture freshFixture ()
    throws Exception {

    if (fixture != null) {
      fixture.cleanup();
    }
    fixture = new DoppelgangerCompilationFixture();

    return fixture;
  }

  @AfterMethod(alwaysRun = true)
  public void afterMethod ()
    throws Exception {

    if (fixture != null) {
      fixture.cleanup();
      fixture = null;
    }
  }

  private void assertNoErrors (DoppelgangerCompilationFixture.Result result) {

    Assert.assertTrue(result.isSuccess(), "compilation failed: " + result.getErrors());
  }

  // ClassWalker: boolean 'is' accessor annotated as BOTH, with a matching setter, registers in both directions.

  public void testBooleanIsAccessorWithMatchingSetter ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Toggle",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "@Doppelganger\n"
        + "public class Toggle {\n"
        + "  private boolean active;\n"
        + "  @View\n"
        + "  public boolean isActive () { return active; }\n"
        + "  public void setActive (boolean active) { this.active = active; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);
    Assert.assertTrue(result.hasGenerated("widget/ToggleInView.java"), result.getGenerated().keySet().toString());
    Assert.assertTrue(result.hasGenerated("widget/ToggleOutView.java"));

    String outView = result.getSource("widget/ToggleOutView.java");
    Assert.assertTrue(outView.contains("public boolean isActive ()"), "boolean getter should use 'is' form");

    String inView = result.getSource("widget/ToggleInView.java");
    Assert.assertTrue(inView.contains("setActive"), "BOTH getter with a setter should reach the in guide");
  }

  // ClassWalker: a getter annotated as BOTH without a corresponding setter is rejected.

  public void testBothGetterWithoutSetterIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Orphan",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger\n"
        + "public class Orphan {\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.BOTH))\n"
        + "  public String getName () { return null; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "a BOTH getter with no setter should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("must have a corresponding 'setter'")), result.getErrors().toString());
  }

  // ClassWalker: a setter annotated other than IN only is rejected.

  public void testSetterAnnotatedBothIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Skew",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger\n"
        + "public class Skew {\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.BOTH))\n"
        + "  public void setName (String name) { }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "a non-IN setter should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("must be annotated as 'IN' only")), result.getErrors().toString());
  }

  // ClassWalker: a 'set' accessor annotated IN only generates only an in view property.

  public void testSetterInOnlyRegistersInbound ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Inbox",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger\n"
        + "public class Inbox {\n"
        + "  private String name;\n"
        + "  public String getName () { return name; }\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.IN))\n"
        + "  public void setName (String name) { this.name = name; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);
    Assert.assertTrue(result.hasGenerated("widget/InboxInView.java"), result.getGenerated().keySet().toString());

    String inView = result.getSource("widget/InboxInView.java");
    Assert.assertTrue(inView.contains("setName"), "the IN-only setter should yield an inbound property");
  }

  // ClassWalker: a field annotated IN with no matching setter is rejected through addInField.

  public void testInFieldWithoutSetterIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Locked",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger\n"
        + "public class Locked {\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.IN))\n"
        + "  private String name;\n"
        + "  public String getName () { return name; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "an IN field without a setter should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("has no 'setter' method")), result.getErrors().toString());
  }

  // ClassWalker: a field annotated OUT with no matching getter is rejected through addOutField.

  public void testOutFieldWithoutGetterIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Sealed",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger\n"
        + "public class Sealed {\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.OUT))\n"
        + "  private String name;\n"
        + "  public void setName (String name) { this.name = name; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "an OUT field without a getter should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("has no 'getter' method")), result.getErrors().toString());
  }

  // ClassWalker: a field annotated BOTH backed by an 'is' boolean accessor exercises the isFieldNameSet branch.

  public void testBothFieldBackedByBooleanIsAccessor ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Flag",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger\n"
        + "public class Flag {\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.BOTH))\n"
        + "  private boolean enabled;\n"
        + "  public boolean isEnabled () { return enabled; }\n"
        + "  public void setEnabled (boolean enabled) { this.enabled = enabled; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String outView = result.getSource("widget/FlagOutView.java");
    Assert.assertNotNull(outView, result.getGenerated().keySet().toString());
    Assert.assertTrue(outView.contains("isEnabled"), "a boolean BOTH field should resolve through the 'is' accessor");

    String inView = result.getSource("widget/FlagInView.java");
    Assert.assertTrue(inView.contains("setEnabled"), "a boolean BOTH field should also register inbound");
  }

  // ClassWalker: a method annotated with @View that is neither getter/setter nor 'is' is rejected.

  public void testNonAccessorViewMethodIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Doer",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "@Doppelganger\n"
        + "public class Doer {\n"
        + "  @View\n"
        + "  public String compute () { return null; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "a @View method that is not a getter/setter should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("must be a 'getter' or 'setter'")), result.getErrors().toString());
  }

  // ClassWalker: an abstract member annotated with @View is rejected.

  public void testAbstractViewMethodIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Shape",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "@Doppelganger\n"
        + "public abstract class Shape {\n"
        + "  @View\n"
        + "  public abstract String getName ();\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "an abstract @View method should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("may not be 'abstract'")), result.getErrors().toString());
  }

  // ClassWalker: a getter annotated OUT plus a separately annotated IN setter for the same field
  // exercises the getFieldNameSet/setMethodMap paths and yields both directions.

  public void testSplitGetterSetterViewMembers ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Split",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger\n"
        + "public class Split {\n"
        + "  private String name;\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.OUT))\n"
        + "  public String getName () { return name; }\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.IN))\n"
        + "  public void setName (String name) { this.name = name; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String outView = result.getSource("widget/SplitOutView.java");
    Assert.assertNotNull(outView, result.getGenerated().keySet().toString());
    Assert.assertTrue(outView.contains("getName"), "OUT getter should appear in the out view");

    String inView = result.getSource("widget/SplitInView.java");
    Assert.assertTrue(inView.contains("setName"), "IN setter should appear in the in view");
  }

  // ClassWalker + processor: a multi-level inherited hierarchy (grandchild extends child extends base),
  // all @Doppelganger, exercises getNearestViewSuperclass recursion and superclass extends clauses.

  public void testMultiLevelInheritanceChain ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("shape.Base",
      "package shape;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"id\", type = @Type(Long.class)))\n"
        + "public class Base {\n"
        + "  private Long id;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "}\n");
    sources.put("shape.Middle",
      "package shape;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"width\", type = @Type(Integer.class)))\n"
        + "public class Middle extends Base {\n"
        + "  private Integer width;\n"
        + "  public Integer getWidth () { return width; }\n"
        + "  public void setWidth (Integer width) { this.width = width; }\n"
        + "}\n");
    sources.put("shape.Leaf",
      "package shape;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"color\", type = @Type(String.class)))\n"
        + "public class Leaf extends Middle {\n"
        + "  private String color;\n"
        + "  public String getColor () { return color; }\n"
        + "  public void setColor (String color) { this.color = color; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String middleOut = result.getSource("shape/MiddleOutView.java");
    Assert.assertNotNull(middleOut, result.getGenerated().keySet().toString());
    Assert.assertTrue(middleOut.contains("extends BaseOutView"), "middle view should extend the base view");

    String leafOut = result.getSource("shape/LeafOutView.java");
    Assert.assertTrue(leafOut.contains("extends MiddleOutView"), "leaf view should extend the middle view");
    Assert.assertTrue(leafOut.contains("super.hashCode()"), "subclass hashCode should defer to its superclass");
    Assert.assertTrue(leafOut.contains("super.equals(obj)"), "subclass equals should defer to its superclass");
  }

  // ClassTracker + processor: a polymorphic base with multiple subclasses generates an adapter
  // referencing every subclass and exercises the multi-subclass instance() chain.

  public void testPolymorphicBaseWithMultipleSubclasses ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("zoo.Creature",
      "package zoo;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Polymorphic;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(polymorphic = @Polymorphic(subClasses = {Cat.class, Fish.class}), real = @Real(field = \"name\", type = @Type(String.class)))\n"
        + "public abstract class Creature {\n"
        + "  private String name;\n"
        + "  public String getName () { return name; }\n"
        + "  public void setName (String name) { this.name = name; }\n"
        + "}\n");
    sources.put("zoo.Cat",
      "package zoo;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"lives\", type = @Type(Integer.class)))\n"
        + "public class Cat extends Creature {\n"
        + "  private Integer lives;\n"
        + "  public Integer getLives () { return lives; }\n"
        + "  public void setLives (Integer lives) { this.lives = lives; }\n"
        + "}\n");
    sources.put("zoo.Fish",
      "package zoo;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"depth\", type = @Type(Integer.class)))\n"
        + "public class Fish extends Creature {\n"
        + "  private Integer depth;\n"
        + "  public Integer getDepth () { return depth; }\n"
        + "  public void setDepth (Integer depth) { this.depth = depth; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String creatureOut = result.getSource("zoo/CreatureOutView.java");
    Assert.assertNotNull(creatureOut, result.getGenerated().keySet().toString());
    Assert.assertTrue(creatureOut.contains("@XmlPolymorphicSubClasses({"), "more than one subclass should emit a brace-wrapped list");
    Assert.assertTrue(creatureOut.contains("CatOutView.class"));
    Assert.assertTrue(creatureOut.contains("FishOutView.class"));
    Assert.assertTrue(creatureOut.contains("instanceof zoo.Cat"), "instance() should branch on each subclass");
    Assert.assertTrue(creatureOut.contains("instanceof zoo.Fish"));
    Assert.assertTrue(creatureOut.contains("IllegalStateException"), "abstract polymorphic base should throw when no subclass matches");

    Assert.assertTrue(result.hasGenerated("zoo/CreatureOutViewPolymorphicXmlAdapter.java"));
    Assert.assertTrue(result.hasGenerated("zoo/CreatureInViewPolymorphicXmlAdapter.java"));
  }

  // ClassTracker + processor: a polymorphic base using the attribute discriminator generates the
  // Attributed adapter variant.

  public void testPolymorphicUseAttributeGeneratesAttributedAdapter ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("zoo.Vehicle",
      "package zoo;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Polymorphic;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(polymorphic = @Polymorphic(subClasses = {Car.class}, useAttribute = true), real = @Real(field = \"vin\", type = @Type(String.class)))\n"
        + "public abstract class Vehicle {\n"
        + "  private String vin;\n"
        + "  public String getVin () { return vin; }\n"
        + "  public void setVin (String vin) { this.vin = vin; }\n"
        + "}\n");
    sources.put("zoo.Car",
      "package zoo;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"doors\", type = @Type(Integer.class)))\n"
        + "public class Car extends Vehicle {\n"
        + "  private Integer doors;\n"
        + "  public Integer getDoors () { return doors; }\n"
        + "  public void setDoors (Integer doors) { this.doors = doors; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    Assert.assertTrue(result.hasGenerated("zoo/VehicleOutViewAttributedPolymorphicXmlAdapter.java"), result.getGenerated().keySet().toString());

    String vehicleOut = result.getSource("zoo/VehicleOutView.java");
    Assert.assertTrue(vehicleOut.contains("AttributedPolymorphicXmlAdapter.class"), "attribute mode should select the attributed adapter");
  }

  // ClassTracker + processor: a hierarchy base with subclasses generates parameterized views (no adapters).

  public void testHierarchyBaseWithSubclasses ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("tree.Node",
      "package tree;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Hierarchy;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(hierarchy = @Hierarchy(subClasses = {Branch.class}), real = @Real(field = \"label\", type = @Type(String.class)))\n"
        + "public abstract class Node {\n"
        + "  private String label;\n"
        + "  public String getLabel () { return label; }\n"
        + "  public void setLabel (String label) { this.label = label; }\n"
        + "}\n");
    sources.put("tree.Branch",
      "package tree;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"weight\", type = @Type(Integer.class)))\n"
        + "public class Branch extends Node {\n"
        + "  private Integer weight;\n"
        + "  public Integer getWeight () { return weight; }\n"
        + "  public void setWeight (Integer weight) { this.weight = weight; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String nodeOut = result.getSource("tree/NodeOutView.java");
    Assert.assertNotNull(nodeOut, result.getGenerated().keySet().toString());
    Assert.assertTrue(nodeOut.contains("<D extends NodeOutView<D>>"), "hierarchy base should carry a recursive type parameter");
    Assert.assertFalse(result.hasGenerated("tree/NodeOutViewPolymorphicXmlAdapter.java"), "hierarchy should not emit a polymorphic adapter");

    String branchOut = result.getSource("tree/BranchOutView.java");
    Assert.assertTrue(branchOut.contains("extends NodeOutView<"), "hierarchy subclass should extend the parameterized base view");
  }

  // DoppelgangerInformation: multiple @Pledge entries with multiple purposes each force several empty views.

  public void testMultiplePledgePurposes ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Signal",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Pledge;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger(pledges = {\n"
        + "  @Pledge(visibility = Visibility.OUT, purposes = {\"ping\", \"pong\"}),\n"
        + "  @Pledge(visibility = Visibility.IN, purposes = \"echo\")\n"
        + "})\n"
        + "public class Signal {\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);
    Assert.assertTrue(result.hasGenerated("widget/SignalPingOutView.java"), result.getGenerated().keySet().toString());
    Assert.assertTrue(result.hasGenerated("widget/SignalPongOutView.java"));
    Assert.assertTrue(result.hasGenerated("widget/SignalEchoInView.java"));
  }

  // DoppelgangerInformation: @Import scoped to a specific purpose lands only in that purpose's view,
  // while an unscoped @Import lands in the default views.

  public void testPurposeScopedAndDefaultImports ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Carrier",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Import;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger(imports = {\n"
        + "  @Import(\"java.util.UUID\"),\n"
        + "  @Import(purposes = \"detail\", value = \"java.util.Locale\")\n"
        + "}, real = {\n"
        + "  @Real(field = \"id\", type = @Type(Long.class)),\n"
        + "  @Real(field = \"name\", type = @Type(String.class), idioms = @Idiom(purposes = \"detail\", visibility = Visibility.OUT))\n"
        + "})\n"
        + "public class Carrier {\n"
        + "  private Long id;\n"
        + "  private String name;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "  public String getName () { return name; }\n"
        + "  public void setName (String name) { this.name = name; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String defaultOut = result.getSource("widget/CarrierOutView.java");
    Assert.assertNotNull(defaultOut, result.getGenerated().keySet().toString());
    Assert.assertTrue(defaultOut.contains("import java.util.UUID;"), "unscoped import should land in the default view");
    Assert.assertFalse(defaultOut.contains("import java.util.Locale;"), "purpose-scoped import should not leak into the default view");

    String detailOut = result.getSource("widget/CarrierDetailOutView.java");
    Assert.assertNotNull(detailOut, result.getGenerated().keySet().toString());
    Assert.assertTrue(detailOut.contains("import java.util.Locale;"), "purpose-scoped import should land in the purpose view");
  }

  // DoppelgangerInformation: @Implementation adds an interface to the generated views' implements clause.

  public void testImplementationAddsInterface ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Marked",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Implementation;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(implementations = @Implementation(java.io.Serializable.class), real = @Real(field = \"id\", type = @Type(Long.class)))\n"
        + "public class Marked {\n"
        + "  private Long id;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String outView = result.getSource("widget/MarkedOutView.java");
    Assert.assertNotNull(outView, result.getGenerated().keySet().toString());
    Assert.assertTrue(outView.contains("implements"), "declared implementation should add an implements clause");
    Assert.assertTrue(outView.contains("java.io.Serializable"), "the declared interface should be present");
  }

  // DoppelgangerInformation: a class-level constraining idiom emits a class-level constraint annotation.

  public void testConstrainingIdiomEmitsClassConstraint ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Guarded",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Constraint;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "import jakarta.validation.constraints.NotNull;\n"
        + "@Doppelganger(constrainingIdioms = @Idiom(visibility = Visibility.OUT, constraints = @Constraint(NotNull.class)), real = @Real(field = \"id\", type = @Type(Long.class)))\n"
        + "public class Guarded {\n"
        + "  private Long id;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String outView = result.getSource("widget/GuardedOutView.java");
    Assert.assertNotNull(outView, result.getGenerated().keySet().toString());
    Assert.assertTrue(outView.contains("@jakarta.validation.constraints.NotNull"), "class-level constraint should be emitted on the out view");

    String inView = result.getSource("widget/GuardedInView.java");
    Assert.assertFalse(inView.contains("@jakarta.validation.constraints.NotNull"), "an OUT-only constraining idiom should not affect the in view");
  }

  // DoppelgangerInformation.extractType: an array @Type that also declares parameters is rejected.

  public void testArrayTypeWithParametersIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Boxed",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"tags\", type = @Type(value = String[].class, parameters = String.class)))\n"
        + "public class Boxed {\n"
        + "  private String[] tags;\n"
        + "  public String[] getTags () { return tags; }\n"
        + "  public void setTags (String[] tags) { this.tags = tags; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "an array type with type arguments should be rejected");
    // extractType rethrows its own DefinitionException ahead of the generic catch, so the specific
    // "array types can't have type arguments" guidance reaches the caller intact.
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("array types can't have type arguments")), result.getErrors().toString());
  }

  // DoppelgangerInformation: a real field carrying a BOTH idiom plus a name override and a comment
  // exercises the BOTH branch of the real switch and propagates property metadata to both views.

  public void testRealFieldBothWithNameAndComment ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Tag",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"label\", type = @Type(String.class), name = \"caption\", comment = \"the caption\", required = true))\n"
        + "public class Tag {\n"
        + "  private String label;\n"
        + "  public String getLabel () { return label; }\n"
        + "  public void setLabel (String label) { this.label = label; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String outView = result.getSource("widget/TagOutView.java");
    Assert.assertNotNull(outView, result.getGenerated().keySet().toString());
    Assert.assertTrue(outView.contains("name = \"caption\""), "the view name override should be emitted");
    Assert.assertTrue(outView.contains("@Comment(\"the caption\")"), "the property comment should be emitted");
    Assert.assertTrue(outView.contains("required = true"), "the required flag should be emitted");

    String inView = result.getSource("widget/TagInView.java");
    Assert.assertTrue(inView.contains("name = \"caption\""), "a BOTH real field should also be present inbound");
  }
}
