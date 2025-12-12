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
package org.smallmind.claxon.emitter.jmx;

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
import org.smallmind.claxon.registry.PushEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;

/**
 * Push emitter that exposes metrics as JMX MBeans and updates their attributes.
 */
public class JMXEmitter extends PushEmitter {

  private final MBeanServer server;

  /**
   * Creates a JMX emitter bound to the provided MBeanServer.
   *
   * @param server MBeanServer to register and update meters on
   */
  public JMXEmitter (MBeanServer server) {

    this.server = server;
  }

  /**
   * Registers a dynamic MBean for the meter if necessary and updates quantity attributes.
   *
   * @param meterName  meter name used as the JMX object name domain
   * @param tags       tags translated to object name properties
   * @param quantities quantities mapped to attributes
   * @throws MalformedObjectNameException   when the object name is invalid
   * @throws NotCompliantMBeanException     when the dynamic MBean does not comply
   * @throws MBeanRegistrationException     when registration fails
   * @throws InstanceAlreadyExistsException when the MBean already exists unexpectedly
   * @throws InstanceNotFoundException      when the MBean cannot be found during update
   * @throws ReflectionException            when attribute setting fails reflectively
   */
  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities)
    throws MalformedObjectNameException, NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, InstanceNotFoundException, ReflectionException {

    ObjectName objectName;
    AttributeList attributeList = new AttributeList();
    Hashtable<String, String> tagTable = new Hashtable<>();

    if ((tags != null) && (tags.length > 0)) {
      for (Tag tag : tags) {
        tagTable.put(tag.getKey(), tag.getValue());
      }
    }

    objectName = new ObjectName(meterName, tagTable);

    if (!server.isRegistered(objectName)) {
      synchronized (server) {
        if (!server.isRegistered(objectName)) {
          server.registerMBean(new MeterDynamicMbean(meterName, tags, quantities), objectName);
        }
      }
    }

    for (Quantity quantity : quantities) {
      attributeList.add(new Attribute(quantity.getName(), quantity.getValue()));
    }

    server.setAttributes(objectName, attributeList);
  }
}
