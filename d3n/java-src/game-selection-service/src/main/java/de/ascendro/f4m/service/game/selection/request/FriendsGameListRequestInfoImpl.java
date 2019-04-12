package de.ascendro.f4m.service.game.selection.request;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.session.SessionWrapper;

public class FriendsGameListRequestInfoImpl extends RequestInfoWithUserIdImpl {

	private List<String> friends;
	private Set<String> games = new HashSet<>();
	private int index;
	
	public FriendsGameListRequestInfoImpl(JsonMessage<?> sourceMessage, Object sourceSession, String userId) {
		super(sourceMessage, (SessionWrapper) sourceSession, userId);
	}

	public List<String> getFriends() {
		if (friends == null) {
			this.friends = new ArrayList<>();
		}
		return friends;
	}

	public void setFriends(List<String> friends) {
		this.index = 0;
		this.friends = friends;
	}

	public void addGames(List<String> games) {
		this.games.addAll(games);
	}

	public boolean hasNextFriend() {
		return !getFriends().isEmpty() && index < friends.size();
	}

	public String getNextFriend() {
		String friendId = null;
		if (hasNextFriend()) {
			friendId = friends.get(index++);
		}
		return friendId;
	}

	public String[] getFriendsAsArray() {
		String[] array;
		if (!getFriends().isEmpty()) {
			array = friends.toArray(new String[friends.size()]);
		} else {
			array = new String[0];
		}
		return array;
	}

	public String[] getGamesAsArray() {
		return games.toArray(new String[games.size()]);
	}

}
