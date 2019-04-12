package de.ascendro.f4m.service.result.engine.model;

public class BalanceObj {
    private double bonus;
    private double credits;
    private double money;
    private double vouchers;

    public BalanceObj() {
        this.bonus = 0;
        this.credits = 0;
        this.money = 0;
        this.vouchers = 0;
    }

    public double getBonus() {
        return bonus;
    }

    public double getCredits() {
        return credits;
    }

    public double getMoney() {
        return money;
    }

    public double getVouchers() {
        return vouchers;
    }

    public void appendBonus(double inc){
        this.bonus+=inc;
    }

    public void appendCredits(double inc){
        this.credits+=inc;
    }

    public void appendMoney(double inc){
        this.money+=inc;
    }

    public void appendVouchers(double inc){
        this.vouchers+=inc;
    }

    @Override
    public String toString() {
        return "BalanceObj{" + "bonus=" + bonus + ", credits=" + credits + ", money=" + money + ", vouchers=" + vouchers + '}';
    }
}
