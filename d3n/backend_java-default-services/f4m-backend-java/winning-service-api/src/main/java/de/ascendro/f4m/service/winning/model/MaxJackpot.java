package de.ascendro.f4m.service.winning.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaxJackpot {

    private static Map<String,Integer> curMap = new HashMap();

    static{
        curMap.put("VOUCHER",4);
        curMap.put("MONEY",3);
        curMap.put("CREDIT",2);
        curMap.put("CREDITS",2);
        curMap.put("BONUS",1);
        curMap.put("",0);
    }

    private String currency;
    private BigDecimal amount;

    public MaxJackpot() {
        this.currency = "";
        this.amount = BigDecimal.ZERO;
    }

    public void addJackpot(String currency, BigDecimal amount, boolean isWinningComponent) {
        if (curMap.get(currency)>curMap.get(this.currency)) {
            this.currency = currency;
            this.amount = amount;
            return;
        }
        if (curMap.get(currency)==curMap.get(this.currency)) {
            if (!isWinningComponent && "VOUCHER".equals(currency) ||
                    (isWinningComponent  && !"VOUCHER".equals(currency) && amount.compareTo(this.amount)==1)){
                this.amount = amount;
            } else if (!isWinningComponent){
                this.amount = this.amount.add(amount);
            }
            return;
        }
    }

    public void addWinningComponentJackpot(WinningComponent winningComponent){
        List<WinningOption> optionList = winningComponent.getWinningOptions();
        if (optionList!=null){
            for (WinningOption option : optionList){
                if (option.getType()!=null &&
                        (option.getAmount()!=null || (option.getPrizeId()!=null && "VOUCHER".equals(option.getType().name())))){
                    BigDecimal amount;
                    if ("VOUCHER".equals(option.getType().name())){
                        amount = new BigDecimal(option.getPrizeId());
                    } else {
                        amount =option.getAmount();
                    }
                    this.addJackpot(option.getType().name(),amount,true);
                }
            }
        }
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
