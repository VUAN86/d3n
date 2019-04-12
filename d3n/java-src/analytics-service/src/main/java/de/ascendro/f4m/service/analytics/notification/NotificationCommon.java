package de.ascendro.f4m.service.analytics.notification;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface NotificationCommon {
    void sendEmailToAdmin(String subject, String[] subjectParameters, String body, String[] bodyParameters);
    void sendEmailToUser(JsonMessage<?> message, SessionWrapper sessionWrapper, String toUserId, String subject,
                    String content, String fromUser);
    void pushMessageToUser(String userId, String messageText, String[] params, ClientInfo clientInfo);
}
