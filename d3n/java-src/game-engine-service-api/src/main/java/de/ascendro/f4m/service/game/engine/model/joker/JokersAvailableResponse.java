package de.ascendro.f4m.service.game.engine.model.joker;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class JokersAvailableResponse implements JsonMessageContent {

	@JsonRequiredNullable
	private Map<String, JokerInformation> items;

	public JokersAvailableResponse(Map<JokerType, JokerInformation> items) {
		this.items = new HashMap<>(items.size());
		items.forEach((type, description) ->
				this.items.put(type.getValue(), description));
	}

	public Map<JokerType, JokerInformation> getItems() {
		Map<JokerType, JokerInformation> result = new EnumMap<>(JokerType.class);
		items.forEach((type, description) ->
				result.put(JokerType.fromString(type), description));
		return result;
	}

}
