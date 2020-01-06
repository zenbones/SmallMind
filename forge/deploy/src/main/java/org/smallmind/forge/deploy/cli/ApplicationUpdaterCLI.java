/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.forge.deploy.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.smallmind.forge.deploy.ApplicationUpdater;
import org.smallmind.forge.deploy.Decorator;
import org.smallmind.forge.deploy.OperatingSystem;
import org.smallmind.forge.deploy.Repository;
import org.smallmind.nutsnbolts.command.CommandLineException;
import org.smallmind.nutsnbolts.command.CommandLineParser;
import org.smallmind.nutsnbolts.command.OptionSet;
import org.smallmind.nutsnbolts.command.template.Template;

public class ApplicationUpdaterCLI {

  public static void main (String... args)
    throws Exception {

    Template template = Template.createTemplate(ApplicationUpdaterCLI.class);

    try {

      OptionSet optionSet;

      optionSet = CommandLineParser.parseCommands(template, args);

      Decorator[] decorators = null;
      Path installPath = Paths.get(optionSet.getArgument("install-dir", 'i'));
      OperatingSystem operatingSystem = optionSet.containsOption("os", 'o') ? OperatingSystem.fromCode(optionSet.getArgument("os", 'o')) : OperatingSystem.LINUX;
      Repository repository = optionSet.containsOption("repository", 'r') ? Repository.fromCode(optionSet.getArgument("repository", 'r')) : Repository.RELEASES;
      String[] decoratorClassNames = optionSet.getArguments("decorators", 'd');
      String[] envVars = optionSet.getArguments("env", 'x');
      String appUser = optionSet.getArgument("app-user", 'u');
      String nexusHost = optionSet.getArgument("nexus-host", 'h');
      String nexusUser = optionSet.getArgument("nexus-user", 'u');
      String nexusPassword = optionSet.getArgument("nexus-password", 'p');
      String groupId = optionSet.getArgument("group-id", 'g');
      String artifactId = optionSet.getArgument("artifact-id", 'a');
      String version = optionSet.getArgument("version", 'v');
      String classifier = optionSet.getArgument("classifier", 'c');
      String extension = optionSet.containsOption("extension", 'e') ? optionSet.getArgument("extension", 'e') : "jar";
      boolean progressBar = (!optionSet.containsOption("progress-bar", 'b')) || (Boolean.parseBoolean(optionSet.getArgument("progress-bar", 'b')));

      if ((decoratorClassNames != null) && (decoratorClassNames.length > 0)) {

        decorators = new Decorator[decoratorClassNames.length];
        int index = 0;

        for (String decoratorClassName : decoratorClassNames) {
          decorators[index++] = (Decorator)Class.forName(decoratorClassName).newInstance();
        }
      }

      ApplicationUpdater.update(operatingSystem, appUser, installPath, progressBar, nexusHost, nexusUser, nexusPassword, repository, groupId, artifactId, version, classifier, extension, envVars, decorators);
    } catch (CommandLineException commandLineException) {
      System.out.println(commandLineException.getMessage());
      System.out.println(template);
    }
  }
}
