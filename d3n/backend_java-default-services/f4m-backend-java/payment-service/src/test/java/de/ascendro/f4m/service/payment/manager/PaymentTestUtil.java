package de.ascendro.f4m.service.payment.manager;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.AccountRestBuilder;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;

public class PaymentTestUtil {
	
	public static final String EUR_ACCOUNT_ID = "eurAccountId";

	public static CurrencyRest prepareCurrencyRest(ExternalTestCurrency currency) {
		CurrencyRest currencyRest = new CurrencyRest();
		currencyRest.setId(getTestCurrencyId(currency));
		currencyRest.setShortName(currency.toString());
		return currencyRest;
	}

	public static String getTestCurrencyId(ExternalTestCurrency currency) {
		return String.valueOf(currency.ordinal());
	}

	public static AccountRest createAccount(ExternalTestCurrency currency, String id) {
		//This is a copy from PaymentManagerImplTest. Move this method to some kind of test util?
		AccountRestBuilder builder = createAccountBuilder(currency, id);
		return builder.build();
	}

	public static AccountRestBuilder createAccountBuilder(ExternalTestCurrency currency, String id) {
		String currencyId = getTestCurrencyId(ExternalTestCurrency.EUR);
		AccountRestBuilder builder = new AccountRestBuilder().currency(prepareCurrencyRest(currency)).id(id).currencyId(currencyId);
		return builder;
	}
	
	public static List<CurrencyRest> createCurrencyRestsForVirtualCurrencies() {
		return Arrays.asList(prepareCurrencyRest(ExternalTestCurrency.BONUS),
				prepareCurrencyRest(ExternalTestCurrency.CREDIT));
	}

	public static AccountRest prepareEurAccount() {
		return new AccountRestBuilder().id(EUR_ACCOUNT_ID).currency(prepareCurrencyRest(ExternalTestCurrency.EUR)).build();
	}

	public static List<AccountRest> prepareAccounts(String eurAccountId, String bonusAccountId) {
		AccountRest eurAccount = new AccountRestBuilder().id(eurAccountId)
				.currency(prepareCurrencyRest(ExternalTestCurrency.EUR)).build();
		AccountRest bonusccount = new AccountRestBuilder().id(bonusAccountId)
				.currency(prepareCurrencyRest(ExternalTestCurrency.BONUS)).build();
		List<AccountRest> accounts = Arrays.asList(eurAccount, bonusccount);
		return accounts;
	}

	public static <T, V> List<V> mapToList(List<T> list, Function<T, V> mapperFunction) {
		return list.stream().map(mapperFunction).collect(Collectors.toList());
	}
}