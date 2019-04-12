package de.ascendro.f4m.service.json.model;

import de.ascendro.f4m.service.request.RequestInfo;

public class MessageSource {
    private String originalQueue;
    private RequestInfo originalRequestInfo;
    private Long seq;

    public MessageSource(String originalQueue) {
        this.originalQueue = originalQueue;
    }

    public MessageSource(String originalQueue, RequestInfo originalRequestInfo, Long seq) {
        this.originalQueue = originalQueue;
        this.originalRequestInfo = originalRequestInfo;
        this.seq = seq;
    }

    public Long getSeq() {
        return seq;
    }

    public String getOriginalQueue() {
        return originalQueue;
    }

    public RequestInfo getOriginalRequestInfo() {
        return originalRequestInfo;
    }

    public MessageSource setOriginalQueue(String originalQueue) {
        this.originalQueue = originalQueue;
        return this;
    }

    public MessageSource setOriginalRequestInfo(RequestInfo originalRequestInfo) {
        this.originalRequestInfo = originalRequestInfo;
        return this;
    }

    public MessageSource setSeq(Long seq) {
        this.seq = seq;
        return this;
    }

    @Override
    public String toString() {
        return "MessageSource{" +
                "originalQueue='" + originalQueue + '\'' +
                ", originalRequestInfo=" + originalRequestInfo +
                ", seq=" + seq +
                '}';
    }
}
