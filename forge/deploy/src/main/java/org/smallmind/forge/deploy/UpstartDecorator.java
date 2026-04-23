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
package org.smallmind.forge.deploy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

/**
 * {@link Decorator} that generates a Linux Upstart installation script for the deployed
 * application. The script is produced by processing a FreeMarker template loaded from the
 * classpath and is written to {@code <installPath>/<artifactId>/bin/<artifactId>.install}.
 * This decorator is a no-op when targeting any platform other than Linux.
 */
public class UpstartDecorator implements Decorator {

  private static final Template UPSTART_TEMPLATE;

  static {

    Configuration freemarkerConf = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

    freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);

    try {
      UPSTART_TEMPLATE = new Template("upstart template", new InputStreamReader(UpstartDecorator.class.getClassLoader().getResourceAsStream("org/smallmind/forge/deploy/upstart-install.freemarker.in")), freemarkerConf);
    } catch (IOException ioException) {
      throw new StaticInitializationError(ioException);
    }
  }

  /**
   * Generate the Upstart installation script for a Linux deployment.
   *
   * <p>Populates a FreeMarker template with the application's metadata and writes the result to
   * the {@code bin} subdirectory of the installed application. Has no effect when
   * {@code operatingSystem} is not {@link OperatingSystem#LINUX}.
   *
   * @param operatingSystem only {@link OperatingSystem#LINUX} triggers script generation
   * @param appUser         OS account owning the installation (passed to the template context)
   * @param installPath     root installation directory used to construct the output file path
   * @param nexusHost       Nexus hostname (available in the template context)
   * @param nexusUser       Nexus username (available in the template context)
   * @param nexusPassword   Nexus password (available in the template context)
   * @param repository      repository from which the artifact was retrieved (available in the template context)
   * @param groupId         Maven {@code groupId} (available in the template context)
   * @param artifactId      Maven {@code artifactId}; used to name the output file
   * @param version         deployed version (available in the template context)
   * @param classifier      artifact classifier (available in the template context)
   * @param extension       artifact extension (available in the template context)
   * @param envVars         environment variable declarations injected into the generated script
   * @throws IOException       if the output file cannot be created or written
   * @throws TemplateException if FreeMarker template interpolation fails
   */
  public void decorate (OperatingSystem operatingSystem, String appUser, Path installPath, String nexusHost, String nexusUser, String nexusPassword, Repository repository, String groupId, String artifactId, String version, String classifier, String extension, String... envVars)
    throws IOException, TemplateException {

    if (operatingSystem.equals(OperatingSystem.LINUX)) {

      HashMap<String, Object> interpolationMap = new HashMap<>();

      interpolationMap.put("artifactId", artifactId);
      interpolationMap.put("applicationName", artifactId.toUpperCase());
      interpolationMap.put("installDir", installPath.toAbsolutePath());
      interpolationMap.put("batchExtension", operatingSystem.getBatchExtension());
      interpolationMap.put("envVars", envVars);

      UPSTART_TEMPLATE.process(interpolationMap, Files.newBufferedWriter(installPath.resolve(artifactId).resolve("bin").resolve(artifactId + ".install"), StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }
  }
}
