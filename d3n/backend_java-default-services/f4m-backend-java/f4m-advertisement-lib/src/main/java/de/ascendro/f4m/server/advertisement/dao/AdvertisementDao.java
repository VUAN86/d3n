package de.ascendro.f4m.server.advertisement.dao;

import java.util.List;

public interface AdvertisementDao {

	Integer getAdvertisementCount(long providerId);

	void addAdvertisementShown(String gameInstanceId, String advertisementBlobKey);

}