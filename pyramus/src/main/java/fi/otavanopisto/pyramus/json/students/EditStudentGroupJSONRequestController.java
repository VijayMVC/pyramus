package fi.otavanopisto.pyramus.json.students;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.StaleObjectStateException;

import fi.internetix.smvc.SmvcRuntimeException;
import fi.internetix.smvc.controllers.JSONRequestContext;
import fi.otavanopisto.pyramus.dao.DAOFactory;
import fi.otavanopisto.pyramus.dao.base.OrganizationDAO;
import fi.otavanopisto.pyramus.dao.base.TagDAO;
import fi.otavanopisto.pyramus.dao.students.StudentDAO;
import fi.otavanopisto.pyramus.dao.students.StudentGroupDAO;
import fi.otavanopisto.pyramus.dao.students.StudentGroupStudentDAO;
import fi.otavanopisto.pyramus.dao.students.StudentGroupUserDAO;
import fi.otavanopisto.pyramus.dao.users.StaffMemberDAO;
import fi.otavanopisto.pyramus.domainmodel.base.Organization;
import fi.otavanopisto.pyramus.domainmodel.base.Tag;
import fi.otavanopisto.pyramus.domainmodel.students.Student;
import fi.otavanopisto.pyramus.domainmodel.students.StudentGroup;
import fi.otavanopisto.pyramus.domainmodel.students.StudentGroupStudent;
import fi.otavanopisto.pyramus.domainmodel.students.StudentGroupUser;
import fi.otavanopisto.pyramus.domainmodel.users.StaffMember;
import fi.otavanopisto.pyramus.domainmodel.users.User;
import fi.otavanopisto.pyramus.framework.JSONRequestController;
import fi.otavanopisto.pyramus.framework.PyramusStatusCode;
import fi.otavanopisto.pyramus.framework.UserRole;
import fi.otavanopisto.pyramus.framework.UserUtils;

/**
 * The controller responsible of modifying an existing student group.
 * 
 * @see fi.otavanopisto.pyramus.views.students.EditStudentGroupViewController
 */
public class EditStudentGroupJSONRequestController extends JSONRequestController {

  /**
   * Processes the request to edit a student group.
   * 
   * @param requestContext
   *          The JSON request context
   */
  public void process(JSONRequestContext requestContext) {
    StaffMemberDAO staffMemberDAO = DAOFactory.getInstance().getStaffMemberDAO();
    StudentDAO studentDAO = DAOFactory.getInstance().getStudentDAO();
    StudentGroupDAO studentGroupDAO = DAOFactory.getInstance().getStudentGroupDAO();
    StudentGroupStudentDAO studentGroupStudentDAO = DAOFactory.getInstance().getStudentGroupStudentDAO();
    StudentGroupUserDAO studentGroupUserDAO = DAOFactory.getInstance().getStudentGroupUserDAO();
    TagDAO tagDAO = DAOFactory.getInstance().getTagDAO();
    OrganizationDAO organizationDAO = DAOFactory.getInstance().getOrganizationDAO();

    // StudentGroup basic information

    String name = requestContext.getString("name");
    String description = requestContext.getString("description");
    Date beginDate = requestContext.getDate("beginDate");
    String tagsText = requestContext.getString("tags");
    Boolean guidanceGroup = requestContext.getBoolean("guidanceGroup");
    
    Set<Tag> tagEntities = new HashSet<>();
    if (!StringUtils.isBlank(tagsText)) {
      List<String> tags = Arrays.asList(tagsText.split("[\\ ,]"));
      for (String tag : tags) {
        if (!StringUtils.isBlank(tag)) {
          Tag tagEntity = tagDAO.findByText(tag.trim());
          if (tagEntity == null)
            tagEntity = tagDAO.create(tag);
          tagEntities.add(tagEntity);
        }
      }
    }

    StudentGroup studentGroup = studentGroupDAO.findById(requestContext.getLong("studentGroupId"));
    User loggedUser = staffMemberDAO.findById(requestContext.getLoggedUserId());

    // Version check
    Long version = requestContext.getLong("version"); 
    if (!studentGroup.getVersion().equals(version))
      throw new StaleObjectStateException(StudentGroup.class.getName(), studentGroup.getId());
    
    Organization organization = organizationDAO.findById(requestContext.getLong("organizationId"));

    if (!UserUtils.canAccessOrganization(loggedUser, organization)) {
      throw new SmvcRuntimeException(PyramusStatusCode.UNAUTHORIZED, "Invalid organization.");
    }
    
    studentGroupDAO.update(studentGroup, organization, name, description, beginDate, loggedUser);

    // Tags

    studentGroupDAO.setStudentGroupTags(studentGroup, tagEntities);
    
    // Guidance group
    
    studentGroupDAO.updateGuidanceGroup(studentGroup, guidanceGroup);

    // Personnel

    StudentGroupUser[] users = studentGroup.getUsers().toArray(new StudentGroupUser[0]);
    StudentGroupStudent[] students = studentGroup.getStudents().toArray(new StudentGroupStudent[0]);

    List<Long> removables = new ArrayList<>();
    Iterator<StudentGroupUser> userIterator = studentGroup.getUsers().iterator();
    while (userIterator.hasNext())
      removables.add(userIterator.next().getId());
    
    int rowCount = requestContext.getInteger("usersTable.rowCount").intValue();
    for (int i = 0; i < rowCount; i++) {
      String colPrefix = "usersTable." + i;
      Long userId = requestContext.getLong(colPrefix + ".userId");
      Long studentGroupUserId = requestContext.getLong(colPrefix + ".studentGroupUserId");
      
      if (studentGroupUserId == null) {
        // New User
        StaffMember staffMember = staffMemberDAO.findById(userId);
        studentGroupUserDAO.create(studentGroup, staffMember, loggedUser);
      } else {
        // Old User, still in list
        removables.remove(studentGroupUserId);
      }
    }

    // Remove the ones that were deleted from group
    for (int i = 0; i < users.length; i++) {
      if (removables.contains(users[i].getId()))
        studentGroupUserDAO.remove(studentGroup, users[i], loggedUser);
    }

    // Students

    removables.clear();
    Iterator<StudentGroupStudent> studentIterator = studentGroup.getStudents().iterator();
    while (studentIterator.hasNext())
      removables.add(studentIterator.next().getId());

    rowCount = requestContext.getInteger("studentsTable.rowCount");
    for (int i = 0; i < rowCount; i++) {
      String colPrefix = "studentsTable." + i;

      Long studentId = requestContext.getLong(colPrefix + ".studentId");
      Long studentGroupStudentId = requestContext.getLong(colPrefix + ".studentGroupStudentId");
      
      if (studentGroupStudentId == null) {
        // New Student
        Student student = studentDAO.findById(studentId);
        studentGroupStudentDAO.create(studentGroup, student, loggedUser);
      } else {
        // Old User, still in list, we'll update if the student has changed student group
        removables.remove(studentGroupStudentId);
        
        StudentGroupStudent sgStudent = studentGroupStudentDAO.findById(studentGroupStudentId);
        if (!sgStudent.getStudent().getId().equals(studentId))
          studentGroupStudentDAO.update(sgStudent, studentDAO.findById(studentId), loggedUser);
      }
    }

    // Remove the ones that were deleted from group
    for (int i = 0; i < students.length; i++) {
      if (removables.contains(students[i].getId()))
        studentGroupStudentDAO.remove(studentGroup, students[i], loggedUser);
    }
    
    requestContext.setRedirectURL(requestContext.getReferer(true));
  }

  public UserRole[] getAllowedRoles() {
    return new UserRole[] { UserRole.MANAGER, UserRole.STUDY_PROGRAMME_LEADER, UserRole.ADMINISTRATOR };
  }

}
