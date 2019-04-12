module.exports = {
    TENANT_1: 	{
		"tenantId": "1",
		"apiId": "02B3E684-78A8-42F3-8EC8-8FE245FC1178",
		"mainCurrency" : "EUR",
		"exchangeRates": [
			{
				"fromCurrency" : "EUR",
				"toCurrency" : "CREDIT",
				"fromAmount" : "5",
				"toAmount" : "10.00"
			},
			{
				"fromCurrency" : "EUR",
				"toCurrency" : "CREDIT",
				"fromAmount" : "10",
				"toAmount" : "30.00"
			},
			{
				"fromCurrency" : "EUR",
				"toCurrency" : "CREDIT",
				"fromAmount" : "20",
				"toAmount" : "100.00"
			},
			{
				"fromCurrency" : "EUR",
				"toCurrency" : "BONUS",
				"fromAmount" : "5.00",
				"toAmount" : "100"
			}
		]
	},
	TENANT_2: {
		"tenantId": "2",
		"apiId": "02B3E684-78A8-42F3-8EC8-8FE245FC1178",
		"mainCurrency" : "EUR",
		"exchangeRates": [
			{
				"fromCurrency" : "EUR",
				"toCurrency" : "CREDIT",
				"fromAmount" : "2",
				"toAmount" : "10.00"
			},
			{
				"fromCurrency" : "BONUS",
				"toCurrency" : "CREDIT",
				"fromAmount" : "100",
				"toAmount" : "1"
			}
		]
	}
}
