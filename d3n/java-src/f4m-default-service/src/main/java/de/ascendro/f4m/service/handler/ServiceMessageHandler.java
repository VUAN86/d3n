package de.ascendro.f4m.service.handler;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import de.ascendro.f4m.service.MessageDecoder;
import de.ascendro.f4m.service.MessageEncoder;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.auth.F4MAuthenticationException;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapper;
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
public abstract class ServiceMessageHandler<D, E, R> implements F4MMessageHandler<E>, MessageDecoder<D, E>,
		MessageEncoder<D, E> {

	protected SessionWrapperFactory sessionWrapperFactory;
	protected Config config;
	protected LoggingUtil loggingUtil;

	private SessionWrapper sessionWrapper;

	@Override
	public void onMessage(E originalMessageEncoded) {
		D originalMessageDecoded = null;
		try {
			originalMessageDecoded = prepare();
			originalMessageDecoded = decode(originalMessageEncoded, originalMessageDecoded);
			logReceivedMessage(originalMessageDecoded, originalMessageEncoded);
			validate(originalMessageEncoded);
			validatePermissions(originalMessageDecoded);
			final R processResult = onProcess(originalMessageDecoded);
			if (processResult != null) {
				onResponse(originalMessageDecoded, processResult);
			}
		} catch (NullPointerException nex) {
			if (nex.getCause() != null) {
				onFailure(originalMessageEncoded, originalMessageDecoded, nex.getCause());
			} else {
				onFailure(originalMessageEncoded, originalMessageDecoded, nex);
			}
		} catch (Exception e) {
			onFailure(originalMessageEncoded, originalMessageDecoded, e);
		}
	}
	
	protected abstract void logReceivedMessage(D originalMessageDecoded, E originalMessageEncoded);

	public void sendAsyncMessage(D originalMessageDecoded) {
		try {
			E encodedMessage = encode(originalMessageDecoded);
			getSessionWrapper().sendAsynText(encodedMessage.toString());
		} catch (EncodeException e) {
			onEncodeFailure(originalMessageDecoded, e);
		} catch (F4MValidationException e) {
			onFailure(null, originalMessageDecoded, e);
		}
	}

	@Override
	public SessionWrapper getSessionWrapper() {
		return sessionWrapper;
	}

	@Override
	public void setSession(Session session) {
		this.sessionWrapper = sessionWrapperFactory.create(session);
	}

	@Override
	public void setSessionWrapper(SessionWrapper sessionWrapper) {
		this.sessionWrapper = sessionWrapper;
	}

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

	public abstract ClientInfo onAuthentication(D message) throws F4MAuthenticationException;

	public abstract R onProcess(D message) throws F4MException;

	public abstract void onEncodeFailure(D originalMessageDecoded, EncodeException e);

	/**
	 * Service Message handling failure processing by responding with error message
	 * 
	 * @param originalMessageEncoded
	 *            - Originally received message (might be null)
	 * @param originalMessageDecoded
	 *            - Decoded original message (might be null)
	 * @param e
	 *            - Exception occurred
	 */
	public abstract void onFailure(E originalMessageEncoded, D originalMessageDecoded, Throwable e);

	public abstract void onResponse(D originalMessageDecoded, R processResultContent);

	@Override
	public void destroy() {
		//empty destroy implementation for standard handlers with no resources to release
	}
}
