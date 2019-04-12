package de.ascendro.f4m.service.tombola.model;

import java.math.BigDecimal;
import java.util.Objects;

import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.voucher.model.Voucher;

public class TombolaWinner {

    private String prizeId;
    private int ticketId;
    private String appId;
    private String ticketCode;
    private String name;
    private PrizeType type;
    private BigDecimal amount;
    private String voucherId;
    private String imageId;
    private ApiProfileBasicInfo user = new ApiProfileBasicInfo();
    private Voucher voucher;

    /** @deprecated use user instead */
	@Deprecated
    private String userId;

    public TombolaWinner(Prize prize, TombolaTicket ticket, ApiProfileBasicInfo user) {
        this.prizeId = prize.getId();
        this.ticketId = ticket.getId();
        this.appId = ticket.getAppId();
        this.ticketCode = ticket.getCode();
        this.name = prize.getName();
        this.type = prize.getType();
        this.amount = prize.getAmount();
        this.voucherId = prize.getVoucherId();
        this.imageId = prize.getImageId();
        this.user = user;
        
        this.userId = ticket.getUserId();
    }

    public String getPrizeId() {
        return prizeId;
    }

    public void setPrizeId(String prizeId) {
        this.prizeId = prizeId;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PrizeType getType() {
        return type;
    }

    public void setType(PrizeType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
    
    public ApiProfileBasicInfo getUser() {
		return user;
	}

	public void setUser(ApiProfileBasicInfo user) {
		this.user = user;
		
		this.userId = user.getUserId();
	}

    public Voucher getVoucher() {
        return voucher;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }

    @Deprecated
    public String getUserId() {
        return userId;
    }

	@Deprecated
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TombolaWinner)) return false;
        TombolaWinner that = (TombolaWinner) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TombolaWinner [prizeId=");
		builder.append(prizeId);
		builder.append(", ticketId=");
		builder.append(ticketId);
		builder.append(", appId=");
		builder.append(appId);
		builder.append(", ticketCode=");
		builder.append(ticketCode);
		builder.append(", name=");
		builder.append(name);
		builder.append(", type=");
		builder.append(type);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", voucherId=");
		builder.append(voucherId);
		builder.append(", imageId=");
		builder.append(imageId);
		builder.append(", user=");
		builder.append(user);
		builder.append(", userId=");
		builder.append(userId);
		builder.append("]");
		return builder.toString();
	}
}
