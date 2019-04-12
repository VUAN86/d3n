package de.ascendro.f4m.service;

import de.ascendro.f4m.service.exception.validation.F4MValidationException;

public interface MessageDecoder<D, E> {
	D prepare();
	D decode(E message, D context) throws F4MValidationException;
	void validate(E message) throws F4MValidationException;
}
