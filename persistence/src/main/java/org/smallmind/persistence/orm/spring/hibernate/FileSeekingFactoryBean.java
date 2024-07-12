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
package org.smallmind.persistence.orm.spring.hibernate;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

public class FileSeekingFactoryBean implements FactoryBean<Resource[]> {

  private FileSeekingBeanFactoryPostProcessor fileSeekingBeanFactoryPostProcessor;
  private Resource[] additionalResources;
  private String sessionSourceKey;

  public void setFileSeekingBeanFactoryPostProcessor (FileSeekingBeanFactoryPostProcessor fileSeekingBeanFactoryPostProcessor) {

    this.fileSeekingBeanFactoryPostProcessor = fileSeekingBeanFactoryPostProcessor;
  }

  public void setSessionSourceKey (String sessionSourceKey) {

    this.sessionSourceKey = sessionSourceKey;
  }

  public void setAdditionalResources (Resource[] additionalResources) {

    this.additionalResources = additionalResources;
  }

  public Resource[] getObject () {

    Resource[] processedResources;

    processedResources = (fileSeekingBeanFactoryPostProcessor == null) ? new Resource[0] : fileSeekingBeanFactoryPostProcessor.getResources(sessionSourceKey);

    if ((additionalResources == null) || additionalResources.length == 0) {

      return processedResources;
    }

    Resource[] combinedResources = new Resource[processedResources.length + additionalResources.length];

    System.arraycopy(processedResources, 0, combinedResources, 0, processedResources.length);
    System.arraycopy(additionalResources, 0, combinedResources, processedResources.length, additionalResources.length);

    return combinedResources;
  }

  public Class getObjectType () {

    return Resource[].class;
  }

  public boolean isSingleton () {

    return true;
  }
}
