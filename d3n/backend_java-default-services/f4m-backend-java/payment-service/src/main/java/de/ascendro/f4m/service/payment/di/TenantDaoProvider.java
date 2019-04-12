package de.ascendro.f4m.service.payment.di;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;

import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantAerospikeDaoImpl;
import de.ascendro.f4m.service.payment.dao.TenantDao;
import de.ascendro.f4m.service.payment.dao.TenantFileDaoImpl;

public class TenantDaoProvider implements Provider<TenantDao> {
	private static final Logger LOGGER = LoggerFactory.getLogger(TenantDaoProvider.class);
	
	private PaymentConfig config;
	private TenantDao tenantDao;
	private TenantDaoFactory factory;

	@Inject
	public TenantDaoProvider(PaymentConfig config, TenantDaoFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public TenantDao get() {
		if (tenantDao == null) {
			String path = config.getProperty(PaymentConfig.TENANT_INFO_MOCK_PATH);
			if (StringUtils.isNotBlank(path)) {
				LOGGER.info("Reading tentant configuration from file {}", path);
				TenantFileDaoImpl mock = factory.createMock();
				mock.setPath(path);
				tenantDao = mock;
			} else {
				LOGGER.debug("Initiating tentant configuration DAO from Aerospike");
				tenantDao = factory.createImpl();
			}
		}
		return tenantDao;
	}

	public interface TenantDaoFactory {
		public TenantAerospikeDaoImpl createImpl();
		public TenantFileDaoImpl createMock();
	}
}
