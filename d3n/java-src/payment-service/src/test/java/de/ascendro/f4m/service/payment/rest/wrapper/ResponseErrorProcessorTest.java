package de.ascendro.f4m.service.payment.rest.wrapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.rest.model.ErrorInfoRest;

public class ResponseErrorProcessorTest {
	@Mock
	private Response response;
	@Mock
	private PaymentConfig config;
	@InjectMocks
	private IdentificationRestWrapper identificationRestWrapper;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testErrorVerificationWithoutErrorInfo() {
		when(response.getStatusInfo()).thenReturn(Response.Status.BAD_REQUEST);
		try {
			ResponseErrorProcessor.verifyError("GET", "entity", response);
		} catch (F4MPaymentException e) {
			assertEquals("Unknown error with status 400", e.getMessage());
		}
	}
	
	@Test
	public void testErrorVerificationWithUnrecognizedCode() {
		when(response.getStatusInfo()).thenReturn(Response.Status.BAD_REQUEST);
		ErrorInfoRest errorInfo = new ErrorInfoRest();
		errorInfo.setCode("-17a");
		errorInfo.setMessage("Msg");
		when(response.readEntity(ErrorInfoRest.class)).thenReturn(errorInfo);
		try {
			ResponseErrorProcessor.verifyError("GET", "entity", response);
		} catch (F4MPaymentException e) {
			assertEquals("Msg - null", e.getMessage());
		}
	}	
}
