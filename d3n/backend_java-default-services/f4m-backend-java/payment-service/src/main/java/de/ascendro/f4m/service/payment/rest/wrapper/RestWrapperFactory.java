package de.ascendro.f4m.service.payment.rest.wrapper;

import com.google.inject.assistedinject.Assisted;

@FunctionalInterface
public interface RestWrapperFactory<T extends RestWrapper> {
	T create(@Assisted String tenantId);
}
