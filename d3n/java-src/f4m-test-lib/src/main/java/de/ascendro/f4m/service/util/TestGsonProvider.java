package de.ascendro.f4m.service.util;

import com.google.gson.Gson;

import de.ascendro.f4m.service.di.GsonProvider;

public class TestGsonProvider extends GsonProvider {
	private static final Gson GSON = JsonTestUtil.getGson();
	
	public TestGsonProvider() {
		super(null);
	}


	@Override
	public Gson get() {
		return GSON;
	}
}
