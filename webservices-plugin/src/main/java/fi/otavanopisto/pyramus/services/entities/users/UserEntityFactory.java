package fi.otavanopisto.pyramus.services.entities.users;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import fi.otavanopisto.pyramus.domainmodel.base.Email;
import fi.otavanopisto.pyramus.domainmodel.base.Tag;
import fi.otavanopisto.pyramus.domainmodel.users.StaffMember;
import fi.otavanopisto.pyramus.services.entities.EntityFactory;

public class UserEntityFactory implements EntityFactory<UserEntity> {
  
  public UserEntity buildFromDomainObject(Object domainObject) {
    StaffMember user = (StaffMember) domainObject;
    
    Set<String> emails = new HashSet<>();
    for (Email email : user.getContactInfo().getEmails())
      emails.add(email.getAddress());
    
    int i = 0;
    String[] tags = new String[user.getTags().size()];
    for (Tag tag : user.getTags()) {
      tags[i++] = tag.getText();
    }
    
    return new UserEntity(user.getId(), emails.toArray(new String[0]), user.getFirstName(), user.getLastName(), tags); //, user.getExternalId(), user.getAuthProvider(), user.getRole().name());
  }
  
  public UserEntity[] buildFromDomainObjects(Collection<?> domainObjects) {
    UserEntity[] entities = new UserEntity[domainObjects.size()];
    int i = 0;
    for (Object domainObject : domainObjects) {
      entities[i++] = buildFromDomainObject(domainObject);
    }
    
    return entities;
  }
}