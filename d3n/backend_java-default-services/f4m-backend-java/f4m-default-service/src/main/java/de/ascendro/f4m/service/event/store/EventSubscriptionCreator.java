package de.ascendro.f4m.service.event.store;

import javax.inject.Inject;

import com.google.inject.assistedinject.Assisted;

@FunctionalInterface
public interface EventSubscriptionCreator {
	@Inject
	EventSubscription createEventSubscription(@Assisted boolean virtual, @Assisted String consumerName, @Assisted String topic);
}
