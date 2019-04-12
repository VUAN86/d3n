package de.ascendro.f4m.service.result.engine.model.get;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.OrderBy;

import java.time.ZonedDateTime;
import java.util.Objects;

public class GameStatisticItem implements JsonMessageContent, Comparable<GameStatisticItem> {
    private String gameInstanceId;
    private String multiplayerGameInstanceId;
    private String transactionId;
    private String transactionReason;
    private ZonedDateTime date;
    private String userId;
    private String userName;
    private double handicap;
    private int correstAnswers;
    private int wrongAnswers;
    private int skipped;
    private int adv;
    private String winningId;
    private String winningTitle;
    private double vouchers;
    private double bonus;
    private double credit;
    private double money;
    private String specialPrizeId;
    private String specialPrizeTitle;
    private String status;
    private OrderBy orderBy;

    public GameStatisticItem() {
        // Initialize empty response
    }

    public GameStatisticItem(String gameInstanceId, String multiplayerGameInstanceId, String transactionReason, ZonedDateTime date, String userId, String userName, double handicap, int correstAnswers, int wrongAnswers, int skipped, int adv, String winningId, String winningTitle, double vouchers, double bonus, double credit, double money, String specialPrizeId, String specialPrizeTitle, OrderBy orderBy) {
        this.gameInstanceId = gameInstanceId;
        this.multiplayerGameInstanceId = multiplayerGameInstanceId;
        this.transactionId = "";
        this.transactionReason = transactionReason;
        this.date = date;
        this.userId = userId;
        this.userName = userName;
        this.handicap = handicap;
        this.correstAnswers = correstAnswers;
        this.wrongAnswers = wrongAnswers;
        this.skipped = skipped;
        this.adv = adv;
        this.winningId = winningId;
        this.winningTitle = winningTitle;
        this.vouchers = vouchers;
        this.bonus = bonus;
        this.credit = credit;
        this.money = money;
        this.specialPrizeId = specialPrizeId;
        this.specialPrizeTitle = specialPrizeTitle;
        this.status = "COMPLETED";
        this.orderBy = orderBy;
    }

    public GameStatisticItem(String gameInstanceId, String multiplayerGameInstanceId, String transactionId, String transactionReason, ZonedDateTime date, String userId, String userName, double handicap, int correstAnswers, int wrongAnswers, int skipped, int adv, String winningId, String winningTitle, double vouchers, double bonus, double credit, double money, String status, OrderBy orderBy) {
        this.gameInstanceId = gameInstanceId;
        this.multiplayerGameInstanceId = multiplayerGameInstanceId;
        this.transactionId = transactionId;
        this.transactionReason = transactionReason;
        this.date = date;
        this.userId = userId;
        this.userName = userName;
        this.handicap = handicap;
        this.correstAnswers = correstAnswers;
        this.wrongAnswers = wrongAnswers;
        this.skipped = skipped;
        this.adv = adv;
        this.winningId = winningId;
        this.winningTitle = winningTitle;
        this.vouchers = vouchers;
        this.bonus = bonus;
        this.credit = credit;
        this.money = money;
        this.status = status;
        this.orderBy = orderBy;
    }

    public String getGameInstanceId() {
        return gameInstanceId;
    }

    public String getMultiplayerGameInstanceId() {
        return multiplayerGameInstanceId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTransactionReason() {
        return transactionReason;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public double getHandicap() {
        return handicap;
    }

    public int getCorrestAnswers() {
        return correstAnswers;
    }

    public int getWrongAnswers() {
        return wrongAnswers;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getAdv() {
        return adv;
    }

    public String getWinningId() {
        return winningId;
    }

    public String getWinningTitle() {
        return winningTitle;
    }

    public double getVouchers() {
        return vouchers;
    }

    public double getBonus() {
        return bonus;
    }

    public double getCredit() {
        return credit;
    }

    public double getMoney() {
        return money;
    }

    public String getSpecialPrizeId() {
        return specialPrizeId;
    }

    public String getSpecialPrizeTitle() {
        return specialPrizeTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setGameInstanceId(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
        this.multiplayerGameInstanceId = multiplayerGameInstanceId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setTransactionReason(String transactionReason) {
        this.transactionReason = transactionReason;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setHandicap(double handicap) {
        this.handicap = handicap;
    }

    public void setCorrestAnswers(int correstAnswers) {
        this.correstAnswers = correstAnswers;
    }

    public void setWrongAnswers(int wrongAnswers) {
        this.wrongAnswers = wrongAnswers;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public void setAdv(int adv) {
        this.adv = adv;
    }

    public void setWinningId(String winningId) {
        this.winningId = winningId;
    }

    public void setWinningTitle(String winningTitle) {
        this.winningTitle = winningTitle;
    }

    public void setVouchers(double vouchers) {
        this.vouchers = vouchers;
    }

    public void setBonus(double bonus) {
        this.bonus = bonus;
    }

    public void setCredit(double credit) {
        this.credit = credit;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public void setSpecialPrizeId(String specialPrizeId) {
        this.specialPrizeId = specialPrizeId;
    }

    public void setSpecialPrizeTitle(String specialPrizeTitle) {
        this.specialPrizeTitle = specialPrizeTitle;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean hasDate(){
        return getDate()!=null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameStatisticItem)) return false;
        GameStatisticItem that = (GameStatisticItem) o;
        return Double.compare(that.getHandicap(), getHandicap()) == 0 &&
                getCorrestAnswers() == that.getCorrestAnswers() &&
                getWrongAnswers() == that.getWrongAnswers() &&
                getSkipped() == that.getSkipped() &&
                getAdv() == that.getAdv() &&
                Objects.equals(getGameInstanceId(), that.getGameInstanceId()) &&
                Objects.equals(getDate(), that.getDate()) &&
                Objects.equals(getUserName(), that.getUserName()) &&
                Objects.equals(getWinningTitle(), that.getWinningTitle()) &&
                Objects.equals(getVouchers(), that.getVouchers()) &&
                Objects.equals(getBonus(), that.getBonus()) &&
                Objects.equals(getCredit(), that.getCredit()) &&
                Objects.equals(getMoney(), that.getMoney()) &&
                Objects.equals(getSpecialPrizeTitle(), that.getSpecialPrizeTitle()) &&
                Objects.equals(getStatus(), that.getStatus());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getGameInstanceId(), getDate(), getUserName(), getHandicap(), getCorrestAnswers(), getWrongAnswers(), getSkipped(), getAdv(), getWinningTitle(), getVouchers(), getBonus(), getCredit(), getMoney(), getSpecialPrizeTitle(), getStatus());
    }

    @Override
    public String toString() {
        return "GameStatisticItem{" +
                "gameInstanceId='" + gameInstanceId + '\'' +
                ", multiplayerGameInstanceId='" + multiplayerGameInstanceId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", transactionReason='" + transactionReason + '\'' +
                ", date=" + date +
                ", userId='" + userId + '\'' +
                ", userName=" + userName +
                ", handicap='" + handicap + '\'' +
                ", correstAnswers=" + correstAnswers +
                ", wrongAnswers=" + wrongAnswers +
                ", skipped=" + skipped +
                ", adv=" + adv +
                ", winningId='" + winningId + '\'' +
                ", winningTitle='" + winningTitle + '\'' +
                ", vouchers='" + vouchers + '\'' +
                ", bonus='" + bonus + '\'' +
                ", credit='" + credit + '\'' +
                ", money='" + money + '\'' +
                ", specialPrizeId='" + specialPrizeId + '\'' +
                ", specialPrizeTitle='" + specialPrizeTitle + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public int compareTo(GameStatisticItem o) {
        int direction = 1;
        if (OrderBy.Direction.desc.equals(this.orderBy.getDirection())) direction =-1;
        switch (this.orderBy.getField()){
            case ("gameInstanceId"):
                int isEqual = this.gameInstanceId.compareTo(o.getGameInstanceId()) * direction;
                if (isEqual==0){
                    return this.transactionId.compareTo(o.getTransactionId()) * direction;
                } else {
                    return isEqual;
                }
            case ("multiplayerGameInstanceId"):
                return this.multiplayerGameInstanceId.compareTo(o.getMultiplayerGameInstanceId()) * direction;
            case ("userName"):
                return this.userName.compareTo(o.getUserName()) * direction;
            case ("status"):
                return this.status.compareTo(o.getStatus()) * direction;
            case ("transactionId"):
                return this.transactionId.compareTo(o.getTransactionId()) * direction;
            case ("transactionReason"):
                return this.transactionReason.compareTo(o.getTransactionReason()) * direction;
            case ("specialPrizeTitle"):
                return this.specialPrizeTitle.compareTo(o.getSpecialPrizeTitle()) * direction;
            case ("winningTitle"):
                return this.winningTitle.compareTo(o.getWinningTitle()) * direction;
            case ("date"):
                if (this.date == null && o.getDate() == null){
                    return 0;
                } else if (this.date != null && o.getDate() == null) {
                    return 1;
                } else if(this.date == null && o.getDate() != null){
                    return -1;
                }
                return this.date.compareTo(o.getDate()) * direction;
            case ("handicap"):
                return Double.compare(this.getHandicap() , o.handicap) * direction;
            case ("correstAnswers"):
                return Integer.compare(this.getCorrestAnswers() , o.correstAnswers) * direction;
            case ("wrongAnswers"):
                return Integer.compare(this.getWrongAnswers() , o.wrongAnswers) * direction;
            case ("skipped"):
                return Integer.compare(this.getSkipped() , o.skipped) * direction;
            case ("adv"):
                return Integer.compare(this.getAdv() , o.adv) * direction;
            case ("vouchers"):
                return Double.compare(this.getVouchers() , o.vouchers) * direction;
            case ("bonus"):
                return Double.compare(this.getBonus() , o.bonus) * direction;
            case ("credit"):
                return Double.compare(this.getCredit() , o.credit) * direction;
            case ("money"):
                return Double.compare(this.getMoney() , o.money) * direction;
        }
        return 0;
    }
}