package de.ascendro.f4m.service.usermessage.aws;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.CreatePlatformApplicationResult;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SubscribeRequest;

import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class AwsSnsExplorationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(AwsSnsExplorationTest.class);
	
	private static final String EMAIL = "mikus.visendorfs@ascendro.de";
	public static final String DEFAULT_TOPIC = "DefaultTopic";
	private UserMessageConfig config;

	private AmazonSNS snsClient;
	
	@Before
	public void setUp() {
		config = new UserMessageConfig();
		//https://aws.amazon.com/articles/3605
		//9. The client mock implementations have been removed. Instead, developers are encouraged to use more flexible and full featured mock libraries, such as: EasyMock, jMock
		initSnsClient();
	}
	
	@Test
	public void testSNS() throws Exception {
        int action = Integer.valueOf(System.getProperty("testAction", "1"));
        if (action == 1) {
        	sendSingleSMS(); //needed and is working
        } else if (action == 2) {
        	sendSingleEmail(); ///not working
        } else if (action == 3) {
        	sendSingleMobilePush(); //not really tested
        } else if (action == 4) {
        	createTopic();
        } else if (action == 5) {
        	subscribeWithEmailAndSend(); //read comments carefully and specify breakpoint where necessary
        }
	}

	private void initSnsClient() {
		AWSCredentials credentials = config.getCredentials();
		assumeTrue("Skipping a test with real AWS SES, because no credentials are specified", credentials != null);
		snsClient = new AmazonSNSClient(credentials);
		snsClient.setRegion(config.getRegion());
	}
	
	private void sendSingleEmail() {
		String message = "Hello from AWS SNS via e-mail!"
				+ "<br/> on " + DateTimeUtil.getCurrentDateTime()
				+ "\n Sincerely, Mikus";
		PublishResult result = snsClient.publish(new PublishRequest()
                .withMessage(message)
                .withSubject("Subject of an e-mail message")
                .withTargetArn(EMAIL));
		//here this fails - email is not a valid endpoint
		LOGGER.info("sendSingleEmail result was {}", result); //{MessageId: e7b0d81e-1c99-52ad-8828-883586fff77a}
	}
	
	private void createTopic() {
		CreateTopicRequest createTopicRequest = new CreateTopicRequest("DEFAULT_TOPIC");
		CreateTopicResult createTopicResult = snsClient.createTopic(createTopicRequest); 
		LOGGER.info("createTopic result was {}", createTopicResult);
	}

	private void sendSingleSMS() {
		String phoneNumber = getPhoneNumber();
		
		String message = "Hello from AWS SNS on " + DateTimeUtil.getCurrentDateTime() + "!";
		//snsClient.checkIfPhoneNumberIsOptedOut(checkIfPhoneNumberIsOptedOutRequest)
		
		Map<String, MessageAttributeValue> smsAttributes = new HashMap<String, MessageAttributeValue>();
		smsAttributes.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue()
		        .withStringValue("mySenderID") //The sender ID shown on the device. 
		        .withDataType("String"));
		//http://docs.aws.amazon.com/sns/latest/dg/sms_publish-to-phone.html
		//AWS.SNS.SMS.MaxPrice
		//AWS.SNS.SMS.SMSType
		PublishResult result = snsClient.publish(new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(phoneNumber)
                .withMessageAttributes(smsAttributes));
		LOGGER.info("sendSingleSMS result was {}", result); //{MessageId: e7b0d81e-1c99-52ad-8828-883586fff77a}
	}

	private String getPhoneNumber() {
		String phoneNumber = System.getProperty("phone");
		assertTrue("Phone number not specified in parameter '-Dphone'", StringUtils.isNotBlank(phoneNumber));
		return phoneNumber;
	}
	
	private void sendSingleMobilePush() {
		String token = "A registered token";
		String message = "Hello from AWS SNS via mobile push on " + DateTimeUtil.getCurrentDateTime() + "!";
		
		CreatePlatformApplicationRequest createPlatformApplicationRequest = new CreatePlatformApplicationRequest()
				.withPlatform("APNS") //.withPlatform("GCM")
				;
		CreatePlatformApplicationResult createPlatformApplicationResult = snsClient
				.createPlatformApplication(createPlatformApplicationRequest);
		
		CreatePlatformEndpointRequest createPlatformEndpointRequest = new CreatePlatformEndpointRequest()
				.withPlatformApplicationArn(createPlatformApplicationResult.getPlatformApplicationArn())
				.withToken(token);
		CreatePlatformEndpointResult platformEndpoint = snsClient.createPlatformEndpoint(createPlatformEndpointRequest);
		
		PublishResult result = snsClient.publish(new PublishRequest()
                .withMessage(message)
                .withTargetArn(platformEndpoint.getEndpointArn()));
		LOGGER.info("sendSingleMobilePush result was {}", result); //{MessageId: e7b0d81e-1c99-52ad-8828-883586fff77a}
	}
	
	private void subscribeWithEmailAndSend() {
		//https://aws.amazon.com/sns/faqs/?nc2=h_ls
		//By default, SNS offers 10 million subscriptions per topic, and 100,000 topics per account.  To request a higher limit, please contact us at at http://aws.amazon.com/support
		
		//create a new SNS topic
		CreateTopicRequest createTopicRequest = new CreateTopicRequest("MyNewTopic-West2");
		CreateTopicResult createTopicResult = snsClient.createTopic(createTopicRequest); 
		//print TopicArn 
		LOGGER.info("Created topic {} with metadata {}", createTopicResult,
				snsClient.getCachedResponseMetadata(createTopicRequest));
		
		String topicArn = createTopicResult.getTopicArn();
		
		//subscribe to an SNS topic
		SubscribeRequest subRequest = new SubscribeRequest(topicArn, "email", EMAIL);
		snsClient.subscribe(subRequest);
		//get request id for SubscribeRequest from SNS metadata
		LOGGER.info("SubscribeRequest - {}", snsClient.getCachedResponseMetadata(subRequest));
		LOGGER.warn("Check your email and confirm subscription to continue...");

		//------------------------------------------------------------------------------------------
		//-- place a breakpoint here and wait for a subscription message in email and confirm it. --
		//------------------------------------------------------------------------------------------
		
		//publish to an SNS topic
		String msg = "My text published to SNS topic with email endpoint";
		PublishRequest publishRequest = new PublishRequest(topicArn, msg);
		PublishResult publishResult = snsClient.publish(publishRequest);
		//print MessageId of message published to SNS topic
		LOGGER.info("Published MessageId {}", publishResult.getMessageId());
		
		
		//delete an SNS topic
		DeleteTopicRequest deleteTopicRequest = new DeleteTopicRequest(topicArn);
		snsClient.deleteTopic(deleteTopicRequest);
		//get request id for DeleteTopicRequest from SNS metadata
		LOGGER.info("DeleteTopicRequest {}", snsClient.getCachedResponseMetadata(deleteTopicRequest));
	}
}
