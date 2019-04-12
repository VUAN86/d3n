package de.ascendro.f4m.service.registry.model.monitor;

public enum MonitoringConnectionStatus {

	/**
	 * Status OK
	 */
	OK,
	/**
	* Status not OK
	*/
	NOK,
	/**
	* Not applicable
	*/
	NA,
	/**
	* Connection not created (yet) due to lazy loading
	*/
	NC
}
