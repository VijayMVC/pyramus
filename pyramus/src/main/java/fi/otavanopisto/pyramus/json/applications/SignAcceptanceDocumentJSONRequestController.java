package fi.otavanopisto.pyramus.json.applications;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import fi.internetix.smvc.controllers.JSONRequestContext;
import fi.otavanopisto.pyramus.dao.DAOFactory;
import fi.otavanopisto.pyramus.dao.application.ApplicationDAO;
import fi.otavanopisto.pyramus.dao.application.ApplicationSignaturesDAO;
import fi.otavanopisto.pyramus.dao.users.StaffMemberDAO;
import fi.otavanopisto.pyramus.domainmodel.application.Application;
import fi.otavanopisto.pyramus.domainmodel.application.ApplicationSignatureState;
import fi.otavanopisto.pyramus.domainmodel.application.ApplicationSignatures;
import fi.otavanopisto.pyramus.domainmodel.application.ApplicationState;
import fi.otavanopisto.pyramus.domainmodel.users.StaffMember;
import fi.otavanopisto.pyramus.framework.JSONRequestController;
import fi.otavanopisto.pyramus.framework.UserRole;

public class SignAcceptanceDocumentJSONRequestController extends JSONRequestController {

  private static final Logger logger = Logger.getLogger(SignAcceptanceDocumentJSONRequestController.class.getName());

  public void process(JSONRequestContext requestContext) {

    // Ensure user has SSN to be able to sign the document

    StaffMemberDAO staffMemberDAO = DAOFactory.getInstance().getStaffMemberDAO();
    StaffMember staffMember = staffMemberDAO.findById(requestContext.getLoggedUserId());
    if (staffMember == null) {
      logger.warning("Current user cannot be resolved");
      fail(requestContext, "Et ole kirjautunut sisään");
      return;
    }
    if (StringUtils.isBlank(staffMember.getPerson().getSocialSecurityNumber())) {
      logger.warning("Current user lacks social security number");
      fail(requestContext, "Allekirjoittamiseen vaadittua henkilötunnusta ei ole asetettu");
      return;
    }

    // Find application and ensure its state

    Long id = requestContext.getLong("id");
    if (id == null) {
      logger.warning("Missing application id");
      fail(requestContext, "Puuttuva hakemustunnus");
      return;
    }
    ApplicationDAO applicationDAO = DAOFactory.getInstance().getApplicationDAO();
    Application application = applicationDAO.findById(id);
    if (application == null) {
      logger.warning(String.format("Application with id %d not found", id));
      fail(requestContext, String.format("Hakemusta tunnuksella %d ei löytynyt", id));
      return;
    }
    if (application.getState() != ApplicationState.WAITING_STAFF_SIGNATURE) {
      logger.warning(String.format("Application with id %d in incorrect state (%s)", id, application.getState()));
      fail(requestContext, "Hakemus ei ole allekirjoitettavassa tilassa");
      return;
    }

    // Signatures tracking

    ApplicationSignaturesDAO applicationSignaturesDAO = DAOFactory.getInstance().getApplicationSignaturesDAO();
    ApplicationSignatures signatures = applicationSignaturesDAO.findByApplication(application);
    if (signatures == null || signatures.getStaffDocumentState() != ApplicationSignatureState.INVITATION_CREATED) {
      fail(requestContext, "Hyväksymisasiakirja ei ole allekirjoitettavassa tilassa");
      return;
    }

    // TODO return URL to mark application signed, authService used, jsonmappings

    OnnistuuClient onnistuuClient = OnnistuuClient.getInstance();
    try {
      String completionUrl = onnistuuClient.getSignatureUrl(
          signatures.getStaffInvitationId(),
          null, // returnUrl
          staffMember.getPerson().getSocialSecurityNumber(),
          null); // authService

      // Respond with URL to complete the signature

      requestContext.addResponseParameter("status", "OK");
      requestContext.addResponseParameter("completionUrl", completionUrl);
    }
    catch (OnnistuuClientException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      fail(requestContext, e.getMessage());
    }
  }

  public UserRole[] getAllowedRoles() {
    return new UserRole[] { UserRole.ADMINISTRATOR, UserRole.MANAGER };
  }

  private void fail(JSONRequestContext requestContext, String reason) {
    requestContext.addResponseParameter("status", "FAIL");
    requestContext.addResponseParameter("reason", reason);
  }

}