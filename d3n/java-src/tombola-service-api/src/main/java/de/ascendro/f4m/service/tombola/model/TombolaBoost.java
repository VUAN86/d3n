package de.ascendro.f4m.service.tombola.model;

public class TombolaBoost {

    private int id;
    private int boostSize;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBoostSize() {
        return boostSize;
    }

    public void setBoostSize(int boostSize) {
        this.boostSize = boostSize;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("TombolaBoost{")
                .append("id='").append(id).append('\'')
                .append(", boostSize=").append(boostSize)
                .append('}').toString();
    }
}

