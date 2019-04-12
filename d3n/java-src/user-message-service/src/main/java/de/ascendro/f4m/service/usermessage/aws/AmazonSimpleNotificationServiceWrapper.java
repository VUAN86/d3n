package de.ascendro.f4m.service.usermessage.aws;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.usermessage.model.SendSmsResponse;

/**
 * Wrapper allowing to use Amazon Simple Email Service for sending e-mails.
 */
public class AmazonSimpleNotificationServiceWrapper {
	private static final String AWS_SNS_SMS_SENDER_ID_PARAM_NAME = "AWS.SNS.SMS.SenderID";
	private static final Logger LOGGER = LoggerFactory.getLogger(AmazonSimpleNotificationServiceWrapper.class);
	private AmazonSNS snsClient;
	private UserMessageConfig config;

	@Inject
	public AmazonSimpleNotificationServiceWrapper(AmazonSNS snsClient, UserMessageConfig config) {
		//how / should we get a new instance of snsClient every time? Is it thread-safe?
		this.snsClient = snsClient;
		this.config = config;
	}

	/**
	 * Sends an SMS using AWS SNS
	 * 
	 * @param smsRequest
	 * @return
	 */
	public SendSmsResponse sendSMS(String phone, String message) {
		//snsClient.checkIfPhoneNumberIsOptedOut(checkIfPhoneNumberIsOptedOutRequest) - is this verification needed?
		String senderId = config.getProperty(UserMessageConfig.SNS_SMS_SENDER_ID);
		
		Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();
		smsAttributes.put(AWS_SNS_SMS_SENDER_ID_PARAM_NAME, new MessageAttributeValue()
		        .withStringValue(senderId) //The sender ID shown on the device. 
		        .withDataType(String.class.getSimpleName()));
		PublishRequest awsRequest = new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(phone)
                .withMessageAttributes(smsAttributes);
		LOGGER.debug("Sending SMS message {}", awsRequest);
		PublishResult awsResult = snsClient.publish(awsRequest);
		LOGGER.debug("SMS message was sent {}", awsResult);
		SendSmsResponse response = new SendSmsResponse();
		response.setMessageId(awsResult.getMessageId());
		return response;
	}
}
