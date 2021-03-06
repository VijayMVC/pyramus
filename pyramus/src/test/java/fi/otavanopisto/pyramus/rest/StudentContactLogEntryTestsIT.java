package fi.otavanopisto.pyramus.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.restassured.response.Response;

import fi.otavanopisto.pyramus.rest.model.StudentContactLogEntry;
import fi.otavanopisto.pyramus.rest.model.StudentContactLogEntryType;

public class StudentContactLogEntryTestsIT extends AbstractRESTServiceTest {
  
  private final static long TEST_STUDENT_ID = 3l;

  @Test
  public void testCreateStudentContactLogEntry() {
    StudentContactLogEntry studentContactLogEntry = new StudentContactLogEntry(null, "create text", "creator name", getDate(2010, 3, 5), StudentContactLogEntryType.CHATLOG, Boolean.FALSE);
    Response response = given().headers(getAuthHeaders())
      .contentType("application/json")
      .body(studentContactLogEntry)
      .post("/students/students/{ID}/contactLogEntries", TEST_STUDENT_ID);
    
    response.then()
      .statusCode(200)
      .body("id", not(is((Long) null)))
      .body("text", is(studentContactLogEntry.getText()))
      .body("creatorName", is(studentContactLogEntry.getCreatorName()))
      .body("entryDate", is(studentContactLogEntry.getEntryDate().toString()))
      .body("type", is(studentContactLogEntry.getType().toString()))
      .body("archived", is( studentContactLogEntry.getArchived() ));
      
    int id = response.body().jsonPath().getInt("id");
    
    given().headers(getAuthHeaders())
      .delete("/students/students/{STUDENTID}/contactLogEntries/{ID}?permanent=true", TEST_STUDENT_ID, id)
      .then()
      .statusCode(204);
  }

  @Test
  public void testListStudentContactLogEntries() {
    given().headers(getAuthHeaders())
      .get("/students/students/{ID}/contactLogEntries", TEST_STUDENT_ID)
      .then()
      .statusCode(200)
      .body("id.size()", is(2))
      .body("id[0]", is(1) )
      .body("text[0]", is("Test text #1"))
      .body("creatorName[0]", is("Tester #1"))
      .body("entryDate[0]", is(getDate(2010, 1, 1).toString()))
      .body("type[0]", is("LETTER"))
      .body("archived[0]", is( Boolean.FALSE ))
      .body("id[1]", is(2) )
      .body("text[1]", is("Test text #2"))
      .body("creatorName[1]", is("Tester #2"))
      .body("entryDate[1]", is(getDate(2011, 1, 1).toString()))
      .body("type[1]", is("PHONE"))
      .body("archived[1]", is( Boolean.FALSE ));
  }
  
  @Test
  public void testFindStudentContactLogEntry() {
    given().headers(getAuthHeaders())
      .get("/students/students/{STUDENTID}/contactLogEntries/{ID}", TEST_STUDENT_ID, 1l)
      .then()
      .statusCode(200)
      .body("id", is(1) )
      .body("text", is("Test text #1"))
      .body("creatorName", is("Tester #1"))
      .body("entryDate", is(getDate(2010, 1, 1).toString()))
      .body("type", is("LETTER"))
      .body("archived", is( Boolean.FALSE ));
  }
  
  @Test
  public void testUpdateStudentContactLogEntry() {
    StudentContactLogEntry studentContactLogEntry = new StudentContactLogEntry(null, "not updated text", "not updated creater", getDate(2010, 3, 5), StudentContactLogEntryType.CHATLOG, Boolean.FALSE);
    Response response = given().headers(getAuthHeaders())
      .contentType("application/json")
      .body(studentContactLogEntry)
      .post("/students/students/{ID}/contactLogEntries", TEST_STUDENT_ID);
    
    response.then()
      .statusCode(200)
      .body("id", not(is((Long) null)))
      .body("text", is(studentContactLogEntry.getText()))
      .body("creatorName", is(studentContactLogEntry.getCreatorName()))
      .body("entryDate", is(studentContactLogEntry.getEntryDate().toString()))
      .body("type", is(studentContactLogEntry.getType().toString()))
      .body("archived", is( studentContactLogEntry.getArchived() ));
      
    Long id = new Long(response.body().jsonPath().getInt("id"));
    try {
      StudentContactLogEntry updateContactLogEntry = new StudentContactLogEntry(id, "updated text", "updated creater", getDate(2013, 3, 5), StudentContactLogEntryType.FACE2FACE, Boolean.FALSE);
      
      given().headers(getAuthHeaders())
        .contentType("application/json")
        .body(updateContactLogEntry)
        .put("/students/students/{STUDENTID}/contactLogEntries/{ID}", TEST_STUDENT_ID, id)
        .then()
        .statusCode(200)
        .body("id", is(updateContactLogEntry.getId().intValue() ))
        .body("text", is(updateContactLogEntry.getText()))
        .body("creatorName", is(updateContactLogEntry.getCreatorName()))
        .body("entryDate", is(updateContactLogEntry.getEntryDate().toString()))
        .body("type", is(updateContactLogEntry.getType().toString()))
        .body("archived", is( updateContactLogEntry.getArchived() ));

    } finally {
      given().headers(getAuthHeaders())
        .delete("/students/students/{STUDENTID}/contactLogEntries/{ID}?permanent=true", TEST_STUDENT_ID, id)
        .then()
        .statusCode(204);
    }
  }
  
  @Test
  public void testDeleteStudentContactLogEntry() {
    StudentContactLogEntry studentContactLogEntry = new StudentContactLogEntry(null, "create text", "creator name", getDate(2010, 3, 5), StudentContactLogEntryType.CHATLOG, Boolean.FALSE);
    Response response = given().headers(getAuthHeaders())
      .contentType("application/json")
      .body(studentContactLogEntry)
      .post("/students/students/{ID}/contactLogEntries", TEST_STUDENT_ID);
    
    response.then()
      .statusCode(200)
      .body("id", not(is((Long) null)))
      .body("text", is(studentContactLogEntry.getText()))
      .body("creatorName", is(studentContactLogEntry.getCreatorName()))
      .body("entryDate", is(studentContactLogEntry.getEntryDate().toString()))
      .body("type", is(studentContactLogEntry.getType().toString()))
      .body("archived", is( studentContactLogEntry.getArchived() ));
      
    Long id = new Long(response.body().jsonPath().getInt("id"));
    assertNotNull(id);
    
    given().headers(getAuthHeaders()).get("/students/students/{STUDENTID}/contactLogEntries/{ID}", TEST_STUDENT_ID, id)
      .then()
      .statusCode(200);
    
    given().headers(getAuthHeaders())
      .delete("/students/students/{STUDENTID}/contactLogEntries/{ID}", TEST_STUDENT_ID, id)
      .then()
      .statusCode(204);
    
    given().headers(getAuthHeaders()).get("/students/students/{STUDENTID}/contactLogEntries/{ID}", TEST_STUDENT_ID, id)
      .then()
      .statusCode(404);
    
    given().headers(getAuthHeaders())
      .delete("/students/students/{STUDENTID}/contactLogEntries/{ID}?permanent=true", TEST_STUDENT_ID, id)
      .then()
      .statusCode(204);
    
    given().headers(getAuthHeaders()).get("/students/students/{STUDENTID}/contactLogEntries/{ID}", TEST_STUDENT_ID, id)
      .then()
      .statusCode(404);
  }
}
