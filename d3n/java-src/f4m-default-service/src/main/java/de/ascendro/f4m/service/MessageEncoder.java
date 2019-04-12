package de.ascendro.f4m.service;

import javax.websocket.EncodeException;

import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;

public interface MessageEncoder<DM, EM> {
	EM encode(DM message) throws EncodeException, F4MValidationException;

	void validatePermissions(DM message) throws F4MInsufficientRightsException, F4MValidationFailedException;
}
