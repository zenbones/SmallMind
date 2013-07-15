/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.cloud.multicast.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.TreeMap;
import org.smallmind.nutsnbolts.util.SelfDestructive;

public class EventMessageMold implements SelfDestructive {

  private TreeMap<Integer, byte[]> messageSegmentMap;
  private int messageLength = 0;
  private int totalBytes = 0;

  public EventMessageMold () {

    messageSegmentMap = new TreeMap<Integer, byte[]>();
  }

  @Override
  public void destroy () {

  }

  public void setMessageLength (int messageLength) {

    this.messageLength = messageLength;
  }

  public void addData (int messageIndex, byte[] messageSegment) {

    messageSegmentMap.put(messageIndex, messageSegment);
    totalBytes += messageSegment.length;
  }

  public boolean isComplete () {

    return (messageLength != 0) && (totalBytes >= messageLength);
  }

  public Object unmoldMessageBody ()
    throws IOException, ClassNotFoundException {

    Object unmoldedObject;
    ObjectInputStream objectInputStream;
    ByteArrayOutputStream byteOutputStream;
    Iterator<Integer> segmentKeyIter;

    if (totalBytes == 0) {
      return null;
    }

    byteOutputStream = new ByteArrayOutputStream(totalBytes);
    segmentKeyIter = messageSegmentMap.keySet().iterator();
    while (segmentKeyIter.hasNext()) {
      byteOutputStream.write(messageSegmentMap.get(segmentKeyIter.next()));
    }
    byteOutputStream.close();

    objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteOutputStream.toByteArray()));
    unmoldedObject = objectInputStream.readObject();
    objectInputStream.close();

    return unmoldedObject;
  }
}
