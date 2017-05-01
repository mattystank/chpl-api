package gov.healthit.chpl.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.healthit.chpl.domain.notification.Recipient;
import gov.healthit.chpl.domain.notification.Subscription;
import gov.healthit.chpl.dto.CertificationBodyDTO;
import gov.healthit.chpl.dto.notification.NotificationTypeDTO;
import gov.healthit.chpl.dto.notification.NotificationTypeRecipientMapDTO;
import gov.healthit.chpl.dto.notification.RecipientDTO;
import gov.healthit.chpl.dto.notification.RecipientWithSubscriptionsDTO;
import gov.healthit.chpl.dto.notification.SubscriptionDTO;
import gov.healthit.chpl.manager.NotificationManager;
import gov.healthit.chpl.web.controller.results.NotificationRecipientResults;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value="notifications")
@RestController
@RequestMapping("/notifications")
public class NotificationController {
	
	private static final Logger logger = LogManager.getLogger(NotificationController.class);

	
	@Autowired NotificationManager notificationManager;

	@ApiOperation(value = "Get the list of all recipients and their associated subscriptions that are applicable to the currently logged in user")
	@RequestMapping(value = "/recipients", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
	public @ResponseBody NotificationRecipientResults getAllRecipientsWithSubscriptions() {
		List<RecipientWithSubscriptionsDTO> foundSubs = notificationManager.getAll();
		
		List<Recipient> resultRecips = new ArrayList<Recipient>();
		for(RecipientWithSubscriptionsDTO foundSub : foundSubs) {
			Recipient recip = new Recipient(foundSub);
			resultRecips.add(recip);
		}
		
		NotificationRecipientResults results = new NotificationRecipientResults();
		results.setResults(resultRecips);
		return results;
	}
	
	@ApiOperation(value = "Update the email address of a recipient with the specified id keeping all subscriptions the same")
	@RequestMapping(value = "/recipients/{recipientId}/update", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	public @ResponseBody Recipient updateRecipient(@PathVariable("recipientId") Long recipientId,
			@RequestBody Recipient updatedRecipient) {
		//updates email and recipient metadata
		RecipientDTO updatedRecip = notificationManager.updateRecipient(recipientId, updatedRecipient.getEmail());
		
		//get the current subscriptions
		RecipientWithSubscriptionsDTO existingRecipient = notificationManager.getAllForRecipient(recipientId);
		
		//figure out what subscriptions need to be deleted
		List<SubscriptionDTO> subsToRemove = new ArrayList<SubscriptionDTO>();
		List<SubscriptionDTO> existingSubs = existingRecipient.getSubscriptions();
		for(SubscriptionDTO existingSub : existingSubs) {
			boolean stillExists = false;
			for(Subscription updatedSub : updatedRecipient.getSubscriptions()) {
				if(updatedSub.getNotificationType().getId().longValue() == existingSub.getNotificationType().getId().longValue() 
					&& 
					((updatedSub.getAcb() == null && existingSub.getAcb() == null) ||
					 (updatedSub.getAcb().getId().longValue() == existingSub.getAcb().getId().longValue()))) {
					stillExists = true;
				}
			}
			
			if(!stillExists) {
				subsToRemove.add(existingSub);
			}
		}
		
		for(SubscriptionDTO subToRemove : subsToRemove) {
			NotificationTypeRecipientMapDTO delMapping = new NotificationTypeRecipientMapDTO();
			delMapping.setRecipient(updatedRecip);
			delMapping.setSubscription(subToRemove);
			notificationManager.deleteRecipientNotificationMap(delMapping);
		}
		
		//figure out what subscriptions need to be added
		List<SubscriptionDTO> subsToAdd = new ArrayList<SubscriptionDTO>();
		for(Subscription updatedSub : updatedRecipient.getSubscriptions()) {
			boolean alreadyExists = false;
			for(SubscriptionDTO existingSub : existingSubs) {
				if(updatedSub.getNotificationType().getId().longValue() == existingSub.getNotificationType().getId().longValue() 
					&& 
					((updatedSub.getAcb() == null && existingSub.getAcb() == null) ||
					 (updatedSub.getAcb().getId().longValue() == existingSub.getAcb().getId().longValue()))) {
					alreadyExists = true;
					}
			}
			if(!alreadyExists) {
				SubscriptionDTO toAdd = new SubscriptionDTO();
				if(updatedSub.getAcb() != null) {
					CertificationBodyDTO acb = new CertificationBodyDTO();
					acb.setId(updatedSub.getAcb().getId());
					toAdd.setAcb(acb);
				}
				NotificationTypeDTO type = new NotificationTypeDTO();
				type.setId(updatedSub.getNotificationType().getId());
				toAdd.setNotificationType(type);
				subsToAdd.add(toAdd);
			}
		}
		
		for(SubscriptionDTO subToAdd : subsToAdd) {
			NotificationTypeRecipientMapDTO newMapping = new NotificationTypeRecipientMapDTO();
			newMapping.setRecipient(updatedRecip);
			newMapping.setSubscription(subToAdd);
			notificationManager.addRecipientNotificationMap(newMapping);
		}
		
		return new Recipient(updatedRecip);
	}
	
	@ApiOperation(value = "Add subscription(s) to a recipient; will create the recipient in the system if they do not already exist.")
	@RequestMapping(value = "/recipients/create", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	public @ResponseBody Recipient createRecipient(@RequestBody Recipient recipientToAdd) 
		throws InvalidArgumentsException {
		if(recipientToAdd.getSubscriptions() == null || recipientToAdd.getSubscriptions().size() == 0) {
			throw new InvalidArgumentsException("At least one subscription must be included with the request.");
		}
		if(recipientToAdd.getId() == null && StringUtils.isEmpty(recipientToAdd.getEmail())) {
			throw new InvalidArgumentsException("A recipient id or email address is required to create a new subscription.");
		}
		
		for(Subscription subscriptionToAdd : recipientToAdd.getSubscriptions()) {
			NotificationTypeRecipientMapDTO dtoToAdd = new NotificationTypeRecipientMapDTO();
			RecipientDTO recip = new RecipientDTO();
			recip.setId(recipientToAdd.getId());
			recip.setEmailAddress(recipientToAdd.getEmail());
			dtoToAdd.setRecipient(recip);
			SubscriptionDTO sub = new SubscriptionDTO();
			if(subscriptionToAdd.getAcb() != null) {
				CertificationBodyDTO acbDto = new CertificationBodyDTO();
				acbDto.setId(subscriptionToAdd.getAcb().getId());
				acbDto.setName(subscriptionToAdd.getAcb().getName());
				sub.setAcb(acbDto);
			}
			if(subscriptionToAdd.getNotificationType() == null || subscriptionToAdd.getNotificationType().getId() == null) {
				throw new InvalidArgumentsException("A notification type id is required for all subscriptions to be created.");
			} else {
				NotificationTypeDTO typeDto = new NotificationTypeDTO();
				typeDto.setId(subscriptionToAdd.getNotificationType().getId());
				sub.setNotificationType(typeDto);
			}
			dtoToAdd.setSubscription(sub);
			notificationManager.addRecipientNotificationMap(dtoToAdd);
		}
		RecipientWithSubscriptionsDTO dtoResult = notificationManager.getAllForRecipient(recipientToAdd.getId());
		Recipient result = new Recipient(dtoResult);
		return result;
	}
	
	@ApiOperation(value = "Remove subscription(s) for a recipient.")
	@RequestMapping(value = "/recipients/{recipientId}/delete", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	public void deleteRecipient(@PathVariable("recipientId") Long recipientId) 
		throws InvalidArgumentsException {
		try {
			notificationManager.deleteRecipient(recipientId);
		} catch(EntityNotFoundException ex) {
			throw new InvalidArgumentsException(ex.getMessage());
		}
	}
}
