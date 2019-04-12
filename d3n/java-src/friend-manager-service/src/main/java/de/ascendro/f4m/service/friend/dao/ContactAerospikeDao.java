package de.ascendro.f4m.service.friend.dao;

import java.util.List;

import de.ascendro.f4m.service.friend.model.Contact;

public interface ContactAerospikeDao {

	void createOrUpdateContact(Contact contact);
	
	Contact getContact(String userId, String contactId);
	
	int resyncIndex();

	void moveContacts(List<Contact> contacts, String userId);

}
