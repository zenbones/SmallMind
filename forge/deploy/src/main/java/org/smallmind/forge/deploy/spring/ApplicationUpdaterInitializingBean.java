/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.forge.deploy.spring;

import java.io.File;
import org.smallmind.forge.deploy.ApplicationUpdater;
import org.smallmind.forge.deploy.Decorator;
import org.smallmind.forge.deploy.OperatingSystem;
import org.smallmind.forge.deploy.Repository;
import org.springframework.beans.factory.InitializingBean;

public class ApplicationUpdaterInitializingBean implements InitializingBean {

  private Decorator[] decorators;
  private File installDir;
  private OperatingSystem operatingSystem;
  private Repository repository;
  private String[] envVars;
  private String appUser;
  private String nexusHost;
  private String nexusUser;
  private String nexusPassword;
  private String groupId;
  private String artifactId;
  private String version;
  private String classifier;
  private String extension;

  public void setOperatingSystem (OperatingSystem operatingSystem) {

    this.operatingSystem = operatingSystem;
  }

  public void setInstallDir (File installDir) {

    this.installDir = installDir;
  }

  public void setAppUser (String appUser) {

    this.appUser = appUser;
  }

  public void setNexusHost (String nexusHost) {

    this.nexusHost = nexusHost;
  }

  public void setNexusUser (String nexusUser) {

    this.nexusUser = nexusUser;
  }

  public void setNexusPassword (String nexusPassword) {

    this.nexusPassword = nexusPassword;
  }

  public void setRepository (Repository repository) {

    this.repository = repository;
  }

  public void setGroupId (String groupId) {

    this.groupId = groupId;
  }

  public void setArtifactId (String artifactId) {

    this.artifactId = artifactId;
  }

  public void setVersion (String version) {

    this.version = version;
  }

  public void setClassifier (String classifier) {

    this.classifier = classifier;
  }

  public void setExtension (String extension) {

    this.extension = extension;
  }

  public void setEnvVars (String[] envVars) {

    this.envVars = envVars;
  }

  public void setDecorators (Decorator[] decorators) {

    this.decorators = decorators;
  }

  @Override
  public void afterPropertiesSet ()
    throws Exception {

    ApplicationUpdater.update(operatingSystem, appUser, installDir, nexusHost, nexusUser, nexusPassword, repository, groupId, artifactId, version, classifier, extension, envVars, decorators);
  }
}
