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
 * Spring {@link FactoryBean} that builds and vends a singleton {@link CloudWatchLogsClient}
 * authenticated with static AWS credentials and targeting the configured region. The client is
 * constructed once during {@link #afterPropertiesSet()} and returned by every subsequent call to
 * {@link #getObject()}.
 */
public class CloudWatchLogsClientFactory implements FactoryBean<CloudWatchLogsClient>, InitializingBean {

  private CloudWatchLogsClient client;
  private Region region;
  private String awsAccessKey;
  private String awsSecretKey;

  /**
   * Sets the AWS access key ID used to authenticate API requests.
   *
   * @param awsAccessKey the AWS access key ID (the public part of the credential pair)
   */
  public void setAwsAccessKey (String awsAccessKey) {

    this.awsAccessKey = awsAccessKey;
  }

  /**
   * Sets the AWS secret access key used to sign API requests.
   *
   * @param awsSecretKey the AWS secret access key (the private part of the credential pair)
   */
  public void setAwsSecretKey (String awsSecretKey) {

    this.awsSecretKey = awsSecretKey;
  }

  /**
   * Sets the AWS region to which CloudWatch Logs API calls will be directed.
   *
   * @param region the target AWS region (e.g. {@code Region.US_EAST_1})
   */
  public void setRegion (Region region) {

    this.region = region;
  }

  /**
   * Declares that this factory manages a singleton; the same {@link CloudWatchLogsClient} instance
   * is returned on every call to {@link #getObject()}.
   *
   * @return always {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory, used by the Spring container for
   * type-based autowiring.
   *
   * @return {@link CloudWatchLogsClient}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return CloudWatchLogsClient.class;
  }

  /**
   * Returns the {@link CloudWatchLogsClient} instance built by {@link #afterPropertiesSet()}.
   *
   * @return the fully configured, ready-to-use CloudWatch Logs client
   */
  @Override
  public CloudWatchLogsClient getObject () {

    return client;
  }

  /**
   * Constructs the {@link CloudWatchLogsClient} using the configured access key, secret key, and
   * region. Called automatically by the Spring container once all bean properties have been injected.
   *
   * @throws Exception if the client cannot be built (e.g. invalid credentials or region)
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKey, awsSecretKey));

    client = CloudWatchLogsClient.builder().credentialsProvider(credentialsProvider).region(region).build();
  }
}
