package de.ascendro.f4m.service.tombola.model;


import java.math.BigDecimal;

public class Prize {

    private String id;
    private String name;
    private PrizeType type;
    private BigDecimal amount;
    private String voucherId;
    private String imageId;
    private int draws;

    public Prize(String id, String name, PrizeType type, BigDecimal amount, String imageId, String voucherId, int draws) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.amount = amount;
        this.imageId = imageId;
        this.voucherId = voucherId;
        this.draws = draws;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Prize{")
                .append("id='").append(id).append('\'')
                .append(", name='").append(name).append('\'')
                .append(", type=").append(type)
                .append(", amount=").append(amount)
                .append(", voucherId='").append(voucherId).append('\'')
                .append(", imageId='").append(imageId).append('\'')
                .append(", draws=").append(draws)
                .append('}').toString();
    }
}
