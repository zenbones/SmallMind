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
package org.smallmind.spark.singularity.boot;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Serializable manifest of the contents of a Singularity bundle. The Maven plugin builds an instance at packaging
 * time, writes it to {@code META-INF/singularity/index/singularity.idx}, and {@link SingularityClassLoader} reads it
 * at boot to learn how to form a URL for each class and resource.
 * <p>The index distinguishes two populations:
 * <ul>
 *   <li>Bare files laid down directly in the outer jar (boot classes, user classes, indexed resources).</li>
 *   <li>Entries drawn from bundled libraries, each pointing back to the nested jar that supplied it.</li>
 * </ul>
 */
public class SingularityIndex implements Serializable {

  private final HashMap<String, String> inverseEntryMap = new HashMap<>();
  private final HashSet<String> fileNameSet = new HashSet<>();

  /**
   * Records that an entry observed inside a bundled library jar should be served from that jar at runtime.
   *
   * @param entryName the path of the entry as it appears inside the library jar
   * @param jarName   the filename of the library jar under {@code META-INF/singularity/lib/}
   */
  public void addInverseJarEntry (String entryName, String jarName) {

    inverseEntryMap.put(entryName, jarName);
  }

  /**
   * Records a file laid down directly inside the outer Singularity jar.
   *
   * @param fileName resource-style path (forward slashes) of the file relative to the jar root
   */
  public void addFileName (String fileName) {

    fileNameSet.add(fileName);
  }

  /**
   * Exposes every directly stored file as a {@link URLEntry} whose URL uses the standard {@code jar:} protocol.
   *
   * @param parentJarUrlPart the external form of the enclosing jar's URL, used as the prefix for each entry
   * @return an {@link Iterable} that yields one URL entry per file, in arbitrary iteration order
   */
  public Iterable<URLEntry> getJarURLEntryIterable (String parentJarUrlPart) {

    return new JarURLIterator(parentJarUrlPart);
  }

  /**
   * Exposes every library-jar entry as a {@link URLEntry} whose URL uses the {@code singularity:} protocol so that
   * the custom connection can resolve it through the nested jar.
   *
   * @param parentJarUrlPart the external form of the enclosing jar's URL, used as the prefix for each entry
   * @return an {@link Iterable} that yields one URL entry per library-sourced resource, in arbitrary iteration order
   */
  public Iterable<URLEntry> getSingularityURLEntryIterable (String parentJarUrlPart) {

    return new SingularityURLIterator(parentJarUrlPart);
  }

  private class JarURLIterator implements Iterator<URLEntry>, Iterable<URLEntry> {

    private final Iterator<String> fileNameIter = fileNameSet.iterator();
    private final String parentJarUrlPart;

    /**
     * Captures the parent jar URL that will prefix each {@code jar:}-protocol URL produced by this iterator.
     *
     * @param parentJarUrlPart external form of the outer jar URL
     */
    public JarURLIterator (String parentJarUrlPart) {

      this.parentJarUrlPart = parentJarUrlPart;
    }

    /**
     * Lets this type serve as its own iterator.
     *
     * @return {@code this}
     */
    @Override
    public Iterator<URLEntry> iterator () {

      return this;
    }

    /**
     * Indicates whether another file name remains to be converted into a {@link URLEntry}.
     *
     * @return {@code true} when at least one unvisited file name remains
     */
    @Override
    public boolean hasNext () {

      return fileNameIter.hasNext();
    }

    /**
     * Produces the next {@link URLEntry} using a standard {@code jar:} URL that references the file in the outer jar.
     *
     * @return a URL entry whose name is the file path and whose URL targets that file inside the outer jar
     * @throws RuntimeException wrapping a {@link URISyntaxException} or {@link MalformedURLException} when the
     *                          composed URL is not well formed
     */
    @Override
    public URLEntry next () {

      try {

        String fileName = fileNameIter.next();

        return new URLEntry(fileName, new URI("jar", parentJarUrlPart + "!/" + fileName, null).toURL());
      } catch (URISyntaxException | MalformedURLException exception) {
        throw new RuntimeException(exception);
      }
    }

    /**
     * Removal is not meaningful because the backing set is treated as immutable once the index has been written.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }

  private class SingularityURLIterator implements Iterator<URLEntry>, Iterable<URLEntry> {

    private final Iterator<Map.Entry<String, String>> inverseEntryIter = inverseEntryMap.entrySet().iterator();
    private final String parentJarUrlPart;

    /**
     * Captures the parent jar URL that will prefix each {@code singularity:}-protocol URL produced by this iterator.
     *
     * @param parentJarUrlPart external form of the outer jar URL
     */
    public SingularityURLIterator (String parentJarUrlPart) {

      this.parentJarUrlPart = parentJarUrlPart;
    }

    /**
     * Lets this type serve as its own iterator.
     *
     * @return {@code this}
     */
    @Override
    public Iterator<URLEntry> iterator () {

      return this;
    }

    /**
     * Indicates whether another inverse-mapped entry remains to be converted into a {@link URLEntry}.
     *
     * @return {@code true} when at least one unvisited mapping remains
     */
    @Override
    public boolean hasNext () {

      return inverseEntryIter.hasNext();
    }

    /**
     * Produces the next {@link URLEntry} using a {@code singularity:} URL that points through the outer jar to the
     * specific library jar and, from there, to the requested entry.
     *
     * @return a URL entry whose name is the entry path and whose URL targets that entry inside its bundled jar
     * @throws RuntimeException wrapping a {@link URISyntaxException} or {@link MalformedURLException} when the
     *                          composed URL is not well formed
     */
    @Override
    public URLEntry next () {

      try {

        Map.Entry<String, String> inverseEntry = inverseEntryIter.next();

        return new URLEntry(inverseEntry.getKey(), new URI("singularity", parentJarUrlPart + "@/META-INF/singularity/lib/" + inverseEntry.getValue() + "!/" + inverseEntry.getKey(), null).toURL());
      } catch (URISyntaxException | MalformedURLException exception) {
        throw new RuntimeException(exception);
      }
    }

    /**
     * Removal is not meaningful because the backing map is treated as immutable once the index has been written.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }

  /**
   * Immutable pairing of an entry's logical name (the key used by class/resource lookup) with the {@link URL} at
   * which its bytes can be fetched.
   *
   * @param entryName the logical entry name (class path or resource path)
   * @param entryURL  the URL at which the entry's bytes can be read
   */
  public record URLEntry(String entryName, URL entryURL) {

  }
}
