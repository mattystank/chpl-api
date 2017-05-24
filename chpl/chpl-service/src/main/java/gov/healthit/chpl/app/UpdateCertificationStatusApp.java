package gov.healthit.chpl.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import gov.healthit.chpl.dao.CertificationBodyDAO;
import gov.healthit.chpl.dao.CertifiedProductDAO;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.KeyValueModel;
import gov.healthit.chpl.domain.ListingUpdateRequest;
import gov.healthit.chpl.domain.concept.CertificationEditionConcept;
import gov.healthit.chpl.dto.CertificationBodyDTO;
import gov.healthit.chpl.dto.CertifiedProductDTO;
import gov.healthit.chpl.dto.CertifiedProductDetailsDTO;
import gov.healthit.chpl.entity.CertificationStatusType;
import gov.healthit.chpl.manager.SearchMenuManager;

@Component("updateCertificationStatusApp")
public class UpdateCertificationStatusApp extends App {
	private static final String CERTIFICATION_NAME = "CCHIT"; 
	
    private CertificationBodyDAO certificationBodyDAO;
	private CertifiedProductDAO certifiedProductDAO;
	private SearchMenuManager searchMenuManager;
	
	private static final Logger logger = LogManager.getLogger(UpdateCertificationStatusApp.class);
	
	public static void main(String[] args) throws Exception {
		// setup application
		UpdateCertificationStatusApp updateCertStatus = new UpdateCertificationStatusApp();
		Properties props = updateCertStatus.getProperties();
		updateCertStatus.setLocalContext(props);
		AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		updateCertStatus.initiateSpringBeans(context, props);
		
		// Get Certification Body for CCHIT
		CertificationBodyDTO cbDTO = updateCertStatus.getCertificationBody(CERTIFICATION_NAME);
		// Get certification edition for year 2014
		KeyValueModel certificationEdition = updateCertStatus.getCertificationEdition(CertificationEditionConcept.CERTIFICATION_EDITION_2014.getYear());
		// Get Map<String, Object> certificationStatus for "Withdrawn by Developer"
		Map<String, Object> updatedCertificationStatus = updateCertStatus.getCertificationStatus(CertificationStatusType.WithdrawnByDeveloper);
		// Get all listings certified by CCHIT with 2014 edition and 'Retired' (216 total according to spreadsheet/DB)
		List<CertifiedProductDTO> listings = updateCertStatus.getListings(cbDTO.getName(), certificationEdition, CertificationStatusType.Retired);
		// Get authentication token for REST call to API
		String token = updateCertStatus.getToken(props);
		// Get Map<CertifiedProductDTO, ListingUpdateRequest> for update
		Map<CertifiedProductDTO, ListingUpdateRequest> listingUpdatesMap = updateCertStatus.getListingUpdateRequests(listings, updatedCertificationStatus, props, token);
		// Update each listing's certification status to 'Withdrawn by Developer'
		updateCertStatus.updateListingsCertificationStatus(cbDTO.getId(), listingUpdatesMap, token, props);
	}

	@Override
	protected void initiateSpringBeans(AbstractApplicationContext context, Properties props) {
		this.setCertificationBodyDAO((CertificationBodyDAO)context.getBean("certificationBodyDAO"));
		this.setCertifiedProductDAO((CertifiedProductDAO)context.getBean("certifiedProductDAO"));
		this.setSearchMenuManager((SearchMenuManager)context.getBean("searchMenuManager"));
	}
	
	private List<CertifiedProductDTO> getListings(String certificationBodyName, KeyValueModel certificationEdition, CertificationStatusType certificationStatusType) throws EntityRetrievalException{
		List<CertifiedProductDTO> cps = new ArrayList<CertifiedProductDTO>();
		List<CertifiedProductDetailsDTO> allCpDetails = certifiedProductDAO.findAll();
		for(CertifiedProductDetailsDTO dto : allCpDetails){
			if(dto.getCertificationBodyName().equalsIgnoreCase(certificationBodyName) 
					&& dto.getCertificationEditionId().longValue() == certificationEdition.getId() 
					&& dto.getCertificationStatusName().equalsIgnoreCase(certificationStatusType.getName())){
				CertifiedProductDTO cpDTO = certifiedProductDAO.getById(dto.getId());
				cps.add(cpDTO);
			}
		}
		logger.info("Found " + cps.size() + " listings for " + certificationBodyName + " with edition " + certificationEdition + " and status " + certificationStatusType.getName());
		return cps;
	}

	private void updateListingsCertificationStatus(Long acbId, Map<CertifiedProductDTO, ListingUpdateRequest> cpUpdateMap, String token, Properties props) throws JsonProcessingException, EntityRetrievalException, EntityCreationException{
		for(CertifiedProductDTO cpDTO : cpUpdateMap.keySet()){
			String url = props.getProperty("chplUrlBegin") + props.getProperty("basePath") + props.getProperty("updateCertifiedProduct");
			logger.info("Making REST HTTP POST call to " + url +
					" using API-key=" + props.getProperty("apiKey"));
			ObjectMapper mapper = new ObjectMapper();
			ListingUpdateRequest updateRequest = cpUpdateMap.get(cpDTO);
			String json = mapper.writeValueAsString(updateRequest);
			logger.info("Updating CP with id " + cpDTO.getId());
			try{
				String result = Request.Post(url)
						.version(HttpVersion.HTTP_1_1)
						.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
						.addHeader("API-key", props.getProperty("apiKey"))
						.addHeader("Authorization", "Bearer " + token)
						.bodyString(json, ContentType.APPLICATION_JSON)
						.execute().returnContent().asString();
				logger.info("Retrieved result of " + url + " as follows: \n" + result);
			} catch (IOException e){
				logger.info("Failed to make call to " + url + 
						" using API-key=" + props.getProperty("apiKey"));
			}
			logger.info("Updated CP " + cpDTO.getChplProductNumber() + " for acb id " + acbId + 
					" to Certification Status = '" + cpUpdateMap.get(cpDTO).getListing().getCertificationStatus() + "'.");
		}
	}
	
	private CertificationBodyDTO getCertificationBody(String certificationBodyName){
		List<CertificationBodyDTO> cbDTOs = this.certificationBodyDAO.findAll(true);
		for(CertificationBodyDTO dto : cbDTOs){
			if(dto.getName().equalsIgnoreCase(certificationBodyName)){
				return dto;
			}
		}
		return null;
	}
	
	private KeyValueModel getCertificationEdition(String certificationEditionYear){
		Set<KeyValueModel> editionNames = this.searchMenuManager.getEditionNames(true);
		for(KeyValueModel editionName : editionNames){
			if(editionName.getName().equalsIgnoreCase(certificationEditionYear)){
				return editionName;
			}
		}
		return null;
	}
	
	private Map<String, Object> getCertificationStatus(CertificationStatusType certificationStatusType){
		Set<KeyValueModel> certStatuses = searchMenuManager.getCertificationStatuses();
		Map<String, Object> certStatusMap = new HashMap<String, Object>();
		for(KeyValueModel certStatus : certStatuses){
			if(certStatus.getName().equalsIgnoreCase(certificationStatusType.getName())){
				certStatusMap.put("date", certStatus.getDescription());
				certStatusMap.put("name", certStatus.getName());
				certStatusMap.put("id", certStatus.getId().toString());
				return certStatusMap;
			}
		}
		return certStatusMap;
	}
	
	private Map<CertifiedProductDTO, ListingUpdateRequest> getListingUpdateRequests(List<CertifiedProductDTO> cpDTOs, Map<String, Object> newCertificationStatus, Properties props, String token) throws JsonParseException, JsonMappingException, IOException{
		Map<CertifiedProductDTO, ListingUpdateRequest> listingUpdatesMap = new HashMap<CertifiedProductDTO, ListingUpdateRequest>();
		for(CertifiedProductDTO dto : cpDTOs){
			String urlRequest = props.getProperty("chplUrlBegin") + props.getProperty("basePath") + String.format(props.getProperty("getCertifiedProductDetails"), dto.getId().toString());
			String result = null;
			logger.info("Making REST HTTP GET call to " + urlRequest +
					" using API-key=" + props.getProperty("apiKey"));
			try{
				result = Request.Get(urlRequest)
						.version(HttpVersion.HTTP_1_1)
						.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
						.addHeader("API-key", props.getProperty("apiKey"))
						.addHeader("Authorization", "Bearer " + token)
						.execute().returnContent().asString();
				logger.info("Retrieved result of " + urlRequest + " as follows: \n" + result);
				// convert json to CertifiedProductSearchDetails object
				ObjectMapper mapper = new ObjectMapper();
				CertifiedProductSearchDetails cpDetails = mapper.readValue(result, CertifiedProductSearchDetails.class);
				ListingUpdateRequest listingUpdate = new ListingUpdateRequest();
				listingUpdate.setBanDeveloper(false);
				listingUpdate.setListing(cpDetails);
				listingUpdate.getListing().setCertificationStatus(newCertificationStatus);
				listingUpdatesMap.put(dto, listingUpdate);
			} catch (IOException e){
				logger.info("Failed to make call to " + urlRequest + 
						" using API-key=" + props.getProperty("apiKey"));
			}
		}
		return listingUpdatesMap;
	}
	
	private String getToken(Properties props) {
		String url = props.getProperty("chplUrlBegin") + props.getProperty("basePath") + props.getProperty("authenticate");
		logger.info("Making REST HTTP POST call to " + url + 
				" using API-key=" + props.getProperty("apiKey"));
		String token = null;
		try{
			String tokenResponse = Request.Post(url)
					.bodyString("{ \"userName\": \"" + props.getProperty("username") + "\","
							+ " \"password\": \"" + props.getProperty("password") + "\" }", ContentType.APPLICATION_JSON)
					.version(HttpVersion.HTTP_1_1)
					.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
					.addHeader("API-key", props.getProperty("apiKey"))
					.execute().returnContent().asString();
					JsonObject jobj = new Gson().fromJson(tokenResponse, JsonObject.class);
					logger.info("Retrieved the following JSON from " + url + ": \n" + jobj.toString());
					token = jobj.get("token").toString();
					logger.info("Retrieved token " + token);
					return token;
		} catch (IOException e){
			logger.info("Failed to make call to " + url +
					" using API-key=" + props.getProperty("apiKey"));
		}
		return token;
	}

	public SearchMenuManager getSearchMenuManager() {
		return searchMenuManager;
	}

	public void setSearchMenuManager(SearchMenuManager searchMenuManager) {
		this.searchMenuManager = searchMenuManager;
	}

	public CertificationBodyDAO getCertificationBodyDAO() {
		return certificationBodyDAO;
	}

	public void setCertificationBodyDAO(CertificationBodyDAO certificationBodyDAO) {
		this.certificationBodyDAO = certificationBodyDAO;
	}

	public CertifiedProductDAO getCertifiedProductDAO() {
		return certifiedProductDAO;
	}

	public void setCertifiedProductDAO(CertifiedProductDAO certifiedProductDAO) {
		this.certifiedProductDAO = certifiedProductDAO;
	}

}
