package de.ascendro.f4m.service.friend.dao;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.friend.util.FriendPrimaryKeyUtil;

public class ContactAerospikeDaoImpl extends AerospikeOperateDaoImpl<FriendPrimaryKeyUtil>
		implements ContactAerospikeDao {

	private static final String VALUE_BIN_NAME = "value";
	
	private final ContactElasticDao contactElasticDao;
	
	@Inject
	public ContactAerospikeDaoImpl(Config config, FriendPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider, ContactElasticDao contactElasticDao) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
		this.contactElasticDao = contactElasticDao;
	}
	
	@Override
	public void createOrUpdateContact(Contact contact) {
		String key = primaryKeyUtil.createContactPrimaryKey(contact.getOwnerId(), contact.getContactId());
		createOrUpdateJson(getSet(), key, VALUE_BIN_NAME, (existing, writePolicy) -> contact.getAsString());
		contactElasticDao.createOrUpdate(contact);
	}
	
	@Override
	public Contact getContact(String userId, String contactId) {
		String key = primaryKeyUtil.createContactPrimaryKey(userId, contactId);
		String contactJson = readJson(getSet(), key, VALUE_BIN_NAME);
		return jsonUtil.toJsonObjectWrapper(contactJson, Contact::new);
	}
	
	@Override
	public void moveContacts(List<Contact> contacts, String userId) {
		contacts.forEach(contact -> {
			// Create new contact
			String key = primaryKeyUtil.createContactPrimaryKey(contact.getOwnerId(), contact.getContactId());
			Contact readContact = getContact(contact.getOwnerId(), contact.getContactId());
			readContact.setOwnerId(userId);
			createOrUpdateContact(readContact);
			
			// Delete old contact
			delete(getSet(), key);
		});
	}

	@Override
	public int resyncIndex() {
		AtomicInteger scanned = new AtomicInteger();
		getAerospikeClient().scanAll(null, getNamespace(), getSet(), (key, record) -> {
			String contactJson = readJson(VALUE_BIN_NAME, record);
			Contact contact = new Contact(jsonUtil.fromJson(contactJson, JsonObject.class));
			contactElasticDao.createOrUpdate(contact);
			scanned.incrementAndGet();
		}, VALUE_BIN_NAME);
		return scanned.get();
	}

	private String getSet() {
		return config.getProperty(FriendManagerConfig.AEROSPIKE_CONTACT_SET);
	}

}
