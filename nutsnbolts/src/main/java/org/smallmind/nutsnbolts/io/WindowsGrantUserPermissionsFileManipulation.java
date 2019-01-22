/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.List;

public class WindowsGrantUserPermissionsFileManipulation implements FileManipulation {

  @Override
  public void manipulateFile (Path path)
    throws IOException {

    AclFileAttributeView aclAttrView = Files.getFileAttributeView(path, AclFileAttributeView.class);
    UserPrincipalLookupService upls = path.getFileSystem().getUserPrincipalLookupService();
    UserPrincipal user = upls.lookupPrincipalByName(System.getProperty("user.name"));
    AclEntry aclEntry = AclEntry.newBuilder()
                          .setPermissions(AclEntryPermission.READ_DATA, AclEntryPermission.EXECUTE, AclEntryPermission.READ_ACL, AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.READ_NAMED_ATTRS, AclEntryPermission.WRITE_ACL, AclEntryPermission.DELETE)
                          .setPrincipal(user)
                          .setType(AclEntryType.ALLOW)
                          .build();
    List<AclEntry> acl = aclAttrView.getAcl();

    Files.setOwner(path, user);
    acl.add(0, aclEntry);
    aclAttrView.setAcl(acl);
  }

  @Override
  public void manipulateDirectory (Path path) {

  }
}
