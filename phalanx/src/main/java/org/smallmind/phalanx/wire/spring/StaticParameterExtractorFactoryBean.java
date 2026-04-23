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
package org.smallmind.phalanx.wire.spring;

import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that constructs a {@link StaticParameterExtractor} from a fixed string value
 * configured via {@link #setParameter(String)}.
 */
public class StaticParameterExtractorFactoryBean implements InitializingBean, FactoryBean<StaticParameterExtractor<?>> {

  private StaticParameterExtractor<?> staticParameterExtractor;
  private String parameter;

  /**
   * Sets the constant string value that the produced extractor always returns.
   *
   * @param parameter static value to embed in the extractor
   */
  public void setParameter (String parameter) {

    this.parameter = parameter;
  }

  /**
   * Constructs the {@link StaticParameterExtractor} after all properties have been set.
   *
   * @throws Exception if extractor construction fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    staticParameterExtractor = new StaticParameterExtractor<>(parameter);
  }

  /**
   * Returns the constructed {@link StaticParameterExtractor}.
   *
   * @return the singleton extractor instance
   */
  @Override
  public StaticParameterExtractor<?> getObject () {

    return staticParameterExtractor;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link StaticParameterExtractor}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return StaticParameterExtractor.class;
  }

  /**
   * Indicates that this factory always returns the same extractor instance.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }
}
