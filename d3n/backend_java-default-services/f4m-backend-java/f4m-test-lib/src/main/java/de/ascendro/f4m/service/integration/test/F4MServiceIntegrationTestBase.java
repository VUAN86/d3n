package de.ascendro.f4m.service.integration.test;

import org.junit.Rule;

import de.ascendro.f4m.service.integration.rule.JettyServerRule;

/**
 * Base test to be extended by all integration tests, which start a real web-server and tries to communicate with
 * service to be tested.
 */
public abstract class F4MServiceIntegrationTestBase extends F4MIntegrationTestBase {

	@Rule
	public JettyServerRule service = jettyServerRule;

}
