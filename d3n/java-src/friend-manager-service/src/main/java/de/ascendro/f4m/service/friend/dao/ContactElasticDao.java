package de.ascendro.f4m.service.friend.dao;

import java.util.List;

import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.json.model.ListResult;

public interface ContactElasticDao {

	void createOrUpdate(Contact contact);

	void delete(String contactId, String userId, boolean wait, boolean silent);

	ListResult<Contact> getContactList(String ownerId, String appId, boolean hasApp, String tenantId, String searchTerm, 
			Boolean hasInvitation, Boolean hasProfile, String[] includeUserIds, String[] excludeUserIds, int limit, long offset);

	List<Contact> listAllContacts(String userId);
	
	List<Contact> listContacts(String ownerId, String[] contactIds);

}
