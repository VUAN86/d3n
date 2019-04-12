package de.ascendro.f4m.service.profile.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ProfileTest {
	@Test
	public void testMergeDeviceIntoDevicesArrayOnEmptyProfile() throws Exception {
		Profile profile = new Profile();
		assertTrue(profile.mergeDeviceIntoDevicesArray("deviceUUID", new JsonObject(), "IMEI", "ONESIGNALID"));
		JsonArray devices = profile.getDevicesAsJsonArray();
		assertEquals(1, devices.size());
		JsonObject device = devices.get(0).getAsJsonObject();
		assertEquals("deviceUUID", device.get(Profile.DEVICE_UUID_PROPERTY_NAME).getAsString());
		assertEquals("IMEI", device.get(Profile.DEVICE_IMEI_PROPERTY_NAME).getAsString());
		assertEquals("ONESIGNALID", device.get(Profile.DEVICE_ONESIGNAL_PROPERTY_NAME).getAsString());
	}
}
