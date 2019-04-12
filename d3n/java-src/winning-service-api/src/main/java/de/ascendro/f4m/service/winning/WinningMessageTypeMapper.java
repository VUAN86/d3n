package de.ascendro.f4m.service.winning;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignRequest;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentGetRequest;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentGetResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentListRequest;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentListResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentLoadRequest;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentLoadResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentMoveRequest;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentStartRequest;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentStartResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentStopRequest;
import de.ascendro.f4m.service.winning.model.component.WinningComponentGetRequest;
import de.ascendro.f4m.service.winning.model.component.WinningComponentGetResponse;
import de.ascendro.f4m.service.winning.model.component.WinningComponentListRequest;
import de.ascendro.f4m.service.winning.model.component.WinningComponentListResponse;
import de.ascendro.f4m.service.winning.model.winning.UserWinningGetRequest;
import de.ascendro.f4m.service.winning.model.winning.UserWinningGetResponse;
import de.ascendro.f4m.service.winning.model.winning.UserWinningListRequest;
import de.ascendro.f4m.service.winning.model.winning.UserWinningListResponse;
import de.ascendro.f4m.service.winning.model.winning.UserWinningMoveRequest;

public class WinningMessageTypeMapper extends JsonMessageTypeMapImpl {
	
	private static final long serialVersionUID = 1873445928105936744L;

	public WinningMessageTypeMapper() {
		init();
	}

	protected void init() {
		this.register(WinningMessageTypes.WINNING_COMPONENT_GET.getMessageName(), new TypeToken<WinningComponentGetRequest>() {}.getType());
		this.register(WinningMessageTypes.WINNING_COMPONENT_GET_RESPONSE.getMessageName(), new TypeToken<WinningComponentGetResponse>() {}.getType());

		this.register(WinningMessageTypes.WINNING_COMPONENT_LIST.getMessageName(), new TypeToken<WinningComponentListRequest>() {}.getType());
		this.register(WinningMessageTypes.WINNING_COMPONENT_LIST_RESPONSE.getMessageName(), new TypeToken<WinningComponentListResponse>() {}.getType());

		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_ASSIGN.getMessageName(), new TypeToken<UserWinningComponentAssignRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_ASSIGN_RESPONSE.getMessageName(), new TypeToken<UserWinningComponentAssignResponse>() {}.getType());
		
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_LIST.getMessageName(), new TypeToken<UserWinningComponentListRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_LIST_RESPONSE.getMessageName(), new TypeToken<UserWinningComponentListResponse>() {}.getType());
		
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_GET.getMessageName(), new TypeToken<UserWinningComponentGetRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_GET_RESPONSE.getMessageName(),new TypeToken<UserWinningComponentGetResponse>() {}.getType());
		
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_LOAD.getMessageName(), new TypeToken<UserWinningComponentLoadRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_LOAD_RESPONSE.getMessageName(),new TypeToken<UserWinningComponentLoadResponse>() {}.getType());
		
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_START.getMessageName(), new TypeToken<UserWinningComponentStartRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_START_RESPONSE.getMessageName(), new TypeToken<UserWinningComponentStartResponse>() {}.getType());
		
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_STOP.getMessageName(), new TypeToken<UserWinningComponentStopRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_STOP_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());

		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_MOVE.getMessageName(), new TypeToken<UserWinningComponentMoveRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_COMPONENT_MOVE_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());

		this.register(WinningMessageTypes.USER_WINNING_LIST.getMessageName(), new TypeToken<UserWinningListRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_LIST_RESPONSE.getMessageName(), new TypeToken<UserWinningListResponse>() {}.getType());
		
		this.register(WinningMessageTypes.USER_WINNING_GET.getMessageName(), new TypeToken<UserWinningGetRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_GET_RESPONSE.getMessageName(), new TypeToken<UserWinningGetResponse>() {}.getType());

		this.register(WinningMessageTypes.USER_WINNING_MOVE.getMessageName(), new TypeToken<UserWinningMoveRequest>() {}.getType());
		this.register(WinningMessageTypes.USER_WINNING_MOVE_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());
	}
}
