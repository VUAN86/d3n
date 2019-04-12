package de.ascendro.f4m.service.payment.cache;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.UpdateCall;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.exception.F4MPaymentSystemIOException;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceResponse;
import de.ascendro.f4m.service.payment.session.PaymentClientInfoImpl;
import io.github.benas.randombeans.api.EnhancedRandom;

public class AccountBalanceCacheTest {
	
	@Mock
	private UserAccountManager userAccountManager;
	@Mock
	private AerospikeDao aerospikeDao;
	@Mock
	private AdminEmailForwarder emailForwarder;
	@InjectMocks
	private AccountBalanceCache cache;
	
	private PaymentClientInfo paymentClientInfo;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		paymentClientInfo = new PaymentClientInfoImpl("tenantId", "profileId", "tenantId");
	}

	@Test
	public void verifyGetAccountBalanceSingleStoresBalanceOnSuccess() {
		GetAccountBalanceRequest request = EnhancedRandom.random(GetAccountBalanceRequest.class);
		GetAccountBalanceResponse balanceResponse = EnhancedRandom.random(GetAccountBalanceResponse.class);
		String amount = "123456789923450293845702983457028.98";
		balanceResponse.setAmount(new BigDecimal(amount));
		when(userAccountManager.getAccountBalance(paymentClientInfo, request)).thenReturn(balanceResponse);
		
		cache.getAccountBalance(paymentClientInfo, request);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<UpdateCall<String>> arg = ArgumentCaptor.forClass(UpdateCall.class);
		verify(aerospikeDao).createOrUpdateString(anyString(), anyString(), anyString(), arg.capture()); 
		assertThat(arg.getValue().update(null, null), equalTo(amount));
	}

	@Test
	public void verifyGetAccountBalanceSingleReturnsFromCacheOnConnectionFailure() {
		GetAccountBalanceRequest request = EnhancedRandom.random(GetAccountBalanceRequest.class);
		request.setCurrency(Currency.BONUS);
		when(aerospikeDao.readString(anyString(), anyString(), anyString())).thenReturn("12.34");
		F4MPaymentSystemIOException exception = new F4MPaymentSystemIOException("expected", null);
		doThrow(exception).when(userAccountManager).getAccountBalance(paymentClientInfo, request);
		when(emailForwarder.shouldForwardErrorToAdmin(any())).thenReturn(true);
		
		GetAccountBalanceResponse response = cache.getAccountBalance(paymentClientInfo, request);
		assertThat(response.getAmount(), comparesEqualTo(new BigDecimal("12.34")));
		assertThat(response.getCurrency(), equalTo(request.getCurrency()));
		verify(emailForwarder).forwardWarningToAdmin(
				eq("getAccountBalance for tenantId=tenantId, profileId=profileId, currency=BONUS"), same(exception));
	}

	@Test
	public void verifyGetAccountBalancesStoresBalanceOnSuccess() {
		GetUserAccountBalanceResponse balanceResponse = new GetUserAccountBalanceResponse();
		balanceResponse.setBalances(Arrays.asList(balance("12.34", Currency.BONUS), balance("1", Currency.CREDIT)));
		when(userAccountManager.getAccountBalances(paymentClientInfo)).thenReturn(balanceResponse);
		
		cache.getAccountBalances(paymentClientInfo);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<UpdateCall<String>> arg = ArgumentCaptor.forClass(UpdateCall.class);
		verify(aerospikeDao, times(2)).createOrUpdateString(anyString(), anyString(), anyString(), arg.capture());
		List<UpdateCall<String>> updateCalls = arg.getAllValues();
		assertThat(updateCalls.get(0).update(null, null), equalTo("12.34"));
		assertThat(updateCalls.get(1).update(null, null), equalTo("1"));
	}

	private GetAccountBalanceResponse balance(String value, Currency currency) {
		GetAccountBalanceResponse balance = new GetAccountBalanceResponse();
		balance.setAmount(new BigDecimal(value));
		balance.setCurrency(currency);
		return balance;
	}

	@Test
	public void verifyGetAccountBalancesReturnsFromCacheOnConnectionFailure() {
		when(aerospikeDao.readString(anyString(), eq("balance:tenantId:profileId:BONUS"), anyString())).thenReturn("12.34");
		when(aerospikeDao.readString(anyString(), eq("balance:tenantId:profileId:CREDIT"), anyString())).thenReturn("2");
		when(aerospikeDao.readString(anyString(), eq("balance:tenantId:profileId:MONEY"), anyString())).thenReturn("5");
		doThrow(new F4MPaymentSystemIOException("expected", null)).when(userAccountManager).getAccountBalances(paymentClientInfo);
		
		GetUserAccountBalanceResponse response = cache.getAccountBalances(paymentClientInfo);
		assertThat(response.getBalances(), hasSize(3));
		assertThat(getAmountByCurrency(response, Currency.BONUS), equalTo(new BigDecimal("12.34")));
		assertThat(getAmountByCurrency(response, Currency.CREDIT), equalTo(new BigDecimal("2")));
		assertThat(getAmountByCurrency(response, Currency.MONEY), equalTo(new BigDecimal("5")));
	}

	private BigDecimal getAmountByCurrency(GetUserAccountBalanceResponse response, Currency currency) {
		return response.getBalances().stream().filter(b -> currency.equals(b.getCurrency())).findFirst().get()
				.getAmount();
	}
}