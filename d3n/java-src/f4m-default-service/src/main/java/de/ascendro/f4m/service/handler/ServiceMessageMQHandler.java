package de.ascendro.f4m.service.handler;

import de.ascendro.f4m.service.MessageDecoder;
import de.ascendro.f4m.service.MessageEncoder;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.json.model.MessageSource;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapperFactory;

/**
 * 
 * Basic Service message handler
 * 
 * @param <D>
 *            - Decoded message type
 * @param <E>
 *            - Encoded message type
 */
public abstract class ServiceMessageMQHandler<D, E, R, M extends MessageSource>  implements  MessageDecoder<D, E> ,
		MessageEncoder<D, E> {

	protected SessionWrapperFactory sessionWrapperFactory;
	protected Config config;
	protected LoggingUtil loggingUtil;

//	private SessionWrapper sessionWrapper;

	public void onMessage(E originalMessageEncoded, M messageSource) {
		System.out.println("onMEssage"+originalMessageEncoded);
		D originalMessageDecoded = null;
		try {
			originalMessageDecoded = prepare();
			originalMessageDecoded = decode(originalMessageEncoded, originalMessageDecoded);
			logReceivedMessage(originalMessageDecoded, originalMessageEncoded);
			validate(originalMessageEncoded);
			validatePermissions(originalMessageDecoded);
			final R processResult = onProcess(originalMessageDecoded, messageSource);
			if (processResult != null) {
				System.out.println("onResponse");
				onResponse(originalMessageDecoded, processResult, messageSource);
			}
		} catch (NullPointerException nex) {
			nex.printStackTrace();
			if (nex.getCause() != null) {
				System.out.println("onFailure "+1);
				onFailure(originalMessageEncoded, originalMessageDecoded, messageSource, nex.getCause());
			} else {
				System.out.println("onFailure "+2);
				onFailure(originalMessageEncoded, originalMessageDecoded, messageSource, nex);
			}
		} catch (Exception e) {
			System.out.println("onFailure "+3);
			e.printStackTrace();
			onFailure(originalMessageEncoded, originalMessageDecoded, messageSource, e);
		}
	}

	protected abstract void logReceivedMessage(D originalMessageDecoded, E originalMessageEncoded);

//	public void sendAsyncMessage(D originalMessageDecoded) {
//		try {
//			E encodedMessage = encode(originalMessageDecoded);
//			getSessionWrapper().sendAsynText(encodedMessage.toString());
//		} catch (EncodeException e) {
//			onEncodeFailure(originalMessageDecoded, e);
//		} catch (F4MValidationException e) {
//			onFailure(null, originalMessageDecoded, e);
//		}
//	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public LoggingUtil getLoggingUtil() {
		return loggingUtil;
	}

	public void setLoggingUtil(LoggingUtil loggingUtil) {
		this.loggingUtil = loggingUtil;
	}

	public SessionWrapperFactory getSessionWrapperFactory() {
		return sessionWrapperFactory;
	}

	public void setSessionWrapperFactory(SessionWrapperFactory sessionWrapperFactory) {
		this.sessionWrapperFactory = sessionWrapperFactory;
	}

	//	public abstract ClientInfo onAuthentication(D message) throws F4MAuthenticationException;

	public abstract R onProcess(D message, MessageSource messageSource) throws F4MException;

//	public abstract void onEncodeFailure(D originalMessageDecoded, EncodeException e);

//	/**
//	 * Service Message handling failure processing by responding with error message
//	 *
//	 * @param originalMessageEncoded
//	 *            - Originally received message (might be null)
//	 * @param originalMessageDecoded
//	 *            - Decoded original message (might be null)
//	 * @param e
//	 *            - Exception occurred
//	 */
	public abstract void onFailure(E originalMessageEncoded, D originalMessageDecoded, MessageSource messageSource, Throwable e);

	public abstract void onResponse(D originalMessageDecoded, R processResultContent, MessageSource messageSource);

}
