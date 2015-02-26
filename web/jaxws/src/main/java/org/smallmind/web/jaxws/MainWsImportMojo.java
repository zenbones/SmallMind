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