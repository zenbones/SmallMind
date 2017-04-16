/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.spark.singularity.boot;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class SingularityIndex implements Serializable {

  private HashMap<String, String> inverseEntryMap = new HashMap<>();
  private HashSet<String> fileNameSet = new HashSet<>();

  public void addInverseJarEntry (String entryName, String jarName) {

    inverseEntryMap.put(entryName, jarName);
  }

  public void addFileName (String fileName) {

    fileNameSet.add(fileName);
  }

  public Iterable<URLEntry> getJarURLEntryIterable (String parentJarUrlPart) {

    return new JarURLIterator(parentJarUrlPart);
  }

  public Iterable<URLEntry> getSingularityURLEntryIterable (String parentJarUrlPart) {

    return new SingularityURLIterator(parentJarUrlPart);
  }

  public static class URLEntry {

    private final URL entryURL;
    private final String entryName;

    public URLEntry (String entryName, URL entryURL) {

      this.entryName = entryName;
      this.entryURL = entryURL;
    }

    public String getEntryName () {

      return entryName;
    }

    public URL getEntryURL () {

      return entryURL;
    }
  }

  private class JarURLIterator implements Iterator<URLEntry>, Iterable<URLEntry> {

    private final Iterator<String> fileNameIter = fileNameSet.iterator();
    private final String parentJarUrlPart;

    public JarURLIterator (String parentJarUrlPart) {

      this.parentJarUrlPart = parentJarUrlPart;
    }

    @Override
    public Iterator<URLEntry> iterator () {

      return this;
    }

    @Override
    public boolean hasNext () {

      return fileNameIter.hasNext();
    }

    @Override
    public URLEntry next () {

      try {

        String fileName = fileNameIter.next();

        return new URLEntry(fileName, new URL("jar", "localhost", parentJarUrlPart + "!/" + fileName));
      } catch (MalformedURLException malformedURLException) {
        throw new RuntimeException(malformedURLException);
      }
    }

    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }

  private class SingularityURLIterator implements Iterator<URLEntry>, Iterable<URLEntry> {

    private final Iterator<Map.Entry<String, String>> inverseEntryIter = inverseEntryMap.entrySet().iterator();
    private final String parentJarUrlPart;

    public SingularityURLIterator (String parentJarUrlPart) {

      this.parentJarUrlPart = parentJarUrlPart;
    }

    @Override
    public Iterator<URLEntry> iterator () {

      return this;
    }

    @Override
    public boolean hasNext () {

      return inverseEntryIter.hasNext();
    }

    @Override
    public URLEntry next () {

      try {

        Map.Entry<String, String> inverseEntry = inverseEntryIter.next();

        return new URLEntry(inverseEntry.getKey(), new URL("singularity", "localhost", parentJarUrlPart + "!!/META-INF/singularity/" + inverseEntry.getValue() + "!/" + inverseEntry.getKey()));
      } catch (MalformedURLException malformedURLException) {
        throw new RuntimeException(malformedURLException);
      }
    }

    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }
}
