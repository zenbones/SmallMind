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
package org.smallmind.claxon.collector.jmx;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import org.smallmind.claxon.registry.Identifier;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;

public class MeterDynamicMbean implements DynamicMBean {

  private final MBeanInfo mBeanInfo;
  private final HashSet<String> attributeNameSet = new HashSet<>();
  private final ConcurrentHashMap<String, Double> valueMap = new ConcurrentHashMap<>();

  public MeterDynamicMbean (Identifier identifier, Tag[] tags, Quantity[] quantities) {

    MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[(quantities == null) ? 0 : quantities.length];
    StringBuilder descriptionBuilder = new StringBuilder("Meter MBean(identifier=").append(identifier).append(", tags[");

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

  @Override
  public Object invoke (String actionName, Object[] params, String[] signature) {

    throw new UnsupportedOperationException(actionName);
  }

  @Override
  public MBeanInfo getMBeanInfo () {

    return mBeanInfo;
  }
}
