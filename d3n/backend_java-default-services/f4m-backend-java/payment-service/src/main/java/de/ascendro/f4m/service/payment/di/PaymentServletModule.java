package de.ascendro.f4m.service.payment.di;

import javax.inject.Singleton;

import com.google.inject.servlet.ServletModule;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.server.IdentificationErrorCallback;
import de.ascendro.f4m.service.payment.server.IdentificationPool;
import de.ascendro.f4m.service.payment.server.IdentificationSuccessCallback;
import de.ascendro.f4m.service.payment.server.PaymentErrorCallback;
import de.ascendro.f4m.service.payment.server.PaymentSuccessCallback;

public class PaymentServletModule extends ServletModule {

	@Override
	protected void configureServlets() {
		bind(IdentificationSuccessCallback.class).in(Singleton.class);
		bind(IdentificationErrorCallback.class).in(Singleton.class);
		bind(PaymentSuccessCallback.class).in(Singleton.class);
		bind(PaymentErrorCallback.class).in(Singleton.class);
		bind(IdentificationPool.class).in(Singleton.class);
		
		serve(PaymentConfig.IDENTIFICATION_SUCCESS_CONTEXT_PATH).with(IdentificationSuccessCallback.class);
		serve(PaymentConfig.IDENTIFICATION_ERROR_CONTEXT_PATH).with(IdentificationErrorCallback.class);
		serve(PaymentConfig.PAYMENT_SUCCESS_CONTEXT_PATH).with(PaymentSuccessCallback.class);
		serve(PaymentConfig.PAYMENT_ERROR_CONTEXT_PATH).with(PaymentErrorCallback.class);
	}
}
