package gov.healthit.chpl.manager.impl;

import org.junit.BeforeClass;
import org.junit.Ignore;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import gov.healthit.chpl.auth.permission.GrantedPermission;
import gov.healthit.chpl.auth.user.JWTAuthenticatedUser;
import gov.healthit.chpl.dao.DeveloperStatusDAO;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.dto.DeveloperDTO;
import gov.healthit.chpl.dto.DeveloperStatusDTO;
import gov.healthit.chpl.dto.ProductDTO;
import gov.healthit.chpl.entity.DeveloperStatusType;
import gov.healthit.chpl.manager.DeveloperManager;
import gov.healthit.chpl.manager.ProductManager;
import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { gov.healthit.chpl.CHPLTestConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class })
@DatabaseSetup("classpath:data/testData.xml")
public class ProductManagerTest extends TestCase {
	
	@Autowired private ProductManager productManager;
	@Autowired private DeveloperManager developerManager;
	@Autowired private DeveloperStatusDAO devStatusDao;
	
	private static JWTAuthenticatedUser adminUser;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		adminUser = new JWTAuthenticatedUser();
		adminUser.setFirstName("Administrator");
		adminUser.setId(-2L);
		adminUser.setLastName("Administrator");
		adminUser.setSubjectName("admin");
		adminUser.getPermissions().add(new GrantedPermission("ROLE_ADMIN"));
	}
	
	//i had this method in here to test for updates being allowed
	//when then developer is Active, but it fails because it triggers
	//a suspicious activity event and tries to send email. 
	//we're missing the email properties but i don't think we want to 
	//have one sent anyway.. so excluding that test.
	@Test
	@Rollback
	@Ignore
	public void testAllowedToUpdateProductWithActiveDeveloper() 
			throws EntityRetrievalException, JsonProcessingException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		ProductDTO product = productManager.getById(-1L);
		assertNotNull(product);
		product.setName("new product name");
		boolean failed = false;
		try {
			product = productManager.update(product);
		} catch(EntityCreationException ex) {
			System.out.println(ex.getMessage());
			failed = true;
		}
		assertFalse(failed);
		assertEquals("new product name", product.getName());
		SecurityContextHolder.getContext().setAuthentication(null);
	}
	
	@Test
	@Rollback
	public void testNotAllowedToUpdateProductWithInactiveDeveloper() 
			throws EntityRetrievalException, JsonProcessingException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		
		//change dev to suspended
		DeveloperDTO developer = developerManager.getById(-1L);
		assertNotNull(developer);
		DeveloperStatusDTO newStatus = devStatusDao.getById(2L);
		developer.setStatus(newStatus);
		
		boolean failed = false;
		try {
			developer = developerManager.update(developer);
		} catch(EntityCreationException ex) {
			System.out.println(ex.getMessage());
			failed = true;
		}
		assertFalse(failed);
		assertNotNull(developer.getStatus());
		assertEquals(DeveloperStatusType.SuspendedByOnc.toString(), developer.getStatus().getStatusName());
		
		//try to update product
		ProductDTO product = productManager.getById(-1L);
		assertNotNull(product);
		product.setName("new product name");
		failed = false;
		try {
			product = productManager.update(product);
		} catch(EntityCreationException ex) {
			System.out.println(ex.getMessage());
			failed = true;
		}
		assertTrue(failed);
		SecurityContextHolder.getContext().setAuthentication(null);
	}
}