package fi.otavanopisto.pyramus.json.matriculation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import fi.internetix.smvc.controllers.JSONRequestContext;
import fi.otavanopisto.pyramus.dao.DAOFactory;
import fi.otavanopisto.pyramus.dao.matriculation.MatriculationExamEnrollmentDAO;
import fi.otavanopisto.pyramus.domainmodel.matriculation.MatriculationExamEnrollment;
import fi.otavanopisto.pyramus.domainmodel.matriculation.MatriculationExamEnrollmentState;
import fi.otavanopisto.pyramus.framework.JSONRequestController;
import fi.otavanopisto.pyramus.framework.UserRole;

public class SearchEnrollmentsJSONRequestController extends JSONRequestController {

  public void process(JSONRequestContext requestContext) {
    Integer resultsPerPage = NumberUtils.createInteger(requestContext.getRequest().getParameter("maxResults"));
    if (resultsPerPage == null) {
      resultsPerPage = 10;
    }

    Integer page = NumberUtils.createInteger(requestContext.getRequest().getParameter("page"));
    if (page == null) {
      page = 0;
    }
    
    String stateStr=
      requestContext.getString("state");
    MatriculationExamEnrollmentState state = null;
    if (stateStr != null) {
      state = MatriculationExamEnrollmentState.valueOf(stateStr);
    }
    
    MatriculationExamEnrollmentDAO dao = DAOFactory.getInstance().getMatriculationExamEnrollmentDAO();
    List<MatriculationExamEnrollment> enrollments;
    if (state == null) {
      enrollments = dao.listAll(page * resultsPerPage, resultsPerPage);
    } else {
      enrollments = dao.listByState(state, page * resultsPerPage, resultsPerPage);
    }
    
    List<Map<String, Object>> results = new ArrayList<>();
    for (MatriculationExamEnrollment enrollment : enrollments) {
      Map<String, Object> result = new HashMap<>();
      
      result.put("id", enrollment.getId());
      result.put("name", enrollment.getName());
      result.put("email", enrollment.getEmail());
      result.put("state", enrollment.getState());
      results.add(result);
    }
    
    long count = dao.count();
    
    requestContext.addResponseParameter("statusMessage", "");
    requestContext.addResponseParameter("results", results);
    requestContext.addResponseParameter("pages", Math.ceil((double)count / resultsPerPage));
    requestContext.addResponseParameter("page", page);
  }

  public UserRole[] getAllowedRoles() {
    return new UserRole[] { UserRole.ADMINISTRATOR, UserRole.MANAGER, UserRole.STUDY_PROGRAMME_LEADER };
  }

}
