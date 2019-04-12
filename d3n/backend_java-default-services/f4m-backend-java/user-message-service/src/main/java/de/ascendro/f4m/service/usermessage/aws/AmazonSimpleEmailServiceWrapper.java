package de.ascendro.f4m.service.usermessage.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;

import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperResponse;

/**
 * Wrapper allowing to use Amazon Simple Email Service for sending e-mails.
 */
public class AmazonSimpleEmailServiceWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(AmazonSimpleEmailServiceWrapper.class);
	private AmazonSimpleEmailService sesClient;
	private UserMessageConfig config;

	@Inject
	public AmazonSimpleEmailServiceWrapper(AmazonSimpleEmailService sesClient, UserMessageConfig config) {
		//how / should we get a new instance of sesClient every time? Is it thread-safe?
		this.sesClient = sesClient;
		this.config = config;
	}

	/**
	 * Sends an e-mail using AWS SES
	 * 
	 * @param emailRequest
	 * @return
	 */
	public SendEmailWrapperResponse sendEmail(String toAddress, String subject, String messageText) {
		Destination destination = new Destination().withToAddresses(toAddress);
		Content subjectContent = new Content(subject);
		Content text = new Content().withData(messageText);
		Body body = new Body().withHtml(text);
		Message message = new Message().withSubject(subjectContent).withBody(body);
		String f4mSourceEmail = config.getProperty(UserMessageConfig.F4M_SENDER_EMAIL);
		SendEmailRequest awsRequest = new SendEmailRequest()
				.withSource(f4mSourceEmail)
				.withDestination(destination)
				.withMessage(message);
		SendEmailResult sendEmailResult = sesClient.sendEmail(awsRequest);
		SendEmailWrapperResponse response = new SendEmailWrapperResponse();
		response.setMessageId(sendEmailResult.getMessageId());
		return response;
	}
}
