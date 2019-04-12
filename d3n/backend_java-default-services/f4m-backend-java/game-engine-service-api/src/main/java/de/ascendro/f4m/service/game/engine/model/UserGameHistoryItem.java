package de.ascendro.f4m.service.game.engine.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.game.selection.model.game.Game;

import java.time.ZonedDateTime;
import java.util.Objects;

public class UserGameHistoryItem  implements JsonMessageContent, Comparable<UserGameHistoryItem>{

    private String gameInstanceId;
    private String multiplayerGameInstanceId;
    private String gameId;
    private String gameTitle;
    private String appId;
//    private String appTitle;
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

    public UserGameHistoryItem() {
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
    }

    public String getAppId() {

        return appId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public void setGameTitle(String gameTitle) {
        this.gameTitle = gameTitle;
    }

//    public void setAppTitle(String appTitle) {
//        this.appTitle = appTitle;
//    }

    public String getGameId() {

        return gameId;
    }

    public String getGameTitle() {
        return gameTitle;
    }

//    public String getAppTitle() {
//        return appTitle;
//    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public ZonedDateTime getDate() {

        return date;
    }

    public void setGameInstanceId(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
        this.multiplayerGameInstanceId = multiplayerGameInstanceId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGameInstanceId() {

        return gameInstanceId;
    }

    public String getMultiplayerGameInstanceId() {
        return multiplayerGameInstanceId;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public int compareTo(UserGameHistoryItem o) {
        int direction = 1;
        if (OrderBy.Direction.desc.equals(this.orderBy.getDirection())) direction =-1;
        switch (this.orderBy.getField()){
//            case ("gameInstanceId"):
//                int isEqual = this.gameInstanceId.compareTo(o.getGameInstanceId()) * direction;
//                if (isEqual==0){
//                    return this.transactionId.compareTo(o.getTransactionId()) * direction;
//                } else {
//                    return isEqual;
//                }
//            case ("multiplayerGameInstanceId"):
//                return this.multiplayerGameInstanceId.compareTo(o.getMultiplayerGameInstanceId()) * direction;
//            case ("userName"):
//                return this.userName.compareTo(o.getUserName()) * direction;
            case ("status"):
                return this.status.compareTo(o.getStatus()) * direction;
//            case ("transactionId"):
//                return this.transactionId.compareTo(o.getTransactionId()) * direction;
//            case ("transactionReason"):
//                return this.transactionReason.compareTo(o.getTransactionReason()) * direction;
//            case ("specialPrizeTitle"):
//                return this.specialPrizeTitle.compareTo(o.getSpecialPrizeTitle()) * direction;
//            case ("winningTitle"):
//                return this.winningTitle.compareTo(o.getWinningTitle()) * direction;
            case ("date"):
//                return this.winningTitle.compareTo(o.getWinningTitle()) * direction;
                if (this.date == null && o.getDate() == null){
                    return 0;
                } else if (this.date != null && o.getDate() == null) {
                    return 1;
                } else if(this.date == null && o.getDate() != null){
                    return -1;
                }
                return this.date.compareTo(o.getDate()) * direction;
//            case ("handicap"):
//                return Double.compare(this.getHandicap() , o.handicap) * direction;
//            case ("correstAnswers"):
//                return Integer.compare(this.getCorrestAnswers() , o.correstAnswers) * direction;
//            case ("wrongAnswers"):
//                return Integer.compare(this.getWrongAnswers() , o.wrongAnswers) * direction;
//            case ("skipped"):
//                return Integer.compare(this.getSkipped() , o.skipped) * direction;
//            case ("adv"):
//                return Integer.compare(this.getAdv() , o.adv) * direction;
//            case ("vouchers"):
//                return Double.compare(this.getVouchers() , o.vouchers) * direction;
//            case ("bonus"):
//                return Double.compare(this.getBonus() , o.bonus) * direction;
//            case ("credit"):
//                return Double.compare(this.getCredit() , o.credit) * direction;
//            case ("money"):
//                return Double.compare(this.getMoney() , o.money) * direction;
        }
        return 0;
    }
}
