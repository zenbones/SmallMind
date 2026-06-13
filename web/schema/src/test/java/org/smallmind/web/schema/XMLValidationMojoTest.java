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
package org.smallmind.web.schema;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Drives {@link XMLValidationMojo#execute} by reflectively injecting its parameters (no
 * maven-plugin-testing-harness), validating XML documents against a small schema acting as the W3C
 * meta-schema. Covers the success path, validation failure, missing-resource errors, and the null-schemas
 * short-circuit.
 *
 * <p>The inner XSD→XML-instance layer (the block that compiles each declared XSD into its own validator
 * and runs the schema's {@code impls} against it) is exercised by supplying a deliberately permissive
 * "w3c" schema that accepts any document rooted at {@code {http://www.w3.org/2001/XMLSchema}schema}. That
 * lets a real, self-contained XSD pass the outer meta-schema check and then act as the validator for its
 * own implementation files, so the full W3C {@code XMLSchema.xsd} meta-schema is not required as a test
 * resource.
 */
@Test(groups = "unit")
public class XMLValidationMojoTest {

  private static final String SCHEMA =
    "<?xml version=\"1.0\"?>"
      + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
      + "  <xs:element name=\"note\">"
      + "    <xs:complexType><xs:sequence><xs:element name=\"to\" type=\"xs:string\"/></xs:sequence></xs:complexType>"
      + "  </xs:element>"
      + "</xs:schema>";

  private static final String PERMISSIVE_W3C =
    "<?xml version=\"1.0\"?>"
      + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
      + "  targetNamespace=\"http://www.w3.org/2001/XMLSchema\""
      + "  elementFormDefault=\"qualified\">"
      + "  <xs:element name=\"schema\">"
      + "    <xs:complexType>"
      + "      <xs:sequence><xs:any minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"skip\"/></xs:sequence>"
      + "      <xs:anyAttribute processContents=\"skip\"/>"
      + "    </xs:complexType>"
      + "  </xs:element>"
      + "</xs:schema>";

  private static final String NOTE_XSD =
    "<?xml version=\"1.0\"?>"
      + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
      + "  <xs:element name=\"note\">"
      + "    <xs:complexType><xs:sequence><xs:element name=\"to\" type=\"xs:string\"/></xs:sequence></xs:complexType>"
      + "  </xs:element>"
      + "</xs:schema>";

  private static final String MALFORMED_XSD =
    "<?xml version=\"1.0\"?>"
      + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
      + "  <xs:bogus/>"
      + "</xs:schema>";

  private Path resourceDirectory;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    resourceDirectory = Files.createTempDirectory("schema-validation-test");

    Files.writeString(resourceDirectory.resolve("schema.xsd"), SCHEMA);
    Files.writeString(resourceDirectory.resolve("valid.xml"), "<note><to>Tove</to></note>");
    Files.writeString(resourceDirectory.resolve("invalid.xml"), "<note></note>");
    Files.writeString(resourceDirectory.resolve("permissive-w3c.xsd"), PERMISSIVE_W3C);
    Files.writeString(resourceDirectory.resolve("note.xsd"), NOTE_XSD);
    Files.writeString(resourceDirectory.resolve("malformed.xsd"), MALFORMED_XSD);
  }

  @AfterClass(alwaysRun = true)
  public void afterClass ()
    throws Exception {

    if (resourceDirectory != null) {
      try (var paths = Files.walk(resourceDirectory)) {
        paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
      }
    }
  }

  private static void setField (Object target, String name, Object value)
    throws Exception {

    Field field = XMLValidationMojo.class.getDeclaredField(name);

    field.setAccessible(true);
    field.set(target, value);
  }

  private XMLValidationMojo mojo (String w3c, List<XSD> schemas)
    throws Exception {

    Resource resource = new Resource();
    resource.setDirectory(resourceDirectory.toString());

    XMLValidationMojo mojo = new XMLValidationMojo();
    setField(mojo, "projectResources", List.of(resource));
    setField(mojo, "w3c", w3c);
    setField(mojo, "schemas", schemas);

    return mojo;
  }

  private XSD xsd (String path) {

    XSD xsd = new XSD();
    xsd.setPath(path);

    return xsd;
  }

  private XSD xsd (String path, List<String> impls) {

    XSD xsd = new XSD();
    xsd.setPath(path);
    xsd.setImpls(impls);

    return xsd;
  }

  public void testValidDocumentPasses ()
    throws Exception {

    mojo("schema.xsd", List.of(xsd("valid.xml"))).execute();
  }

  @Test(expectedExceptions = MojoExecutionException.class)
  public void testInvalidDocumentFails ()
    throws Exception {

    mojo("schema.xsd", List.of(xsd("invalid.xml"))).execute();
  }

  @Test(expectedExceptions = MojoExecutionException.class)
  public void testMissingW3cResourceFails ()
    throws Exception {

    mojo("does-not-exist.xsd", List.of(xsd("valid.xml"))).execute();
  }

  @Test(expectedExceptions = MojoExecutionException.class)
  public void testMissingSchemaResourceFails ()
    throws Exception {

    mojo("schema.xsd", List.of(xsd("does-not-exist.xml"))).execute();
  }

  public void testNullSchemasPasses ()
    throws Exception {

    mojo("schema.xsd", null).execute();
  }

  public void testSchemaWithNullImplsPasses ()
    throws Exception {

    mojo("permissive-w3c.xsd", List.of(xsd("note.xsd"))).execute();
  }

  public void testSchemaWithEmptyImplsPasses ()
    throws Exception {

    mojo("permissive-w3c.xsd", List.of(xsd("note.xsd", List.of()))).execute();
  }

  public void testValidImplAgainstSchemaPasses ()
    throws Exception {

    mojo("permissive-w3c.xsd", List.of(xsd("note.xsd", List.of("valid.xml")))).execute();
  }

  @Test(expectedExceptions = MojoExecutionException.class)
  public void testInvalidImplAgainstSchemaFails ()
    throws Exception {

    mojo("permissive-w3c.xsd", List.of(xsd("note.xsd", List.of("invalid.xml")))).execute();
  }

  @Test(expectedExceptions = MojoExecutionException.class)
  public void testMissingImplResourceFails ()
    throws Exception {

    mojo("permissive-w3c.xsd", List.of(xsd("note.xsd", List.of("does-not-exist.xml")))).execute();
  }

  @Test(expectedExceptions = MojoExecutionException.class)
  public void testMalformedSchemaFails ()
    throws Exception {

    mojo("permissive-w3c.xsd", List.of(xsd("malformed.xsd", List.of("valid.xml")))).execute();
  }
}
