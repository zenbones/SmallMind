/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.nutsnbolts.freemarker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import freemarker.cache.TemplateLoader;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.nutsnbolts.resource.ResourceParser;
import org.smallmind.nutsnbolts.resource.ResourceTypeResourceGenerator;

public class ResourceTemplateLoader implements TemplateLoader {

  private static final ResourceParser RESOURCE_PARSER = new ResourceParser(new ResourceTypeResourceGenerator());

  @Override
  public Object findTemplateSource (String name)
    throws IOException {

    try {
      return new ResourceTemplateSource(RESOURCE_PARSER.parseResource(name));
    } catch (ResourceException resourceException) {
      throw new IOException(resourceException);
    }
  }

  @Override
  public long getLastModified (Object templateSource) {

    return -1;
  }

  @Override
  public Reader getReader (Object templateSource, String encoding)
    throws IOException {

    try {
      return new InputStreamReader(((ResourceTemplateSource)templateSource).getInputStream(), encoding);
    } catch (ResourceException resourceException) {
      throw new IOException(resourceException);
    }
  }

  @Override
  public void closeTemplateSource (Object templateSource)
    throws IOException {

    ((ResourceTemplateSource)templateSource).close();
  }
}
