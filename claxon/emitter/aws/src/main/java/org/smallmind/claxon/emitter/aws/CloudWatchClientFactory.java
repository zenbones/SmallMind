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
package org.smallmind.claxon.emitter.aws;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

/**
 * Spring {@link FactoryBean} that constructs and exposes a singleton {@link CloudWatchClient}
 * configured with static AWS credentials and a target region.
 *
 * <p>Callers should set {@link #setAwsAccessKey(String)}, {@link #setAwsSecretKey(String)},
 * and {@link #setRegion(Region)} before the Spring container invokes
 * {@link #afterPropertiesSet()}, at which point the client is built and cached for the
 * lifetime of the bean.
 */
public class CloudWatchClientFactory implements FactoryBean<CloudWatchClient>, InitializingBean {

  /**
   * The fully-constructed {@link CloudWatchClient} produced by this factory; populated during
   * {@link #afterPropertiesSet()}.
   */
  private CloudWatchClient client;

  /**
   * The AWS region to which the CloudWatch client will connect.
   */
  private Region region;

  /**
   * The AWS access key ID used to authenticate API calls.
   */
  private String awsAccessKey;

  /**
   * The AWS secret access key used to authenticate API calls.
   */
  private String awsSecretKey;

  /**
   * Sets the AWS access key ID used to authenticate requests to CloudWatch.
   *
   * @param awsAccessKey the AWS access key ID; must not be {@code null}
   */
  public void setAwsAccessKey (String awsAccessKey) {

    this.awsAccessKey = awsAccessKey;
  }

  /**
   * Sets the AWS secret access key used to authenticate requests to CloudWatch.
   *
   * @param awsSecretKey the AWS secret access key; must not be {@code null}
   */
  public void setAwsSecretKey (String awsSecretKey) {

    this.awsSecretKey = awsSecretKey;
  }

  /**
   * Sets the AWS region to which the {@link CloudWatchClient} will connect.
   *
   * @param region the target {@link Region}; must not be {@code null}
   */
  public void setRegion (Region region) {

    this.region = region;
  }

  /**
   * Indicates that this factory always produces the same client instance.
   *
   * @return {@code true} because the produced {@link CloudWatchClient} is a singleton
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link CloudWatchClient}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return CloudWatchClient.class;
  }

  /**
   * Returns the singleton {@link CloudWatchClient} built during {@link #afterPropertiesSet()}.
   *
   * @return the configured {@link CloudWatchClient}
   */
  @Override
  public CloudWatchClient getObject () {

    return client;
  }

  /**
   * Constructs the {@link CloudWatchClient} using the configured access key, secret key, and
   * region after all Spring properties have been injected.
   */
  @Override
  public void afterPropertiesSet () {

    AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKey, awsSecretKey));

    client = CloudWatchClient.builder().credentialsProvider(credentialsProvider).region(region).build();
  }
}
