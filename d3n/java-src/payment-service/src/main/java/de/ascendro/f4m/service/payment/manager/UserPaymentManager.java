package de.ascendro.f4m.service.payment.manager;

import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.external.GetExternalPaymentRequest;
import de.ascendro.f4m.service.payment.model.external.GetExternalPaymentResponse;
import de.ascendro.f4m.service.payment.model.external.GetIdentificationResponse;
import de.ascendro.f4m.service.payment.model.external.InitExternalPaymentRequest;
import de.ascendro.f4m.service.payment.model.external.InitExternalPaymentResponse;
import de.ascendro.f4m.service.payment.model.external.InitIdentificationRequest;
import de.ascendro.f4m.service.payment.model.external.InitIdentificationResponse;

public interface UserPaymentManager {
	InitIdentificationResponse initiateIdentification(PaymentClientInfo paymentClientInfo, InitIdentificationRequest request);
	
	GetIdentificationResponse getIdentification(PaymentClientInfo paymentClientInfo);
	
	InitExternalPaymentResponse initiateExternalPayment(PaymentClientInfo paymentClientInfo, InitExternalPaymentRequest request);
	
	GetExternalPaymentResponse getExternalPayment(PaymentClientInfo paymentClientInfo, GetExternalPaymentRequest request);
	
	void synchronizeIdentityInformation(String userId);
}
