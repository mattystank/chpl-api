package gov.healthit.chpl.auth.controller;


import gov.healthit.chpl.auth.user.User;
import gov.healthit.chpl.auth.user.UserImpl;
import gov.healthit.chpl.auth.user.UserManager;
import gov.healthit.chpl.auth.user.UserRetrievalException;
import gov.healthit.chpl.auth.user.registration.UserCreationException;
import gov.healthit.chpl.auth.user.registration.UserDTO;
import gov.healthit.chpl.auth.user.registration.UserRegistrar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class UserManagementController {
	
	@Autowired
	UserRegistrar registrar;
	
	@Autowired
	UserManager userManager;
	
	@RequestMapping(value="/create_user", method= RequestMethod.POST, 
			consumes= MediaType.APPLICATION_JSON_VALUE,
			produces="application/json; charset=utf-8")
	public String createUser(@RequestBody UserDTO userInfo) throws UserCreationException {
		
		registrar.createUser(userInfo);
		String isSuccess = String.valueOf(true);
		return "{\"userCreated\" : "+isSuccess+" }";
		
	}
	
	@RequestMapping(value="/reset_password", method= RequestMethod.POST, 
			consumes= MediaType.APPLICATION_FORM_URLENCODED_VALUE,
			produces="application/json; charset=utf-8")
	public String resetPassword(@RequestParam("userName") String userName, 
			@RequestParam("password") String password) throws UserRetrievalException {
		
		boolean passwordUpdated = registrar.updateUserPassword(userName, password);
		String isSuccess = String.valueOf(passwordUpdated);
		return "{\"passwordUpdated\" : "+isSuccess+" }";
		
	}
	
	
	@RequestMapping(value="/update_user", method= RequestMethod.POST, 
			consumes= MediaType.APPLICATION_JSON_VALUE,
			produces="application/json; charset=utf-8")
	public String updateUserDetails(@RequestBody UserDTO userInfo) throws UserRetrievalException {
		return "";
	}
	
	@RequestMapping(value="/add_user_role", method= RequestMethod.POST, 
			consumes= MediaType.APPLICATION_JSON_VALUE,
			produces="application/json; charset=utf-8")
	public String addUserRole(@RequestParam("userName") String userName, 
			@RequestParam("role") String role) throws UserRetrievalException {
		
		
		User fetchedUser = userManager.getByUserName(userName);
		String isSuccess = String.valueOf(false);
		
		if (fetchedUser == null){
			throw new UserRetrievalException("User not found");
		} else {
			UserImpl user = (UserImpl) fetchedUser;
			userManager.addRole(user, role);
			userManager.update(user);
			isSuccess = String.valueOf(true);
		}
		
		return "{\"roleAdded\" : "+isSuccess+" }";
		
	}
	
	@RequestMapping(value="/init_admin", method= RequestMethod.GET)
	public void initAdminUser() {
		registrar.createAdminUser();
	}
	
	
}
