/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

// Validates XML against W3C XSD
@Mojo(name = "validate-xml", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class XMLValidationMojo extends AbstractMojo {

  private static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
  private static final ValidationErrorHandler ERROR_HANDLER = new ValidationErrorHandler();

  @Parameter(readonly = true, property = "project.resources")
  private List<Resource> projectResources;

  @Parameter(required = true)
  private String w3c;

  @Parameter
  private List<XSD> schemas;

  public void execute ()
    throws MojoExecutionException {

    Validator w3cSchemaValidator;
    Validator schemaValidator;
    Path w3cSchemaFile;
    Path schemaFile;
    Path xmlFile;

    try {
      if ((w3cSchemaFile = getResourceFile(w3c)) == null) {
        throw new MojoExecutionException("Could not find the w3c schema for schemas at path(" + w3c + ") in this projects resources");
      }

      w3cSchemaValidator = SCHEMA_FACTORY.newSchema(new StreamSource(Files.newInputStream(w3cSchemaFile))).newValidator();
      w3cSchemaValidator.setErrorHandler(ERROR_HANDLER);

      if (schemas != null) {
        for (XSD xsd : schemas) {
          System.out.println("validating [xsd] (" + xsd.getPath() + ")...");

          if ((schemaFile = getResourceFile(xsd.getPath())) == null) {
            throw new MojoExecutionException("Could not find xsd at path(" + xsd.getPath() + ") in this projects resources");
          }
          w3cSchemaValidator.validate(new StreamSource(Files.newInputStream(schemaFile)));

          if ((xsd.getImpls() != null) && (!xsd.getImpls().isEmpty())) {
            schemaValidator = SCHEMA_FACTORY.newSchema(new StreamSource(Files.newInputStream(schemaFile))).newValidator();

            for (String xmlPath : xsd.getImpls()) {
              System.out.println("validating [xml] (" + xmlPath + ")...");

              if ((xmlFile = getResourceFile(xmlPath)) == null) {
                throw new MojoExecutionException("Could not find xml at path(" + xmlPath + ") in this projects resources");
              }
              schemaValidator.validate(new StreamSource(Files.newInputStream(xmlFile)));
            }
          }
        }
      }
    } catch (MojoExecutionException mojoExecutionException) {
      throw mojoExecutionException;
    } catch (Exception exception) {
      throw new MojoExecutionException(exception.getMessage(), exception);
    }
  }

  private Path getResourceFile (String resourceName) {

    Path schemaFile;

    for (Resource projectResource : projectResources) {
      schemaFile = Paths.get(projectResource.getDirectory(), resourceName);
      if (Files.isRegularFile(schemaFile)) {
        return schemaFile;
      }
    }

    return null;
  }

  private static class ValidationErrorHandler extends DefaultHandler {

    public void fatalError (SAXParseException saxException)
      throws SAXException {

      System.out.println("[fatal error] " + saxException.getMessage());
      throw saxException;
    }

    public void error (SAXParseException saxException)
      throws SAXException {

      System.out.println("[error] " + saxException.getMessage());
      throw saxException;
    }

    public void warning (SAXParseException saxException)
      throws SAXException {

      System.out.println("[warning] " + saxException.getMessage());
      throw saxException;
    }
  }
}
