package fi.pyramus.dao.students;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;

import fi.pyramus.dao.PyramusEntityDAO;
import fi.pyramus.domainmodel.students.StudentEducationalLevel;

@Stateless
public class StudentEducationalLevelDAO extends PyramusEntityDAO<StudentEducationalLevel> {
 
  public StudentEducationalLevel create(String name) {
    EntityManager entityManager = getEntityManager();
    
    StudentEducationalLevel studentEducationalLevel = new StudentEducationalLevel();
    studentEducationalLevel.setName(name);
    
    entityManager.persist(studentEducationalLevel);
    return studentEducationalLevel;
  }
  
  public StudentEducationalLevel updateName(StudentEducationalLevel studentEducationalLevel, String name) {
    EntityManager entityManager = getEntityManager();
    
    studentEducationalLevel.setName(name);
    
    entityManager.persist(studentEducationalLevel);
    return studentEducationalLevel;
  }
  
}
