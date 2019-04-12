package de.ascendro.f4m.server.exception;

import com.aerospike.client.AerospikeException;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class F4MAerospikeException extends F4MFatalErrorException {

	private static final long serialVersionUID = -896413555144248947L;

	public F4MAerospikeException(AerospikeException e) {
		super(getFormattedMessage(e), e);
	}

	private static String getFormattedMessage(AerospikeException e) {
		F4MAerospikeResultCodes code = F4MAerospikeResultCodes.findByCode(e.getResultCode());
		return String.format("%s: %s", code == null ? e.getResultCode() : code.name(), e.getMessage());
	}

}
