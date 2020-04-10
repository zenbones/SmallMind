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
package org.smallmind.claxon.registry.jmx;

import java.util.Hashtable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.smallmind.claxon.meter.Domain;
import org.smallmind.claxon.meter.Identifier;
import org.smallmind.claxon.meter.Quantity;
import org.smallmind.claxon.meter.Recorder;
import org.smallmind.claxon.meter.Tag;

public class JMXRecorder implements Recorder {

  private final MBeanServer server;

  public JMXRecorder (MBeanServer server) {

    this.server = server;
  }

  @Override
  public void record (Identifier identifier, Tag[] tags, Quantity[] quantities)
    throws MalformedObjectNameException, NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, InstanceNotFoundException, ReflectionException {

    if ((quantities != null) && (quantities.length > 0)) {

      ObjectName objectName;
      AttributeList attributeList = new AttributeList();
      Hashtable<String, String> tagTable = new Hashtable<>();

      if ((tags != null) && (tags.length > 0)) {
        for (Tag tag : tags) {
          tagTable.put(tag.getKey(), tag.getValue());
        }
      }

      objectName = new ObjectName(identifier.getName(), tagTable);

      if (!server.isRegistered(objectName)) {
        synchronized (server) {
          if (!server.isRegistered(objectName)) {
            server.registerMBean(new MeterDynamicMbean(new Domain("domain"), identifier, tags, quantities), objectName);
          }
        }
      }

      for (Quantity quantity : quantities) {
        attributeList.add(new Attribute(quantity.getName(), quantity.getValue()));
      }

      server.setAttributes(objectName, attributeList);
    }
  }
}
