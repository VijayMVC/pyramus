package fi.pyramus.util.dataimport.scripting.api;

import fi.pyramus.dao.DAOFactory;
import fi.pyramus.domainmodel.clientapplications.ClientApplication;

public class ClientApplicationAPI {
  
  public ClientApplicationAPI(Long loggedUserId) {
    this.loggedUserId = loggedUserId;
  }
  
  public Long create(String clientName, String clientId, String clientSecret)
  {
    ClientApplication app = DAOFactory.getInstance().getClientApplicationDAO().create(clientName, clientId, clientSecret);
    if (app == null) {
      return null;
    } else {
      return app.getId();
    }
  }
  
  @SuppressWarnings("unused")
  private Long loggedUserId;

}
