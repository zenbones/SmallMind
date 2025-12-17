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
package org.smallmind.scribe.pen;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

/**
 * Spring factory bean for constructing an AWS CloudWatch Logs client with static credentials.
 */
public class CloudWatchLogsClientFactory implements FactoryBean<CloudWatchLogsClient>, InitializingBean {

  private CloudWatchLogsClient client;
  private Region region;
  private String awsAccessKey;
  private String awsSecretKey;

  /**
   * Sets the AWS access key.
   *
   * @param awsAccessKey access key id
   */
  public void setAwsAccessKey (String awsAccessKey) {

    this.awsAccessKey = awsAccessKey;
  }

  /**
   * Sets the AWS secret key.
   *
   * @param awsSecretKey secret access key
   */
  public void setAwsSecretKey (String awsSecretKey) {

    this.awsSecretKey = awsSecretKey;
  }

  /**
   * Sets the AWS region.
   *
   * @param region AWS region
   */
  public void setRegion (Region region) {

    this.region = region;
  }

  /**
   * Indicates the factory produces a singleton instance.
   *
   * @return always {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link CloudWatchLogsClient} class
   */
  @Override
  public Class<?> getObjectType () {

    return CloudWatchLogsClient.class;
  }

  /**
   * Provides the created {@link CloudWatchLogsClient} instance.
   *
   * @return configured client
   */
  @Override
  public CloudWatchLogsClient getObject () {

    return client;
  }

  /**
   * Builds the CloudWatch Logs client after required properties are set.
   *
   * @throws Exception if client construction fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKey, awsSecretKey));

    client = CloudWatchLogsClient.builder().credentialsProvider(credentialsProvider).region(region).build();
  }
}
