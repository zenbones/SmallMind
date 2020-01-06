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
package org.smallmind.web.jetty;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import org.eclipse.jetty.util.resource.Resource;

public class ByteArrayResource extends Resource {

  private byte[] bytes;

  public ByteArrayResource (byte[] bytes) {

    this.bytes = bytes;
  }

  @Override
  public boolean isContainedIn (Resource resource) {

    return false;
  }

  @Override
  public void close () {

  }

  @Override
  public boolean exists () {

    return true;
  }

  @Override
  public boolean isDirectory () {

    return false;
  }

  @Override
  public long lastModified () {

    return -1;
  }

  @Override
  public long length () {

    return bytes.length;
  }

  @Override
  public URL getURL () {

    return null;
  }

  @Override
  public File getFile () {

    return null;
  }

  @Override
  public String getName () {

    return null;
  }

  @Override
  public InputStream getInputStream ()  {

    return new ByteArrayInputStream(bytes);
  }

  @Override
  public ReadableByteChannel getReadableByteChannel () {

    return null;
  }

  @Override
  public boolean delete () throws SecurityException {

    return false;
  }

  @Override
  public boolean renameTo (Resource resource) throws SecurityException {

    return false;
  }

  @Override
  public String[] list () {

    return new String[0];
  }

  @Override
  public Resource addPath (String s) {

    return null;
  }
}
