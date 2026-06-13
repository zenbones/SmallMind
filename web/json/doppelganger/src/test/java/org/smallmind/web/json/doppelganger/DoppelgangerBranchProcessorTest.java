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
import org.testng.annotations.Test;

/**
 * Integration tests that exercise the heavily branched generation paths of {@link ClassWalker},
 * {@link DoppelgangerAnnotationProcessor}, and {@link DoppelgangerInformation} by compiling annotated
 * fixture classes through the in-process {@code javac} harness and inspecting the generated view sources.
 */
@Test(groups = "integration")
public class DoppelgangerBranchProcessorTest {

  private DoppelgangerCompilationFixture fixture;

  private DoppelgangerCompilationFixture freshFixture ()
    throws Exception {

    if (fixture != null) {
      fixture.cleanup();
    }
    fixture = new DoppelgangerCompilationFixture();

    return fixture;
  }

  @org.testng.annotations.AfterMethod(alwaysRun = true)
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

  public void testFieldViewWithConstraintsCommentAdapterRequired ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Gadget",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Constraint;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "import jakarta.validation.constraints.NotNull;\n"
        + "@Doppelganger\n"
        + "public class Gadget {\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.BOTH, constraints = @Constraint(NotNull.class), required = true), comment = \"the label\", name = \"label\")\n"
        + "  private String name;\n"
        + "  public String getName () { return name; }\n"
        + "  public void setName (String name) { this.name = name; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);
    Assert.assertTrue(result.hasGenerated("widget/GadgetInView.java"), result.getGenerated().keySet().toString());
    Assert.assertTrue(result.hasGenerated("widget/GadgetOutView.java"));

    String outView = result.getSource("widget/GadgetOutView.java");
    Assert.assertTrue(outView.contains("@jakarta.validation.constraints.NotNull"), "constraint should be emitted");
    Assert.assertTrue(outView.contains("@Comment(\"the label\")"), "comment should be emitted");
    Assert.assertTrue(outView.contains("name = \"label\""), "xml element name override should be emitted");
    Assert.assertTrue(outView.contains("required = true"), "required flag should be emitted");
  }

  public void testVirtualPropertyAndSerializableAndImports ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Holder",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Virtual;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "import org.smallmind.web.json.doppelganger.Import;\n"
        + "@Doppelganger(serializable = true, comment = \"a holder\", imports = @Import(\"java.util.UUID\"), virtual = {\n"
        + "  @Virtual(field = \"token\", type = @Type(String.class))\n"
        + "})\n"
        + "public class Holder {\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);
    Assert.assertTrue(result.hasGenerated("widget/HolderInView.java"));
    Assert.assertTrue(result.hasGenerated("widget/HolderOutView.java"));

    String outView = result.getSource("widget/HolderOutView.java");
    Assert.assertTrue(outView.contains("implements"), "serializable should add an implements clause");
    Assert.assertTrue(outView.contains("java.io.Serializable"));
    Assert.assertTrue(outView.contains("// virtual fields"));
    Assert.assertTrue(outView.contains("token"));
    Assert.assertTrue(outView.contains("@Comment(\"a holder\")"));
    Assert.assertTrue(outView.contains("import java.util.UUID;"), "additional import should be emitted");
  }

  public void testListAndArrayOfGeneratedViewsUseMutators ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Leaf",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"id\", type = @Type(Long.class)))\n"
        + "public class Leaf {\n"
        + "  private Long id;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "}\n");
    sources.put("widget.Branch",
      "package widget;\n"
        + "import java.util.List;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = {\n"
        + "  @Real(field = \"leaves\", type = @Type(value = List.class, parameters = Leaf.class)),\n"
        + "  @Real(field = \"leafArray\", type = @Type(Leaf[].class))\n"
        + "})\n"
        + "public class Branch {\n"
        + "  private List<Leaf> leaves;\n"
        + "  private Leaf[] leafArray;\n"
        + "  public List<Leaf> getLeaves () { return leaves; }\n"
        + "  public void setLeaves (List<Leaf> leaves) { this.leaves = leaves; }\n"
        + "  public Leaf[] getLeafArray () { return leafArray; }\n"
        + "  public void setLeafArray (Leaf[] leafArray) { this.leafArray = leafArray; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    String branchOut = result.getSource("widget/BranchOutView.java");
    Assert.assertNotNull(branchOut, result.getGenerated().keySet().toString());
    Assert.assertTrue(branchOut.contains("ListMutator.toViewType"), "list translator should be selected");
    Assert.assertTrue(branchOut.contains("ArrayMutator.toViewType"), "array translator should be selected");

    String branchIn = result.getSource("widget/BranchInView.java");
    Assert.assertTrue(branchIn.contains("ListMutator.toEntityType"));
    Assert.assertTrue(branchIn.contains("ArrayMutator.toEntityType"));
  }

  public void testPolymorphicHierarchyGeneratesAdapters ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("zoo.Animal",
      "package zoo;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Polymorphic;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(polymorphic = @Polymorphic(subClasses = {Dog.class}), real = @Real(field = \"name\", type = @Type(String.class)))\n"
        + "public abstract class Animal {\n"
        + "  private String name;\n"
        + "  public String getName () { return name; }\n"
        + "  public void setName (String name) { this.name = name; }\n"
        + "}\n");
    sources.put("zoo.Dog",
      "package zoo;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"breed\", type = @Type(String.class)))\n"
        + "public class Dog extends Animal {\n"
        + "  private String breed;\n"
        + "  public String getBreed () { return breed; }\n"
        + "  public void setBreed (String breed) { this.breed = breed; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);

    Assert.assertTrue(result.hasGenerated("zoo/AnimalOutViewPolymorphicXmlAdapter.java"), result.getGenerated().keySet().toString());
    Assert.assertTrue(result.hasGenerated("zoo/AnimalInViewPolymorphicXmlAdapter.java"));

    String animalOut = result.getSource("zoo/AnimalOutView.java");
    Assert.assertNotNull(animalOut);
    Assert.assertTrue(animalOut.contains("abstract class AnimalOutView"), "abstract base should stay abstract");
    Assert.assertTrue(animalOut.contains("@XmlJavaTypeAdapter"), "polymorphic base should carry the adapter");

    String dogOut = result.getSource("zoo/DogOutView.java");
    Assert.assertNotNull(dogOut);
    Assert.assertTrue(dogOut.contains("extends AnimalOutView"), "subclass view should extend the base view");
  }

  public void testPledgeForcesEmptyView ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Beacon",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Pledge;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger(pledges = @Pledge(visibility = Visibility.OUT, purposes = \"ping\"))\n"
        + "public class Beacon {\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);
    Assert.assertTrue(result.hasGenerated("widget/BeaconPingOutView.java"), result.getGenerated().keySet().toString());

    String pingOut = result.getSource("widget/BeaconPingOutView.java");
    Assert.assertTrue(pingOut.contains("return 0;"), "an empty view should hashCode to a constant");
  }

  public void testStaticViewElementIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Bad",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "@Doppelganger\n"
        + "public class Bad {\n"
        + "  @View\n"
        + "  private static String name;\n"
        + "  public String getName () { return name; }\n"
        + "  public void setName (String name) { Bad.name = name; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "a static @View field should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("may not be 'static'")), result.getErrors().toString());
  }

  public void testGetterAnnotatedInOnlyIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Skewed",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.View;\n"
        + "import org.smallmind.web.json.doppelganger.Idiom;\n"
        + "import org.smallmind.web.json.doppelganger.Visibility;\n"
        + "@Doppelganger\n"
        + "public class Skewed {\n"
        + "  @View(idioms = @Idiom(visibility = Visibility.IN))\n"
        + "  public String getName () { return null; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "an IN-only getter should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("can't be annotated as 'IN' only")), result.getErrors().toString());
  }

  public void testEmptyDoppelgangerIsRejected ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Empty",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "@Doppelganger\n"
        + "public class Empty {\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    Assert.assertFalse(result.isSuccess(), "a property-less concrete class should be rejected");
    Assert.assertTrue(result.getErrors().stream().anyMatch((message) -> message.contains("contained no properties")), result.getErrors().toString());
  }

  public void testPrefixOptionAppliedToViewName ()
    throws Exception {

    Map<String, String> sources = new LinkedHashMap<>();

    sources.put("widget.Plain",
      "package widget;\n"
        + "import org.smallmind.web.json.doppelganger.Doppelganger;\n"
        + "import org.smallmind.web.json.doppelganger.Real;\n"
        + "import org.smallmind.web.json.doppelganger.Type;\n"
        + "@Doppelganger(real = @Real(field = \"id\", type = @Type(Long.class)))\n"
        + "public class Plain {\n"
        + "  private Long id;\n"
        + "  public Long getId () { return id; }\n"
        + "  public void setId (Long id) { this.id = id; }\n"
        + "}\n");

    DoppelgangerCompilationFixture.Result result = freshFixture().compile(sources);

    assertNoErrors(result);
    Assert.assertTrue(result.hasGenerated("widget/PlainInView.java"));
    Assert.assertTrue(result.hasGenerated("widget/PlainOutView.java"));

    String outView = result.getSource("widget/PlainOutView.java");
    Assert.assertTrue(outView.contains("Objects.hashCode(getId())"), "single-property hashCode should reference the getter");
  }
}
