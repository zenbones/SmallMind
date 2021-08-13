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
package org.smallmind.file;

import java.nio.file.Path;
import org.smallmind.file.jailed.JailedPath;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PathTest {

  @Test
  public void test () {

    Path p1 = new JailedPath(null, "/alpha/beta/gamma");

    System.out.println(p1);
    System.out.println(p1.isAbsolute());
    System.out.println(p1.getRoot());
    System.out.println(p1.getFileName());
    System.out.println(p1.getNameCount());

    System.out.println(p1.startsWith(new JailedPath(null, "/alpha")));
    System.out.println(p1.startsWith(new JailedPath(null, "alpha")));
    System.out.println(p1.endsWith(new JailedPath(null, "/gamma")));
    System.out.println(p1.endsWith(new JailedPath(null, "gamma")));
    System.out.println(p1.endsWith(new JailedPath(null, "zebra")));

    System.out.println(p1.getParent());
    System.out.println(p1.getParent().getParent());
    System.out.println(p1.getParent().getParent().getParent());
    System.out.println(p1.getParent().getParent().getParent().getParent());

    System.out.println(p1.resolve(new JailedPath(null, "delta/epsilon")));
    System.out.println(p1.resolve(new JailedPath(null, "/delta/epsilon")));

    Path p2 = new JailedPath(null, "alpha/beta/gamma");

    System.out.println(p2);
    System.out.println(p2.isAbsolute());
    System.out.println(p2.getRoot());
    System.out.println(p2.getFileName());
    System.out.println(p2.getNameCount());

    System.out.println(p2.startsWith(new JailedPath(null, "/alpha")));
    System.out.println(p2.startsWith(new JailedPath(null, "alpha")));
    System.out.println(p2.endsWith(new JailedPath(null, "/gamma")));
    System.out.println(p2.endsWith(new JailedPath(null, "gamma")));
    System.out.println(p2.endsWith(new JailedPath(null, "zebra")));

    System.out.println(p2.resolve(new JailedPath(null, "delta/epsilon")));
    System.out.println(p2.resolve(new JailedPath(null, "/delta/epsilon")));

    Path p3 = new JailedPath(null, "/alpha/beta/gamma/delta/epsilon/zeta");

    System.out.println(p3.relativize(new JailedPath(null, "/delta/epsilon")));
    System.out.println(p3.relativize(new JailedPath(null, "delta/epsilon")));
    System.out.println(p3.relativize(new JailedPath(null, "/abc/def")));
    System.out.println(p3.relativize(new JailedPath(null, "abc/def")));

    Path p4 = new JailedPath(null, "alpha/beta/gamma/delta/epsilon/zeta");
  }
}
