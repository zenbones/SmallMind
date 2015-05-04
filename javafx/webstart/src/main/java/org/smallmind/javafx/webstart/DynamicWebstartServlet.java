/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.javafx.webstart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DynamicWebstartServlet extends HttpServlet {

  private static Pattern JNLP_HREF_PATTERN = Pattern.compile("<jnlp .*href\\s*=\\s*\"([^\"]*)\"\\s*>");

  public void init (ServletConfig config)
    throws ServletException {

    super.init(config);
  }

  protected void service (HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {

    InputStream jnlpInputStream;

    if ((jnlpInputStream = getServletContext().getResourceAsStream(req.getRequestURI().substring(req.getContextPath().length()))) == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    else {

      Matcher jnlpHrefMatcher;
      ByteArrayOutputStream jnlpOutputStream;
      String jnlpContents;
      byte[] buffer;
      int bytesRead;

      buffer = new byte[1024];
      jnlpOutputStream = new ByteArrayOutputStream();
      while ((bytesRead = jnlpInputStream.read(buffer)) >= 0) {
        jnlpOutputStream.write(buffer, 0, bytesRead);
      }

      jnlpOutputStream.close();
      jnlpInputStream.close();

      jnlpContents = new String(jnlpOutputStream.toByteArray());
      if ((jnlpHrefMatcher = JNLP_HREF_PATTERN.matcher(jnlpContents)).find()) {
        jnlpContents = jnlpContents.substring(0, jnlpHrefMatcher.start(1)) + req.getRequestURL() + jnlpContents.substring(jnlpHrefMatcher.end(1));
      }

      res.setContentType("application/x-java-jnlp");
      res.getWriter().print(jnlpContents);
      res.flushBuffer();
    }
  }
}
