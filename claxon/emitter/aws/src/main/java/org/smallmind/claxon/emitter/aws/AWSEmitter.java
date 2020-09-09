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
package org.smallmind.claxon.emitter.aws;

import java.time.Instant;
import java.util.Date;
import org.smallmind.claxon.registry.PushEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

public class AWSEmitter extends PushEmitter {

  private final CloudWatchClient client = null;
  private final String namespace;

  public AWSEmitter (String namespace) {

    this.namespace = namespace;
  }

  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities) {

    Instant now = Instant.now();
    MetricDatum[] data = new MetricDatum[quantities.length];
    Dimension[] dimensions = new Dimension[(tags == null) ? 0 : tags.length];
    int dataIndex = 0;

    if ((tags != null) && (tags.length > 0)) {

      int tagIndex = 0;

      for (Tag tag : tags) {
        dimensions[tagIndex++] = Dimension.builder().name(tag.getKey()).value(tag.getValue()).build();
      }
    }

    for (Quantity quantity : quantities) {
      data[dataIndex++] = MetricDatum.builder().metricName(quantity.getName()).timestamp(now).unit(StandardUnit.NONE).value(quantity.getValue()).dimensions(dimensions).build();
    }

    client.putMetricData(PutMetricDataRequest.builder().namespace(namespace).metricData(data).build());
  }
}
