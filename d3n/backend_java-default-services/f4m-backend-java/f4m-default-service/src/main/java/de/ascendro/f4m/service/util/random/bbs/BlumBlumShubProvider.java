package de.ascendro.f4m.service.util.random.bbs;

import java.security.Provider;

public class BlumBlumShubProvider extends Provider {

	private static final long serialVersionUID = -7126465959529488545L;

	private static final String NAME = "BlumBlumShub";
	private static final double VERSION = 1.0;
	private static final String INFO = "BlumBlumShub secure random implementation";
	
	public BlumBlumShubProvider() {
		super(NAME, VERSION, INFO);
	}
	
}
