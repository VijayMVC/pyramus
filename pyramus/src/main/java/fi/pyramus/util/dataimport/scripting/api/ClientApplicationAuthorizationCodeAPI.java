package fi.pyramus.util.dataimport.scripting.api;

import fi.pyramus.dao.DAOFactory;
import fi.pyramus.domainmodel.clientapplications.ClientApplication;
import fi.pyramus.domainmodel.clientapplications.ClientApplicationAuthorizationCode;
import fi.pyramus.domainmodel.users.User;
import fi.pyramus.util.dataimport.scripting.InvalidScriptException;

public class ClientApplicationAuthorizationCodeAPI {
  
  public ClientApplicationAuthorizationCodeAPI(Long loggedUserId) {
    this.loggedUserId = loggedUserId;
  }
  

  public Long create(String authorizationCode, String redirectUrl, Long userId, Long clientApplicationId) throws InvalidScriptException {
    DAOFactory daoFactory = DAOFactory.getInstance();
    
    User user = daoFactory.getStaffMemberDAO().findById(userId);
    if (user == null) {
      user = daoFactory.getStudentDAO().findById(userId);
    }
    if (user == null) {
      throw new InvalidScriptException("User not found");
    }
    
    ClientApplication clientApplication = daoFactory.getClientApplicationDAO().findById(clientApplicationId);
    if (clientApplication == null) {
      throw new InvalidScriptException("Client application not found");
    }
    
    ClientApplicationAuthorizationCode code =
        DAOFactory.getInstance().getClientApplicationAuthorizationCodeDAO().create(user, clientApplication, authorizationCode, redirectUrl);
    return code.getId();
  }
  
  @SuppressWarnings("unused")
  private Long loggedUserId;

}
