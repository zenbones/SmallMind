package org.smallmind.file.ephemeral;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;

public class EphemeralUserPrincipalLookupService extends UserPrincipalLookupService {

  @Override
  public UserPrincipal lookupPrincipalByName (String name) {

    return new EphemeralUserPrincipal(name);
  }

  @Override
  public GroupPrincipal lookupPrincipalByGroupName (String group) {

    return new EphemeralGroupPrincipal(group);
  }
}
