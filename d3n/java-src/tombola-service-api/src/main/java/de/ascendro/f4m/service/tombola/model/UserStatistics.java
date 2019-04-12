package de.ascendro.f4m.service.tombola.model;

public class UserStatistics {

    private int boughtTicketsNumber;
    private int winningTicketsNumber;
    private int ticketsBoughtSinceLastWin;
    private String lastWinDate;

    public UserStatistics() {
        setBoughtTicketsNumber(0);
        setWinningTicketsNumber(0);
        setLastWinDate(null);
        setTicketsBoughtSinceLastWin(0);
    }

    public int getBoughtTicketsNumber() {
        return boughtTicketsNumber;
    }

    public void setBoughtTicketsNumber(int boughtTicketsNumber) {
        this.boughtTicketsNumber = boughtTicketsNumber;
    }

    public int getWinningTicketsNumber() {
        return winningTicketsNumber;
    }

    public void setWinningTicketsNumber(int winningTicketsNumber) {
        this.winningTicketsNumber = winningTicketsNumber;
    }

    public int getTicketsBoughtSinceLastWin() {
        return ticketsBoughtSinceLastWin;
    }

    public void setTicketsBoughtSinceLastWin(int ticketsBoughtSinceLastWin) {
        this.ticketsBoughtSinceLastWin = ticketsBoughtSinceLastWin;
    }

    public String getLastWinDate() {
        return lastWinDate;
    }

    public void setLastWinDate(String lastWinDate) {
        this.lastWinDate = lastWinDate;
    }
}
