package de.ascendro.f4m.service.usermessage.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;

/**
 * User Message Service configuration parameters
 */
public class UserMessageConfig extends F4MConfigImpl implements Config, AWSCredentialsProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserMessageConfig.class);

	
	/**
	 * Parameter name to specify OneSignal App ID
	 */
	public static final String ONE_SIGNAL_APP_ID = "oneSignalAppId";
	
	/**
	 * Parameter name to specify OneSignal App REST API Key
	 */
	public static final String ONE_SIGNAL_APP_REST_API_KEY = "oneSignalAppRestApiKey";

	/**
	 * Parameter name to specify Amazon Web Services access key
	 */
	public static final String AWS_ACCESS_KEY = "awsAccessKey";
	
	/**
	 * Parameter name to specify Amazon Web Services secret key
	 */
	public static final String AWS_SECRET_KEY = "awsSecretKey";
	
	/**
	 * Parameter name to specify Amazon Web Services region, which should be used. Default value 'US_WEST_2'.
	 */
	public static final String AWS_REGION = "awsRegion";
	
	public static final Regions AWS_REGION_DEFAULT_VALUE = Regions.US_WEST_2;
	
	/**
	 * Parameter name to specify from e-mail address for all e-mails sent using User Message Service.
	 */
	public static final String F4M_SENDER_EMAIL = "f4mSenderEmail";
	
	public static final String F4M_SENDER_EMAIL_DEFAULT_VALUE = "m.kuldyaev@f4m.tv";
	
	/**
	 * Parameter name to specify F4M logo to use as default image in emails if the company logo is not being uploaded yet
	 */
	public static final String F4M_LOGO = "f4mLogo";
	public static final String F4M_LOGO_DEFAULT_VALUE = "http://www.f4m.tv/wp-content/themes/darbas/dist/images/logo-small2.png";
	
	/**
	 * Parameter name to specify Amazon Web Services parameter SNS.SMS.SenderID.
	 * For more information and possible (currently unsupported) other parameters like AWS.SNS.SMS.MaxPrice and AWS.SNS.SMS.SMSType
	 * see <a href="http://docs.aws.amazon.com/sns/latest/dg/sms_publish-to-phone.html">Amazon documentation</a>
	 */
	public static final String SNS_SMS_SENDER_ID = "snsSmsSenderId";
	
	public static final String SNS_SMS_SENDER_ID_DEFAULT_VALUE = "F4M";
	
	public static final String AEROSPIKE_USERMESSAGE_SET = "usermessage.aerospike.set";
	
	public static final String AEROSPIKE_USERMESSAGE_SET_DEFAULT_VALUE = "usermessage";

	public UserMessageConfig() {
		super(new AerospikeConfigImpl());
		setProperty(AWS_REGION, AWS_REGION_DEFAULT_VALUE.getName());
		setProperty(F4M_SENDER_EMAIL, F4M_SENDER_EMAIL_DEFAULT_VALUE);
		setProperty(SNS_SMS_SENDER_ID, SNS_SMS_SENDER_ID_DEFAULT_VALUE);
		setProperty(AEROSPIKE_USERMESSAGE_SET, AEROSPIKE_USERMESSAGE_SET_DEFAULT_VALUE);
		setProperty(F4M_LOGO, F4M_LOGO_DEFAULT_VALUE);
		
		loadProperties();
	}

	@Override
	public AWSCredentials getCredentials() {
		String accessKey = getProperty(UserMessageConfig.AWS_ACCESS_KEY);
		String secretKey = getProperty(UserMessageConfig.AWS_SECRET_KEY);
		if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
			return new AWSCredentials() {
				@Override
				public String getAWSAccessKeyId() {
					return accessKey;
				}

				@Override
				public String getAWSSecretKey() {
					return  secretKey;
				}
			};
		} else {
			LOGGER.warn("AWS access key and secret key not set, please specify configuration keys {} and {}",
					AWS_ACCESS_KEY, AWS_SECRET_KEY);
			return null;
		}
	}

	public Region getRegion() {
		return Region.getRegion(Regions.fromName(getProperty(UserMessageConfig.AWS_REGION)));
	}

	@Override
	public void refresh() {
		// refreshing of config not supported
	}
}
