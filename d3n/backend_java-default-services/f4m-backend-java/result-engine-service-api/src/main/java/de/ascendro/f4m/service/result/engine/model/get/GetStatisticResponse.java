package de.ascendro.f4m.service.result.engine.model.get;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

import java.util.ArrayList;
import java.util.List;

public class GetStatisticResponse implements JsonMessageContent {
    private List<GameStatisticItem> statistic = new ArrayList<GameStatisticItem>();
    private long limit;
    private long offset;
    private long total;

    public GetStatisticResponse() {
        // Initialize empty response
    }

    public GetStatisticResponse(List<GameStatisticItem> statistic, long limit, long offset, long total) {
        this.statistic = statistic;
        this.limit = limit;
        this.offset = offset;
        this.total = total;
    }

    public void setStatistic(List<GameStatisticItem> statistic) {
        this.statistic = statistic;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setOffset(long offset) {

        this.offset = offset;
    }

    public void setLimit(long limit) {

        this.limit = limit;
    }

    public long getTotal() {
        return total;
    }

    public long getOffset() {

        return offset;
    }

    public long getLimit() {

        return limit;
    }

    public List<GameStatisticItem> getStatistic() {

        return statistic;
    }

    @Override
    public String toString() {
        return "GetStatisticResponse{" +
                "statistic=" + statistic +
                '}';
    }
}
