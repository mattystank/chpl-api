package gov.healthit.chpl.questionableActivity;


import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import gov.healthit.chpl.auth.permission.GrantedPermission;
import gov.healthit.chpl.auth.user.JWTAuthenticatedUser;
import gov.healthit.chpl.caching.UnitTestRules;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.dao.QuestionableActivityDAO;
import gov.healthit.chpl.domain.CQMResultDetails;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertificationStatus;
import gov.healthit.chpl.domain.CertificationStatusEvent;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.ListingUpdateRequest;
import gov.healthit.chpl.domain.concept.QuestionableActivityTriggerConcept;
import gov.healthit.chpl.dto.questionableActivity.QuestionableActivityCertificationResultDTO;
import gov.healthit.chpl.dto.questionableActivity.QuestionableActivityListingDTO;
import gov.healthit.chpl.manager.CertifiedProductDetailsManager;
import gov.healthit.chpl.web.controller.CertifiedProductController;
import gov.healthit.chpl.web.controller.InvalidArgumentsException;
import gov.healthit.chpl.web.controller.exception.MissingReasonException;
import gov.healthit.chpl.web.controller.exception.ValidationException;
import junit.framework.TestCase;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { gov.healthit.chpl.CHPLTestConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class })
@DatabaseSetup("classpath:data/testData.xml")
public class CertificationResultTest extends TestCase {
	
	@Autowired private QuestionableActivityDAO qaDao;	
	@Autowired private CertifiedProductController cpController;
	@Autowired private CertifiedProductDetailsManager cpdManager;
	private static JWTAuthenticatedUser adminUser;
	
	@Rule
    @Autowired
    public UnitTestRules cacheInvalidationRule;
	
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		adminUser = new JWTAuthenticatedUser();
		adminUser.setFirstName("Administrator");
		adminUser.setId(-2L);
		adminUser.setLastName("Administrator");
		adminUser.setSubjectName("admin");
		adminUser.getPermissions().add(new GrantedPermission("ROLE_ADMIN"));
	}
	
	@Test
	@Transactional
	@Rollback
	public void testUpdateGap() throws 
	    EntityCreationException, EntityRetrievalException, 
	    ValidationException, InvalidArgumentsException, JsonProcessingException,
	    MissingReasonException, IOException {
	    SecurityContextHolder.getContext().setAuthentication(adminUser);

	    Date beforeActivity = new Date(); 
	    CertifiedProductSearchDetails listing = cpdManager.getCertifiedProductDetails(1L);
	    for(CertificationResult certResult : listing.getCertificationResults()) {
	        if(certResult.getId().longValue() == 1) {
	            certResult.setGap(Boolean.FALSE);
	        }
	    }
	    ListingUpdateRequest updateRequest = new ListingUpdateRequest();
	    updateRequest.setBanDeveloper(false);
	    updateRequest.setListing(listing);
	    cpController.updateCertifiedProduct(updateRequest);
		Date afterActivity = new Date();
		
		List<QuestionableActivityCertificationResultDTO> activities = 
		        qaDao.findCertificationResultActivityBetweenDates(beforeActivity, afterActivity);
		assertNotNull(activities);
		assertEquals(1, activities.size());
		QuestionableActivityCertificationResultDTO activity = activities.get(0);
		assertEquals(1, activity.getCertResultId().longValue());
		assertNotNull(activity.getCertResult());
		assertNotNull(activity.getListing());
		assertEquals(1, activity.getListing().getId().longValue());
		assertEquals(Boolean.TRUE.toString(), activity.getBefore());
		assertEquals(Boolean.FALSE.toString(), activity.getAfter());
		assertEquals(QuestionableActivityTriggerConcept.GAP_EDITED.getName(), activity.getTrigger().getName());
		
		SecurityContextHolder.getContext().setAuthentication(null);
	}
	
	@Test
    @Transactional
    @Rollback
    public void testUpdateGapInsideActivityThreshold_DoesNotRecordActivity() throws
        EntityCreationException, EntityRetrievalException,
        ValidationException, InvalidArgumentsException, JsonProcessingException,
        MissingReasonException, IOException {
        SecurityContextHolder.getContext().setAuthentication(adminUser);

        //make the certification date be now
        //and update the listing so the last updated date minus certification date is
        //within the questionable activity threshold
        Date now = new Date();
        CertifiedProductSearchDetails existingListing = cpdManager.getCertifiedProductDetails(1L);
        existingListing.setCertificationDate(now.getTime());
        existingListing.getCertificationEvents().clear();
        CertificationStatusEvent statusEvent = new CertificationStatusEvent();
        CertificationStatus status = new CertificationStatus();
        status.setId(1L);
        status.setName("Active");
        statusEvent.setStatus(status);
        statusEvent.setEventDate(now.getTime());
        existingListing.getCertificationEvents().add(statusEvent);

        ListingUpdateRequest updateRequest = new ListingUpdateRequest();
        updateRequest.setBanDeveloper(false);
        updateRequest.setListing(existingListing);
        cpController.updateCertifiedProduct(updateRequest);
        
        //confirm the certification date was changed properly
        CertifiedProductSearchDetails updatedListing = cpdManager.getCertifiedProductDetails(1L);
        assertEquals(now.getTime(), updatedListing.getCertificationDate().longValue());
        
        //perform an update that would generate questionable activity outside
        //of the threshold but make sure that no questionable activity was entered.
        Date beforeActivity = new Date();
        CertifiedProductSearchDetails listing = cpdManager.getCertifiedProductDetails(1L);
        for(CertificationResult certResult : listing.getCertificationResults()) {
            if(certResult.getId().longValue() == 1) {
                certResult.setGap(Boolean.FALSE);
            }
        }
        updateRequest = new ListingUpdateRequest();
        updateRequest.setBanDeveloper(false);
        updateRequest.setListing(listing);
        cpController.updateCertifiedProduct(updateRequest);
        Date afterActivity = new Date();

        List<QuestionableActivityCertificationResultDTO> activities =
                qaDao.findCertificationResultActivityBetweenDates(beforeActivity, afterActivity);
        assertTrue(activities == null || activities.size() == 0);
        SecurityContextHolder.getContext().setAuthentication(null);
    }
	
	@Test
    @Transactional
    @Rollback
    public void testUpdateG1Success() throws 
        EntityCreationException, EntityRetrievalException, 
        ValidationException, InvalidArgumentsException, JsonProcessingException,
	    MissingReasonException, IOException {
        SecurityContextHolder.getContext().setAuthentication(adminUser);

        Date beforeActivity = new Date(); 
        CertifiedProductSearchDetails listing = cpdManager.getCertifiedProductDetails(1L);
        for(CertificationResult certResult : listing.getCertificationResults()) {
            if(certResult.getId().longValue() == 1) {
                certResult.setG1Success(Boolean.FALSE);
            }
        }
        ListingUpdateRequest updateRequest = new ListingUpdateRequest();
        updateRequest.setBanDeveloper(false);
        updateRequest.setListing(listing);
        cpController.updateCertifiedProduct(updateRequest);
        Date afterActivity = new Date();
        
        List<QuestionableActivityCertificationResultDTO> activities = 
                qaDao.findCertificationResultActivityBetweenDates(beforeActivity, afterActivity);
        assertNotNull(activities);
        assertEquals(1, activities.size());
        QuestionableActivityCertificationResultDTO activity = activities.get(0);
        assertEquals(1, activity.getCertResultId().longValue());
        assertNotNull(activity.getCertResult());
        assertNotNull(activity.getListing());
        assertEquals(1, activity.getListing().getId().longValue());
        assertEquals(Boolean.TRUE.toString(), activity.getBefore());
        assertEquals(Boolean.FALSE.toString(), activity.getAfter());
        assertEquals(QuestionableActivityTriggerConcept.G1_SUCCESS_EDITED.getName(), activity.getTrigger().getName());
        
        SecurityContextHolder.getContext().setAuthentication(null);
    }
	
	@Test
    @Transactional
    @Rollback
    public void testUpdateG2Success() throws 
        EntityCreationException, EntityRetrievalException, 
        ValidationException, InvalidArgumentsException, JsonProcessingException,
	    MissingReasonException, IOException {
        SecurityContextHolder.getContext().setAuthentication(adminUser);

        Date beforeActivity = new Date(); 
        CertifiedProductSearchDetails listing = cpdManager.getCertifiedProductDetails(1L);
        for(CertificationResult certResult : listing.getCertificationResults()) {
            if(certResult.getId().longValue() == 1) {
                certResult.setG2Success(Boolean.TRUE);
            }
        }
        ListingUpdateRequest updateRequest = new ListingUpdateRequest();
        updateRequest.setBanDeveloper(false);
        updateRequest.setListing(listing);
        cpController.updateCertifiedProduct(updateRequest);
        Date afterActivity = new Date();
        
        List<QuestionableActivityCertificationResultDTO> activities = 
                qaDao.findCertificationResultActivityBetweenDates(beforeActivity, afterActivity);
        assertNotNull(activities);
        assertEquals(1, activities.size());
        QuestionableActivityCertificationResultDTO activity = activities.get(0);
        assertEquals(1, activity.getCertResultId().longValue());
        assertNotNull(activity.getCertResult());
        assertNotNull(activity.getListing());
        assertEquals(1, activity.getListing().getId().longValue());
        assertEquals(Boolean.FALSE.toString(), activity.getBefore());
        assertEquals(Boolean.TRUE.toString(), activity.getAfter());
        assertEquals(QuestionableActivityTriggerConcept.G2_SUCCESS_EDITED.getName(), activity.getTrigger().getName());
        
        SecurityContextHolder.getContext().setAuthentication(null);
    }
	
	//TODO: add test for g1 and g2 macra measures added/removed.
	//Need a 2015 listing that passes validation that also certifies to 
	//a criteria that can have g1 and g2 macra measures
}