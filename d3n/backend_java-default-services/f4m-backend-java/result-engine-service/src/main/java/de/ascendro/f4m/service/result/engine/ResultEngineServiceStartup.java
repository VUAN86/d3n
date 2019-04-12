package de.ascendro.f4m.service.result.engine;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.result.engine.di.ResultEngineServiceModule;
import de.ascendro.f4m.service.result.engine.di.ResultEngineWebSocketModule;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;

/**
 * Result Engine Service startup class
 */
public class ResultEngineServiceStartup extends ServiceStartup {
	public ResultEngineServiceStartup(Stage stage) {
		super(stage);
	}
	
	public static void main(String... args) throws Exception {
		new ResultEngineServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new ResultEngineServiceModule(), new ResultEngineWebSocketModule());
	}

	@Override
	protected String getServiceName() {
		return ResultEngineMessageTypes.SERVICE_NAME;
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	protected List<String> getDependentServiceNames(){
		return Arrays.asList(ProfileMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME, 
				VoucherMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME);
	}

}
