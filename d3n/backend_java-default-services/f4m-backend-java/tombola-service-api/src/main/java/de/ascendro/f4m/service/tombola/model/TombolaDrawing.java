package de.ascendro.f4m.service.tombola.model;

import java.util.List;

public class TombolaDrawing {

    private String tombolaId;
    private String name;
    private String drawDate;
    private int totalTicketsNumber;
    private String imageId;
    private List<TombolaWinner> winners;

    public TombolaDrawing() {}

    public TombolaDrawing(String tombolaId, String name, String drawDate, String imageId, int totalTicketsNumber) {
        this.tombolaId = tombolaId;
        this.name = name;
        this.drawDate = drawDate;
        this.imageId = imageId;
        this.totalTicketsNumber = totalTicketsNumber;
    }

    public TombolaDrawing(Tombola tombola, int totalTicketsNumber, List<TombolaWinner> winners, String drawDate) {
        setTombola(tombola);
        setTotalTicketsNumber(totalTicketsNumber);
        setWinners(winners);
        setDrawDate(drawDate);
    }

    public void setTombola(Tombola tombola) {
        setTombolaId(tombola.getId());
        setName(tombola.getName());
        setImageId(tombola.getImageId());
    }

    public String getTombolaId() {
        return tombolaId;
    }

    public void setTombolaId(String tombolaId) {
        this.tombolaId = tombolaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(String drawDate) {
        this.drawDate = drawDate;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public int getTotalTicketsNumber() {
        return totalTicketsNumber;
    }

    public void setTotalTicketsNumber(int totalTicketsNumber) {
        this.totalTicketsNumber = totalTicketsNumber;
    }

    public List<TombolaWinner> getWinners() {
        return winners;
    }

    public void setWinners(List<TombolaWinner> winners) {
        this.winners = winners;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("TombolaDrawing{")
                .append("tombolaId='").append(tombolaId).append('\'')
                .append(", name='").append(name).append('\'')
                .append(", drawDate='").append(drawDate).append('\'')
                .append(", totalTicketsNumber=").append(totalTicketsNumber)
                .append(", imageId='").append(imageId).append('\'')
                .append(", winners=").append(winners)
                .append('}').toString();
    }
}
