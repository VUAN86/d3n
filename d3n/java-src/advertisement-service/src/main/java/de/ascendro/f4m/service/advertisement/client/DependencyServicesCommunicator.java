package de.ascendro.f4m.service.advertisement.client;

public interface DependencyServicesCommunicator {

    void initiateRewardPayment(RewardPayoutRequestInfo requestInfo);
    void sendEmailToAdmin(String subject, String[] subjectParameters, String body, String[] bodyParameters);

}
