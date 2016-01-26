/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.web.jaxws;

import java.io.File;
import java.io.IOException;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

@Mojo(name = "wsgen", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class MainWsGenMojo extends AbstractWsGenMojo {

  /**
   * Specify where to place output generated classes. Use <code>xnocompile</code>
   * to turn this off.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}")
  private File destDir;

  /**
   * Specify where to place generated source files, keep is turned on with this option.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/wsgen")
  private File sourceDestDir;

  /**
   * Directory containing the generated wsdl files.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/wsdl")
  private File resourceDestDir;

  /**
   * Either ${build.outputDirectory} or ${build.testOutputDirectory}.
   */
  @Override
  protected File getDestDir () {

    return destDir;
  }

  @Override
  protected File getSourceDestDir () {

    return sourceDestDir;
  }

  @Override
  protected File getResourceDestDir () {

    return resourceDestDir;
  }

  @Override
  protected File getDefaultSrcOut () {

    return new File(project.getBuild().getDirectory(), "generated-sources/wsgen");
  }

  @Override
  protected File getClassesDir () {

    return new File(project.getBuild().getOutputDirectory());
  }

  @Override
  public void execute () throws MojoExecutionException, MojoFailureException {

    super.execute();
    if (genWsdl) {
      try {
        attachWsdl();
      }
      catch (IOException ex) {
        throw new MojoExecutionException("Failed to execute wsgen", ex);
      }
    }

  }

  private void attachWsdl () throws IOException {

    File target = new File(project.getBuild().getDirectory());
    if (!"war".equalsIgnoreCase(project.getPackaging())) {
      // META-INF/wsdl for jar etc packagings
      target = new File(project.getBuild().getOutputDirectory(), "META-INF/wsdl");
    }
    else {
      // WEB-INF/wsdl for war
      String targetPath = null;
      Plugin war = project.getBuild().getPluginsAsMap().get("org.apache.maven.plugins:maven-war-plugin");
      for (PluginExecution exec : war.getExecutions()) {
        //check execution/configuration
        String s = getWebappDirectory(exec.getConfiguration());
        if (s != null) {
          targetPath = s;
          break;
        }
      }
      if (targetPath == null) {
        //check global plugin configuration
        targetPath = getWebappDirectory(war.getConfiguration());
      }
      target = targetPath != null ? new File(targetPath) : new File(target, project.getBuild().getFinalName());
      target = new File(target, "WEB-INF/wsdl");
    }
    if (!target.mkdirs() && !target.exists()) {
      getLog().warn("Cannot create directory: " + target.getAbsolutePath());
    }
    getLog().debug("Packaging WSDL(s) to: " + target);
    FileUtils.copyDirectory(getResourceDestDir(), target);
  }

  private String getWebappDirectory (Object conf) {

    if (conf == null) {
      return null;
    }
    Xpp3Dom el = ((Xpp3Dom)conf).getChild("webappDirectory");
    return el != null ? el.getValue() : null;
  }

}