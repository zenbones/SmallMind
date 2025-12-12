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

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;

/**
 * Dynamic MBean representing a meter; attributes correspond to meter quantities.
 */
public class MeterDynamicMbean implements DynamicMBean {

  private final MBeanInfo mBeanInfo;
  private final HashSet<String> attributeNameSet = new HashSet<>();
  private final ConcurrentHashMap<String, Double> valueMap = new ConcurrentHashMap<>();

  /**
   * Builds an MBean definition from the meter name, tags, and quantities.
   *
   * @param name       meter name
   * @param tags       associated tags for description purposes
   * @param quantities quantities defining the exposed attributes
   */
  public MeterDynamicMbean (String name, Tag[] tags, Quantity[] quantities) {

    MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[(quantities == null) ? 0 : quantities.length];
    StringBuilder descriptionBuilder = new StringBuilder("Meter MBean(name=").append(name).append(", tags[");

    if ((tags != null) && (tags.length > 0)) {

      boolean first = true;

      for (Tag tag : tags) {
        if (!first) {
          descriptionBuilder.append(", ");
        }
        descriptionBuilder.append(tag.getKey()).append("=").append(tag.getValue());
        first = false;
      }
    }
    descriptionBuilder.append("])");

    if ((quantities != null) && (quantities.length > 0)) {

      int index = 0;

      for (Quantity quantity : quantities) {
        attributeNameSet.add(quantity.getName());
        attributeInfos[index++] = new MBeanAttributeInfo(quantity.getName(), Double.class.getName(), quantity.getName(), true, false, false);
      }
    }

    mBeanInfo = new MBeanInfo(MeterDynamicMbean.class.getName(), descriptionBuilder.toString(), attributeInfos, null, null, null);
  }

  /**
   * Returns the value of a single attribute or throws if unknown.
   *
   * @param attribute attribute name
   * @return attribute value (defaults to 0 when unset)
   * @throws AttributeNotFoundException when the attribute is not defined
   */
  @Override
  public Object getAttribute (String attribute)
    throws AttributeNotFoundException {

    if (attributeNameSet.contains(attribute)) {

      Double value;

      return ((value = valueMap.get(attribute)) == null) ? 0 : value;
    } else {
      throw new AttributeNotFoundException(attribute);
    }
  }

  /**
   * Sets a single attribute value, accepting numeric or string representations.
   *
   * @param attribute attribute to set
   * @throws AttributeNotFoundException   when the attribute is not defined
   * @throws InvalidAttributeValueException when the value cannot be converted to double
   */
  @Override
  public void setAttribute (Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException {

    if (attributeNameSet.contains(attribute.getName())) {
      if (Number.class.isAssignableFrom(attribute.getValue().getClass())) {
        valueMap.put(attribute.getName(), ((Number)attribute.getValue()).doubleValue());
      } else if (String.class.equals(attribute.getValue().getClass())) {
        valueMap.put(attribute.getName(), Double.parseDouble((String)attribute.getValue()));
      } else {
        throw new InvalidAttributeValueException("Requires a double value");
      }
    } else {
      throw new AttributeNotFoundException(attribute.getName());
    }
  }

  /**
   * Returns a list of the requested attributes that currently have values.
   *
   * @param attributes attribute names
   * @return list of attributes with values
   */
  @Override
  public AttributeList getAttributes (String[] attributes) {

    AttributeList attributeList = new AttributeList();

    for (String attribute : attributes) {

      Double value;

      if ((value = valueMap.get(attribute)) != null) {
        attributeList.add(new Attribute(attribute, value));
      }
    }

    return attributeList;
  }

  /**
   * Sets multiple attributes, ignoring unknown names and returning those applied.
   *
   * @param attributes attributes to set
   * @return list of attributes that were applied
   */
  @Override
  public AttributeList setAttributes (AttributeList attributes) {

    AttributeList setAttributeList = new AttributeList();

    for (Object obj : attributes) {
      if ((obj instanceof Attribute) && attributeNameSet.contains(((Attribute)obj).getName())) {
        if (double.class.equals(((Attribute)obj).getValue().getClass())) {
          setAttributeList.add(obj);
          valueMap.put(((Attribute)obj).getName(), (double)((Attribute)obj).getValue());
        } else if (Double.class.equals(((Attribute)obj).getValue().getClass())) {
          setAttributeList.add(obj);
          valueMap.put(((Attribute)obj).getName(), (Double)((Attribute)obj).getValue());
        } else if (String.class.equals(((Attribute)obj).getValue().getClass())) {
          setAttributeList.add(obj);
          valueMap.put(((Attribute)obj).getName(), new Double((String)((Attribute)obj).getValue()));
        }
      }
    }

    return setAttributeList;
  }

  /**
   * Invoking operations is not supported.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public Object invoke (String actionName, Object[] params, String[] signature) {

    throw new UnsupportedOperationException(actionName);
  }

  /**
   * @return metadata describing this MBean
   */
  @Override
  public MBeanInfo getMBeanInfo () {

    return mBeanInfo;
  }
}
