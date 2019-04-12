package de.ascendro.f4m.server.advertisement.dao;

public interface AdvertisementDao {

	Integer getAdvertisementCount(long providerId);

	void addAdvertisementShown(String gameInstanceId, String advertisementBlobKey);

}