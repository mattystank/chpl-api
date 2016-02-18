package gov.healthit.chpl.entity;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;


/** 
 * Object mapping for hibernate-handled table: vendor_contact_map.
 * 
 *
 * @author autogenerated
 */

@Embeddable
public class DeveloperContactMapPK implements Cloneable, Serializable {

	/** Serial Version UID. */
	private static final long serialVersionUID = -4117814996302259798L;

	

	/** Field mapping. */
	@ManyToOne( cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY )
	@org.hibernate.annotations.Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE})
	@Basic( optional = false )
	@JoinColumn(name = "contact_id", nullable = false , insertable = false, updatable = false )
	private ContactEntity contact;

	/** Field mapping. */
	@ManyToOne( cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY )
	@org.hibernate.annotations.Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE})
	@Basic( optional = false )
	@JoinColumn(name = "vendor_id_vendor", nullable = false , insertable = false, updatable = false )
	private DeveloperEntity developerId;

 


 
	/** Return the type of this class. Useful for when dealing with proxies.
	* @return Defining class.
	*/
	@Transient
	public Class<?> getClassType() {
		return DeveloperContactMapPK.class;
	}
 

	 /**
	 * Return the value associated with the column: contact.
	 * @return A Contact object (this.contact)
	 */
	public ContactEntity getContact() {
		return this.contact;
		
	}
	

  
	 /**  
	 * Set the value related to the column: contact.
	 * @param contact2 the contact value you wish to set
	 */
	public void setContact(final ContactEntity contact2) {
		this.contact = contact2;
	}

	 /**
	 * Return the value associated with the column: developerIdDeveloper.
	 * @return A Developer object (this.vendorIdVendor)
	 */
	public DeveloperEntity getDeveloperId() {
		return this.developerId;
		
	}
	

  
	 /**  
	 * Set the value related to the column: developerIdDeveloper.
	 * @param developerIdDeveloper the developerIdDeveloper value you wish to set
	 */
	public void setDeveloperId(final DeveloperEntity developerId) {
		this.developerId = developerId;
	}


   /**
    * Deep copy.
	* @return cloned object
	* @throws CloneNotSupportedException on error
    */
    @Override
    public DeveloperContactMapPK clone() throws CloneNotSupportedException {
		
        final DeveloperContactMapPK copy = (DeveloperContactMapPK)super.clone();

		return copy;
	}
	


	/** Provides toString implementation.
	 * @see java.lang.Object#toString()
	 * @return String representation of this class.
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		return sb.toString();		
	}


	/** Equals implementation. 
	 * @see java.lang.Object#equals(java.lang.Object)
	 * @param aThat Object to compare with
	 * @return true/false
	 */
	@Override
	public boolean equals(final Object aThat) {
		Object proxyThat = aThat;
		
		if ( this == aThat ) {
			 return true;
		}

		if (aThat == null)  {
			 return false;
		}
		
		final DeveloperContactMapPK that; 
		try {
			that = (DeveloperContactMapPK) proxyThat;
			if ( !(that.getClassType().equals(this.getClassType()))){
				return false;
			}
		} catch (org.hibernate.ObjectNotFoundException e) {
				return false;
		} catch (ClassCastException e) {
				return false;
		}
		
		
		boolean result = true;
		result = result && (((getContact() == null) && (that.getContact() == null)) || (getContact() != null && getContact().getId().equals(that.getContact().getId())));	
		result = result && (((getDeveloperId() == null) && (that.getDeveloperId() == null)) || (getDeveloperId() != null && getDeveloperId().getId().equals(that.getDeveloperId().getId())));	
		return result;
	}
	
	/** Calculate the hashcode.
	 * @see java.lang.Object#hashCode()
	 * @return a calculated number
	 */
	@Override
	public int hashCode() {
	int hash = 0;
		hash = hash + getContact().hashCode();
		hash = hash + getDeveloperId().hashCode();
	return hash;
	}
	

	
}
