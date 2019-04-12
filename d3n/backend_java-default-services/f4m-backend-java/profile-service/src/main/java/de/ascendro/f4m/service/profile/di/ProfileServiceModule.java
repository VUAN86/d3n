package de.ascendro.f4m.service.profile.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.util.ApplicationConfigrationPrimaryKeyUtil;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.profile.config.ProfileConfig;
import de.ascendro.f4m.service.profile.dao.EndConsumerInvoiceAerospikeDao;
import de.ascendro.f4m.service.profile.dao.EndConsumerInvoiceAerospikeDaoImpl;
import de.ascendro.f4m.service.profile.dao.ProfileAerospikeDao;
import de.ascendro.f4m.service.profile.dao.ProfileAerospikeDaoImpl;
import de.ascendro.f4m.service.profile.util.EndConsumerInvoicePrimaryKeyUtil;
import de.ascendro.f4m.service.profile.util.ProfileUtil;

public class ProfileServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ProfileConfig.class).in(Singleton.class);
		bind(Config.class).to(ProfileConfig.class);
		bind(F4MConfigImpl.class).to(ProfileConfig.class);

		bind(AerospikeClientProvider.class).in(Singleton.class);
		bind(ProfileAerospikeDao.class).to(ProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(EndConsumerInvoiceAerospikeDao.class).to(EndConsumerInvoiceAerospikeDaoImpl.class).in(Singleton.class);

		bind(ProfileUtil.class).in(Singleton.class);

		bind(ApplicationConfigurationAerospikeDao.class).to(ApplicationConfigurationAerospikeDaoImpl.class).in(
				Singleton.class);

		bind(PrimaryKeyUtil.class).to(ProfilePrimaryKeyUtil.class).in(Singleton.class);
		bind(ProfilePrimaryKeyUtil.class).in(Singleton.class);
		bind(ApplicationConfigrationPrimaryKeyUtil.class).in(Singleton.class);
		bind(EndConsumerInvoicePrimaryKeyUtil.class).in(Singleton.class);
	}

}
