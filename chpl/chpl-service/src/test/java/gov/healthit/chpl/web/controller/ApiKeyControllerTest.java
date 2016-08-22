package gov.healthit.chpl.web.controller;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import gov.healthit.chpl.auth.permission.GrantedPermission;
import gov.healthit.chpl.auth.user.JWTAuthenticatedUser;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.dao.impl.ApiKeyActivityDAOImpl;
import gov.healthit.chpl.dao.impl.ApiKeyDAOImpl;
import gov.healthit.chpl.domain.ApiKeyActivity;
import gov.healthit.chpl.entity.ApiKeyActivityEntity;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { gov.healthit.chpl.CHPLTestConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class })
@DatabaseSetup("classpath:data/testData.xml") 
public class ApiKeyControllerTest {
@Autowired
ApiKeyController apiKeyController;

private static JWTAuthenticatedUser adminUser;

@Autowired
private ApiKeyActivityDAOImpl apiKeyActivityDAOImpl;

@BeforeClass
public static void setUpClass() throws Exception {
	adminUser = new JWTAuthenticatedUser();
	adminUser.setFirstName("Administrator");
	adminUser.setId(-2L);
	adminUser.setLastName("Administrator");
	adminUser.setSubjectName("admin");
	adminUser.getPermissions().add(new GrantedPermission("ROLE_ADMIN"));
}

/** Description: Tests the startDate parameter in the 
 * listActivity(Integer pageNumber, Integer pageSize, String apiKeyFilter, boolean dateAscending, Long startDateMilli, Long endDateMilli) 
 * method when passing in null/undefined for &startDate
 * Expected Result: One API key activity is returned that has a api key activity id == first api key activity id in the openchpl_test DB
 * Assumptions:
 * Pre-existing data in openchpl_test DB is there per the \CHPL\chpl-api\chpl\chpl-service\src\test\resources\data\testData.xml
 */
@Test
public void test_listActivity_startDate_UndefinedReturnsOldestApiKeyActivity() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
	SecurityContextHolder.getContext().setAuthentication(adminUser);
	Long oldestApiKeyId = null; 
	List<ApiKeyActivityEntity> allActivitiesList = apiKeyActivityDAOImpl.getAllEntities();
	
	// Get the oldest API key id
	for(ApiKeyActivityEntity activity : allActivitiesList){
		Long activityId = activity.getId();
		if(oldestApiKeyId == null || oldestApiKeyId > activityId){
			oldestApiKeyId = activityId;
		}
	}
	
	// Simulate API inputs
	String apiKeyFilter = null; // Valid API Key in openchpl_test DB
	Integer pageNumber = 0;
	Integer pageSize = 1;
	boolean dateAscending = true;
	
	List<ApiKeyActivity> activitiesList = apiKeyController.listActivity
			(pageNumber, pageSize, apiKeyFilter, dateAscending, null, null);
	assertTrue("Activities list should contain some activities", activitiesList.size() > 0);
	
	Long firstActivityApiKeyId = activitiesList.get(0).getId();
	assertTrue("The api key id returned is not equivalent to the oldest recorded", oldestApiKeyId == firstActivityApiKeyId);
}

/** Description: Tests the endDate parameter in the 
 * listActivity(Integer pageNumber, Integer pageSize, String apiKeyFilter, boolean dateAscending, Long startDateMilli, Long endDateMilli) 
 * method when passing in null/undefined for &endDate
 * Expected Result: The most recent API key activity id is returned
 * Assumptions:
 * Pre-existing data in openchpl_test DB is there per the \CHPL\chpl-api\chpl\chpl-service\src\test\resources\data\testData.xml
 */
@Test
public void test_listActivity_endDate_UndefinedReturnsNewestApiKeyActivity() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
	SecurityContextHolder.getContext().setAuthentication(adminUser);
	Long newestApiKeyId = null;
	List<ApiKeyActivityEntity> allActivitiesList = apiKeyActivityDAOImpl.getAllEntities();
	
	// Get the newest API key id
	for(ApiKeyActivityEntity activity : allActivitiesList){
		Long activityId = activity.getId();
		if(newestApiKeyId == null || newestApiKeyId < activityId){
			newestApiKeyId = activityId;
		}
	}
	
	// Simulate API inputs
	String apiKeyFilter = null; // Valid API Key in openchpl_test DB
	Integer pageNumber = 0;
	Integer pageSize = 1;
	boolean dateAscending = false;
	
	List<ApiKeyActivity> activitiesList = apiKeyController.listActivity
			(pageNumber, pageSize, apiKeyFilter, dateAscending, null, null);
	assertTrue("Activities list should contain some activities", activitiesList.size() > 0);
	
	Long lastActivityApiKeyId = activitiesList.get(activitiesList.size() - 1).getId();
	assertTrue("The api key id returned is not equivalent to the newest recorded", newestApiKeyId.compareTo(lastActivityApiKeyId) == 0);
}

/** Description: Tests the startDate parameter in the 
 * listActivity(Integer pageNumber, Integer pageSize, String apiKeyFilter, boolean dateAscending, Long startDateMilli, Long endDateMilli) 
 * method when passing in a string (instead of expected long datatype) for &endDate
 * Expected Result: Returns handled error message with 400 status code
 * Assumptions:
 * Pre-existing data in openchpl_test DB is there per the \CHPL\chpl-api\chpl\chpl-service\src\test\resources\data\testData.xml
 */
@Test
public void test_listActivity_startDate_ImproperInputReturns400StatusCode() throws EntityRetrievalException, JsonProcessingException, EntityCreationException{
	SecurityContextHolder.getContext().setAuthentication(adminUser);
	
	// Simulate API inputs
	String apiKeyFilter = null; // Valid API Key in openchpl_test DB
	Integer pageNumber = 0;
	Integer pageSize = 1;
	boolean dateAscending = false;
	
	List<ApiKeyActivity> activitiesList = apiKeyController.listActivity
			(pageNumber, pageSize, apiKeyFilter, dateAscending, null, null);
}
	
}