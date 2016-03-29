/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.web.reverse;

import java.net.Socket;

public class Wombat2 {

  public static void main (String... args)
    throws Exception {

    String s = "GET / HTTP/1.1\r\n" +
                 "Host: www.google.com:80\r\n" +
                 "Connection: keep-alive\r\n" +
                 "User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36\r\n" +
                 "Cache-Control: no-cache\r\n" +
                 "Content-Type: application/json\r\n" +
                 "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiIwMmE5OWRlMy0xZGQ2LTRjOWMtOThlNi1jOWZiNGM0ZDNhNGQiLCJzdWIiOiIyMzNlZjlhOC1hZjhlLTQxYWQtODRiYS05YTIxMDk3ZjZmNTUiLCJzY29wZSI6WyJvYXV0aC5hcHByb3ZhbHMiLCJvcGVuaWQiXSwiY2xpZW50X2lkIjoibG9naW4iLCJjaWQiOiJsb2dpbiIsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiIyMzNlZjlhOC1hZjhlLTQxYWQtODRiYS05YTIxMDk3ZjZmNTUiLCJ1c2VyX25hbWUiOiJtam9uZXNTdGFnZUBmb3Jpby5jb20iLCJlbWFpbCI6Im1qb25lc1N0YWdlQGZvcmlvLmNvbSIsInBhcmVudF9hY2NvdW50X2lkIjpudWxsLCJpYXQiOjE0NTg5NDQxMjMsImV4cCI6MTQ1ODk4NzMyMywiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo5NzYzL3VhYS9vYXV0aC90b2tlbiIsImF1ZCI6WyJvYXV0aCIsIm9wZW5pZCJdfQ.d19sClfRtl7LXl00cVqf7DZZhrblzCs_20qS83cMlkaDgFHang5VnUJO-8J7AA8FDWxAUX1PiFAT8CKZmQJxnRLtsgXhxCHyFEHrildv7l08LeG6nLCoC56-hnIya3A_hfDwBjBnWA1fc3lANskd9GzfE439UB3TqMC0R6_iJB9lesppdgkiZ68wikh1kyb4NDYPCnsTTCv5CIq9AVP_aKRHYejJ6oFYDZbl2E_I_pQFGpShjpLBYjwHilPtWdrBdDmP-b1D-HvzRnVD7CTfFfnlSzETJuB_gQICtn9f6WR2Gf3k7jHAwhTJBHG0voPtc1a9gdv-42oAqYHj3jD0Ig\r\n" +
                 "Postman-Token: aef67747-fca5-0952-bb21-0334a8eb883d\r\n" +
                 "Accept: */*\r\n" +
                 "Accept-Encoding: gzip, deflate, sdch\r\n" +
                 "Accept-Language: en-US,en;q=0.8\r\n\r\n";

    Socket socket = new Socket("google.com", 80);
    socket.getOutputStream().write(s.getBytes());

    byte[] buffer = new byte[8196];
    int bytesRead;
    bytesRead = socket.getInputStream().read(buffer);
    System.out.println(new String(buffer, 0, bytesRead));
  }
}
