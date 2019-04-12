package de.ascendro.f4m.service.friend.model.api;

import de.ascendro.f4m.service.profile.model.Profile;

public class ApiProfile {

    private String userId;
    private Double handicap;
    private String image;
    private String name;
    private String fullName;
    private String nickname;
    private ApiProfileAddress address;

    public ApiProfile(String userId) {
    	this.userId = userId;
    }
    
	public ApiProfile(Profile profile) {
		userId = profile.getUserId();
		handicap = profile.getHandicap();
		image = profile.getImage();
		name = profile.getFullNameOrNickname();
		if (profile.isShowFullName()) {
			fullName = profile.getFullName();
		}
		nickname = profile.getNickname(); 
		address = new ApiProfileAddress(profile.getAddress());
	}

	public String getUserId() {
		return userId;
	}

	public Double getHandicap() {
		return handicap;
	}
	
	public String getImage() {
		return image;
	}

	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return fullName;
	}

	public String getNickname() {
		return nickname;
	}

	public ApiProfileAddress getAddress() {
		return address;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("userId=").append(userId);
		builder.append(", handicap=").append(handicap);
		builder.append(", image=").append(image);
		builder.append(", name=").append(name);
		builder.append(", fullName=").append(fullName);
		builder.append(", nickname=").append(nickname);
		builder.append(", address=").append(address);
		builder.append("]");
		return builder.toString();
	}
    
}
