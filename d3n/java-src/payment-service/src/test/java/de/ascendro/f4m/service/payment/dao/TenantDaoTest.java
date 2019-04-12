package de.ascendro.f4m.service.payment.dao;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.payment.model.external.ExchangeRate;

/**
 * Tests that both TenantDao implementations work in the same way.
 */
public abstract class TenantDaoTest {
	
	public abstract TenantDao getTenantDao();
	
	public abstract void beforeTestLoadingAndRead() throws Exception;
	
	@Test
	public void testLoadingAndRead() throws Exception {
		beforeTestLoadingAndRead();
		TenantInfo tenantInfo = getTenantDao().getTenantInfo("1");
		assertNotNull(tenantInfo);
		assertThat(tenantInfo.getApiId(), equalTo("username"));
		assertThat(tenantInfo.getMainCurrency(), equalTo("EUR"));
		assertThat(tenantInfo.getExchangeRates(), hasSize(2));
		ExchangeRate rate = tenantInfo.getExchangeRates().get(0);
		assertThat(rate.getFromCurrency(), equalTo("EUR"));
		assertThat(rate.getToCurrency(), equalTo("CREDIT"));
		assertThat(rate.getFromAmount(), comparesEqualTo(new BigDecimal("2")));
		assertThat(rate.getToAmount(), comparesEqualTo(new BigDecimal("10.00")));
	}
	
	@Test
	public void getTenantExchangeRatesFiltered() throws Exception {
		beforeTestLoadingAndRead();
		TenantInfo tenantInfo = getTenantDao().getTenantInfo("1");
		tenantInfo.getExchangeRates().forEach(rate -> {
			assertNotEquals(String.format("Wrong exchange rate not filtered, %s", rate.toString()),
					tenantInfo.getMainCurrency(), rate.getToCurrency());
		});
	}

	public abstract void beforeTestTenantNotFound() throws Exception;

	@Test
	public void testTenantNotFound() throws Exception {
		beforeTestTenantNotFound();
		try {
			getTenantDao().getTenantInfo("nonExistentTenantId");
			fail();
		} catch (F4MEntryNotFoundException e) {
			assertEquals("Tenant not found nonExistentTenantId", e.getMessage());
		}
	}
}
