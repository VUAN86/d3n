package de.ascendro.f4m.service.event.activemq;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.apache.activemq.transport.TransportDisposedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMSExceptionListener implements ExceptionListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(JMSExceptionListener.class);

	@Override
	public void onException(JMSException jmsEx) {
		if (jmsEx.getCause() instanceof TransportDisposedIOException) {
			LOGGER.debug("JMS Exception occured", jmsEx);
		} else {
			LOGGER.error("JMS Exception occured", jmsEx);
		}
	}

}
