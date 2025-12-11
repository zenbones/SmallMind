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
package org.smallmind.persistence.orm.hibernate;

import java.util.Date;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.DATE;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;

public class H2Dialect extends org.hibernate.dialect.H2Dialect {

  @Override
  public void initializeFunctionRegistry (FunctionContributions functionContributions) {

    super.initializeFunctionRegistry(functionContributions);

    SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
    TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
    BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
    BasicType<Date> timestampType = basicTypeRegistry.resolve(StandardBasicTypes.TIMESTAMP);
    BasicType<Integer> integerType = basicTypeRegistry.resolve(StandardBasicTypes.INTEGER);

    functionRegistry.patternDescriptorBuilder("org_smallmind_day_diff", "timestampdiff(SQL_TSI_DAY, ?1, ?2)")
      .setInvariantType(integerType)
      .setExactArgumentCount(2)
      .setParameterTypes(DATE, DATE)
      .setArgumentListSignature("(DATE first, DATE second)")
      .register();

    functionRegistry.patternDescriptorBuilder("org_smallmind_date_sub_minutes", "timestampadd(SQL_TSI_MINUTE, -?2, ?1)")
      .setInvariantType(timestampType)
      .setExactArgumentCount(2)
      .setParameterTypes(DATE, INTEGER)
      .setArgumentListSignature("(DATE datetime, INTEGER minutes)")
      .register();
  }
}
