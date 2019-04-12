package de.ascendro.f4m.service.winning.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

import java.math.BigDecimal;
import java.util.*;

import static de.ascendro.f4m.service.util.F4MEnumUtils.getEnum;

public class WinningComponent extends JsonObjectWrapper {

	public static final String PROPERTY_TENANTS = "tenants";
	public static final String PROPERTY_WINNING_COMPONENT_ID = "winningComponentId";
	public static final String PROPERTY_GAME_TYPE = "gameType";
	public static final String PROPERTY_CASINO_COMPONENT = "casinoComponent";
	public static final String PROPERTY_IMAGE_ID = "imageId";
	public static final String PROPERTY_TITLE = "title";
	public static final String PROPERTY_RULES = "rules";
	public static final String PROPERTY_DESCRIPTION = "description";
	public static final String PROPERTY_INFO = "info";
	public static final String PROPERTY_WINNING_OPTIONS = "winningOptions";
	public static final String PROPERTY_MAX_JACKPOT_AMOUNT = "maxJackpotAmount";
	public static final String PROPERTY_MAX_JACKPOT_TYPE = "maxJackpotType";

	public WinningComponent(String[] tenants, String winningComponentId, List<WinningOption> winningOptions, String title) {
		setTenants(tenants);
		setProperty(PROPERTY_WINNING_COMPONENT_ID, winningComponentId);
		setWinningOptions(winningOptions);
		setProperty(PROPERTY_TITLE, title);
	}
	
	public WinningComponent(JsonObject configuration) {
		super(configuration);
	}

	public String getWinningComponentId() {
		return getPropertyAsString(PROPERTY_WINNING_COMPONENT_ID);
	}

	public boolean addTenant(String tenantId) {
		Set<String> tenants = getTenants();
		if (tenants == null) {
			tenants = new HashSet<>();
		}
		boolean added = tenants.add(tenantId);
		setArray(PROPERTY_TENANTS, tenants);
		return added;
	}

	public void setTenants(String... tenantsToAdd) {
		Set<String> tenants = new HashSet<>();
		setArray(PROPERTY_TENANTS, tenants);
	}
	
	public Set<String> getTenants() {
		Set<String> results = getPropertyAsStringSet(PROPERTY_TENANTS);
		return results == null ? Collections.emptySet() : results;
	}

	public WinningComponentGameType getGameType() {
		return getEnum(WinningComponentGameType.class, getPropertyAsString(PROPERTY_GAME_TYPE));
	}

	public CasinoComponentType getCasinoComponent() {
		return getEnum(CasinoComponentType.class, getPropertyAsString(PROPERTY_CASINO_COMPONENT));
	}
	
	public String getImageId() {
		return getPropertyAsString(PROPERTY_IMAGE_ID);
	}

	public String getTitle() {
		return getPropertyAsString(PROPERTY_TITLE);
	}
	
	public String getRules() {
		return getPropertyAsString(PROPERTY_RULES);
	}
	
	public String getDescription() {
		return getPropertyAsString(PROPERTY_DESCRIPTION);
	}
	
	public String getInfo() {
		return getPropertyAsString(PROPERTY_INFO);
	}

	public BigDecimal getJackpotAmount() {
		return getPropertyAsBigDecimal(PROPERTY_MAX_JACKPOT_AMOUNT);
	}

	public String getJackpotType() {
		return getPropertyAsString(PROPERTY_MAX_JACKPOT_TYPE);
	}

	public void setJackpotAmount(BigDecimal amount) {
		setProperty(PROPERTY_MAX_JACKPOT_AMOUNT, amount);
	}

	public void setJackpotType(String type) {
		setProperty(PROPERTY_MAX_JACKPOT_TYPE, type);
	}

	public List<WinningOption> getWinningOptions() {
		JsonArray winningOptions = getArray(PROPERTY_WINNING_OPTIONS);
		if (winningOptions == null || winningOptions.isJsonNull()) {
			return Collections.emptyList();
		}
		List<WinningOption> result = new ArrayList<>(winningOptions.size());
		
		winningOptions.forEach(winningOption -> {
			if (winningOption.isJsonObject()) {
				result.add(new WinningOption(winningOption.getAsJsonObject()));
			}
		});
		return result;
	}

	private void setWinningOptions(List<WinningOption> winningOptions) {
		JsonArray winningOptionArray = new JsonArray();
		winningOptions.forEach(option -> winningOptionArray.add(option.getJsonObject()));
		setProperty(PROPERTY_WINNING_OPTIONS, winningOptionArray);
	}

}
