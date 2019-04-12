package de.ascendro.f4m.service.payment.model;

/**
 * 
 */
public enum TransactionStatus {

	/**
	 * The call to create transaction has been made to Payment Service / Payment System.
	 */
	INITIATED, 
	/**
	 * Service has received successful response about creation of transaction from Payment System,
	 * but transaction is not yet approved. A callback from Payment System is necessary to complete transaction.
	 */
	PROCESSING,
	/**
	 * Service has received successful response about creation of transaction from Payment Service / Payment System.
	 */
	COMPLETED,
	/**
	 * Error has been received from Payment Service, that transaction has been created but not accepted.
	 * Can only be used for payment transactions with callbacks.
	 */
	ERROR;

}
