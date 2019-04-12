package de.ascendro.f4m.service.winning.manager;

import org.junit.Ignore;
import org.junit.Test;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

import static org.junit.Assert.*;

@Ignore
public class InsuranceInterfaceWrapperTest {

	InsuranceInterfaceWrapper insuranceApi = new InsuranceInterfaceWrapper();
	
	@Test
	public void testSuccess() {
		assertFalse(insuranceApi.callInsuranceApi("http://rtrt.emirat.de/APIS/unsicker/unsicker.ashx?k=E06ECEE5-B9AC-48D7-A69A-CC8E93673D3A&v=100001&i=usr1&g=10000"));
	}
	
	@Test(expected = F4MFatalErrorException.class)
	public void testInvalidParams() {
		insuranceApi.callInsuranceApi("http://rtrt.emirat.de/APIS/unsicker/unsicker.ashx?k=E06ECEE5-B9AC-48D7-A69A-CC8E93673D3A&v=100001&i=usr1");
	}
	
	@Test(expected = F4MFatalErrorException.class)
	public void testInvalidUri() {
		insuranceApi.callInsuranceApi("http://rtrt.eat.deeeee/APIS/unsicker/unsicker.ashx?k=E06ECEE5-B9AC-48D7-A69A-CC8E93673D3A&v=100001&i=usr1");
	}
	
}
