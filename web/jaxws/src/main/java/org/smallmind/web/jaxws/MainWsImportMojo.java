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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "wsimport", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class MainWsImportMojo extends WsImportMojo {

  /**
   * Specify where to place output generated classes. Use <code>xnocompile</code>
   * to turn this off.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}")
  private File destDir;

  /**
   * Specify where to place generated source files, keep is turned on with this option.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/wsimport")
  private File sourceDestDir;

  /**
   * Specify where to generate JWS implementation file.
   */
  @Parameter(defaultValue = "${project.build.sourceDirectory}")
  private File implDestDir;

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
  protected File getDefaultSrcOut () {

    return new File(project.getBuild().getDirectory(), "generated-sources/wsimport");
  }

  @Override
  protected File getImplDestDir () {

    return implDestDir;
  }
}