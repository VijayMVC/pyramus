package fi.otavanopisto.pyramus.rest;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import fi.otavanopisto.pyramus.dao.DAOFactory;
import fi.otavanopisto.pyramus.dao.application.ApplicationDAO;
import fi.otavanopisto.pyramus.dao.base.LanguageDAO;
import fi.otavanopisto.pyramus.dao.base.MunicipalityDAO;
import fi.otavanopisto.pyramus.dao.base.NationalityDAO;
import fi.otavanopisto.pyramus.dao.base.StudyProgrammeDAO;
import fi.otavanopisto.pyramus.dao.system.SettingDAO;
import fi.otavanopisto.pyramus.dao.system.SettingKeyDAO;
import fi.otavanopisto.pyramus.domainmodel.application.Application;
import fi.otavanopisto.pyramus.domainmodel.application.ApplicationState;
import fi.otavanopisto.pyramus.domainmodel.base.Language;
import fi.otavanopisto.pyramus.domainmodel.base.Municipality;
import fi.otavanopisto.pyramus.domainmodel.base.Nationality;
import fi.otavanopisto.pyramus.domainmodel.base.StudyProgramme;
import fi.otavanopisto.pyramus.domainmodel.system.Setting;
import fi.otavanopisto.pyramus.domainmodel.system.SettingKey;
import fi.otavanopisto.pyramus.rest.annotation.Unsecure;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

@Path("/application")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateful
@RequestScoped
public class ApplicationRESTService extends AbstractRESTService {

  private static final Logger logger = Logger.getLogger(ApplicationRESTService.class.getName());

  @Context
  private UriInfo uri;

  @Inject
  private MunicipalityDAO municipalityDAO;

  @Inject
  private NationalityDAO nationalityDAO;

  @Inject
  private LanguageDAO languageDAO;

  @Path("/createattachment")
  @POST
  @Unsecure
  @Consumes(MediaType.MULTIPART_FORM_DATA + ";charset=UTF-8")
  public Response createAttachment(MultipartFormDataInput multipart, @HeaderParam("Referer") String referer) {
    if (!isApplicationCall(referer, "application/create.page")) {
      return Response.status(Status.FORBIDDEN).build();
    }
    String attachmentsFolder = getSettingValue("application.storagePath");
    try {
      byte[] fileData = getFile(multipart, "file");
      String name = getString(multipart, "name");
      String attachmentFolder = getString(multipart, "applicationId");
      if (fileData == null || fileData.length == 0 || name == null || attachmentFolder == null) {
        logger.log(Level.SEVERE, "Application attachment preconditions not met");
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
      }
      File folder = Paths.get(attachmentsFolder, attachmentFolder).toFile();
      if (!folder.exists()) {
        folder.mkdir();
      }
      File file = Paths.get(attachmentsFolder, attachmentFolder, name).toFile();
      if (file.exists()) {
        return Response.status(Status.CONFLICT).build();
      }
      FileUtils.writeByteArrayToFile(file, fileData);
      logger.log(Level.INFO, String.format("Stored application attachment %s", file.getAbsolutePath()));
    }
    catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to store application attachment", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.noContent().build();
  }

  @Path("/getattachment/{ID}")
  @GET
  @Unsecure
  @Produces("*/*")
  public Response getAttachment(@PathParam("ID") String applicationId, @QueryParam("attachment") String attachment, @HeaderParam("Referer") String referer) {
    try {
      if (!isApplicationCall(referer, "application/create.page")) {
        return Response.status(Status.FORBIDDEN).build();
      }
      java.nio.file.Path path = Paths.get(getSettingValue("application.storagePath"), applicationId, attachment);
      File file = path.toFile();
      if (file.exists()) {
        String contentType = Files.probeContentType(path);
        byte[] data = FileUtils.readFileToByteArray(file);
        return Response.ok(data)
            .type(contentType)
            .header("Content-Length", data.length)
            .header("Content-Disposition", String.format("inline; filename=\"%s\"", StringUtils.replace(attachment, "\"", "\\\"")))
            .build();
      }
    }
    catch (Exception e) {
      logger.log(Level.SEVERE, String.format("Exception serving application attachment %s", attachment), e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  @Path("/createapplication")
  @POST
  @Unsecure
  public Response createApplication(Object object, @HeaderParam("Referer") String referer) {
    if (!isApplicationCall(referer, "application/create.page")) {
      return Response.status(Status.FORBIDDEN).build();
    }

    JSONObject formData = JSONObject.fromObject(object);
    System.out.println("-----------> " + formData.toString());
    
    // Validate key parts of form data
    
    String referenceCode = "RANDOM"; // TODO Generate, report back to caller
    try {
      String applicationId = formData.getString("field-application-id");
      if (applicationId == null) {
        logger.log(Level.WARNING, "Refusing application creation due to missing applicationId");
        return Response.status(Status.BAD_REQUEST).build();
      }
      String firstName = formData.getString("field-first-names");
      if (firstName == null) {
        logger.log(Level.WARNING, "Refusing application creation due to missing first name");
        return Response.status(Status.BAD_REQUEST).build();
      }
      String lastName = formData.getString("field-last-name");
      if (lastName == null) {
        logger.log(Level.WARNING, "Refusing application creation due to missing last name");
        return Response.status(Status.BAD_REQUEST).build();
      }
      String email = formData.getString("field-email");
      if (email == null) {
        logger.log(Level.WARNING, "Refusing application creation due to missing email");
        return Response.status(Status.BAD_REQUEST).build();
      }
      Long studyProgrammeId = formData.getLong("field-studyprogramme-id");
      StudyProgrammeDAO studyProgrammeDAO = DAOFactory.getInstance().getStudyProgrammeDAO();
      StudyProgramme studyProgramme = studyProgrammeDAO.findById(studyProgrammeId);
      if (studyProgramme == null) {
        logger.log(Level.WARNING, String.format("Refusing application creation due to study programme with id %d not found", studyProgrammeId));
      }
      
      // Store application
      
      ApplicationDAO applicationDAO = DAOFactory.getInstance().getApplicationDAO();
      Application application = applicationDAO.createApplication(
          applicationId,
          studyProgramme,
          firstName,
          lastName,
          email,
          referenceCode,
          formData.toString(),
          ApplicationState.PENDING);

      logger.log(Level.INFO, String.format("Created new %s application with id %s", studyProgramme.getName(), application.getApplicationId()));
      
      // TODO Send confirmation mail
      
      Map<String, String> response = new HashMap<String, String>();
      response.put("referenceCode", referenceCode);

      return Response.ok(response).build();
    }
    catch (JSONException e) {
      logger.log(Level.SEVERE, String.format("Refusing application creation due to malformatted json: %s", e.toString()));
      return Response.status(Status.BAD_REQUEST).build();
      
    }
  }

  @Path("/municipalities")
  @GET
  @Unsecure
  public Response listMunicipalities() {
    List<Municipality> municipalities = municipalityDAO.listAll();
    municipalities.sort(new Comparator<Municipality>() {
      public int compare(Municipality o1, Municipality o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    List<HashMap<String, String>> municipalityList = new ArrayList<HashMap<String, String>>();

    HashMap<String, String> municipalityData = new HashMap<String, String>();
    municipalityData.put("text", "Ei kotikuntaa Suomessa");
    municipalityData.put("value", "none");
    municipalityList.add(municipalityData);

    for (Municipality municipality : municipalities) {
      municipalityData = new HashMap<String, String>();
      municipalityData.put("text", municipality.getName());
      municipalityData.put("value", municipality.getId().toString());
      municipalityList.add(municipalityData);
    }

    return Response.ok(municipalityList).build();
  }

  @Path("/nationalities")
  @GET
  @Unsecure
  public Response listNationalities() {
    List<Nationality> nationalities = nationalityDAO.listAll();
    nationalities.sort(new Comparator<Nationality>() {
      public int compare(Nationality o1, Nationality o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    List<HashMap<String, String>> nationalityList = new ArrayList<HashMap<String, String>>();

    for (Nationality nationality : nationalities) {
      HashMap<String, String> nationalityData = new HashMap<String, String>();
      nationalityData.put("text", nationality.getName());
      nationalityData.put("value", nationality.getId().toString());
      nationalityList.add(nationalityData);
    }

    return Response.ok(nationalityList).build();
  }

  @Path("/languages")
  @GET
  @Unsecure
  public Response listLanguages() {
    List<Language> languages = languageDAO.listAll();
    languages.sort(new Comparator<Language>() {
      public int compare(Language o1, Language o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    List<HashMap<String, String>> languageList = new ArrayList<HashMap<String, String>>();

    for (Language language : languages) {
      HashMap<String, String> languageData = new HashMap<String, String>();
      languageData.put("text", language.getName());
      languageData.put("value", language.getId().toString());
      languageList.add(languageData);
    }

    return Response.ok(languageList).build();
  }

  private boolean isApplicationCall(String referer, String expectedPath) {
    String s = uri.getBaseUri().toString();
    s = s.substring(0, s.length() - 2); // JaxRsActivator path
    s += expectedPath;
    return StringUtils.equals(referer, s);
  }

  private byte[] getFile(MultipartFormDataInput multipart, String field) {
    try {
      Map<String, List<InputPart>> form = multipart.getFormDataMap();
      List<InputPart> inputParts = form.get(field);
      if (inputParts.size() == 0) {
        return null;
      }
      else {
        return IOUtils.toByteArray(inputParts.get(0).getBody(InputStream.class, null));
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private String getString(MultipartFormDataInput multipart, String field) {
    try {
      Map<String, List<InputPart>> form = multipart.getFormDataMap();
      List<InputPart> inputParts = form.get(field);
      return inputParts.size() == 0 ? null : inputParts.get(0).getBodyAsString();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private String getSettingValue(String key) {
    SettingKeyDAO settingKeyDAO = DAOFactory.getInstance().getSettingKeyDAO();
    SettingKey settingKey = settingKeyDAO.findByName(key);
    if (settingKey != null) {
      SettingDAO settingDAO = DAOFactory.getInstance().getSettingDAO();
      Setting setting = settingDAO.findByKey(settingKey);
      if (setting != null && setting.getValue() != null) {
        return setting.getValue();
      }
    }
    return null;
  }

}