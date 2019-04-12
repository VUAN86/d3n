package de.ascendro.f4m.service.payment.server;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentificationSuccessCallback extends HttpServlet {

	private final IdentificationPool identificationPool;

	private static final long serialVersionUID = -2983407233949399055L;
	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationSuccessCallback.class);
	public static final String USER_ID = "userid";

	@Inject
	public IdentificationSuccessCallback(IdentificationPool identificationPool) {
		this.identificationPool = identificationPool;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String userId = req.getParameter(USER_ID);
		//identification success should be registered in event log?
		if (StringUtils.isNotBlank(userId)) {
			LOGGER.info("Success callback received from payment system for user '{}'", userId);
			try {
				identificationPool.add(userId);
			} catch (Exception e) {
				LOGGER.error("Error processing success identification callback", e);
			}
		} else {
			LOGGER.error("Success callback received from payment system for incorrect userId '{}'", userId);
		}
	}
}
