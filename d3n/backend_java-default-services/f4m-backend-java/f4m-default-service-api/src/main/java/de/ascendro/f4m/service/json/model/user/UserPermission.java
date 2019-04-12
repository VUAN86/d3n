package de.ascendro.f4m.service.json.model.user;

/**
 * Enum of known user roles' permissions. 
 * Caution: not all permissions need/can be defined in this enum. 
 * Only those for programmatic permissions checks. 
 */
public enum UserPermission {
	/**
	 * User can play games for bonuspoints
	 */
	GAME,
	/**
	 * User can play games for credits
	 */
	GAME_CREDIT,
	/**
	 * User can play games for money
	 */
	GAME_MONEY,
	/**
	 * User can play O18 games (e.g. gambling)
	 */
	GAME_O18,
	/**
	 * User can access data out of workorders
	 */
	INTERNAL_DATA_READ,
	/**
	 * User can manage data out of workorders
	 */
	INTERNAL_DATA_WRITE,
	/**
	 * User can access data within the workorders
	 */
	WORKORDER_DATA_READ,
	/**
	 * User can manage data within the workorders
	 */
	WORKORDER_DATA_WRITE,
	/**
	 * User can access all tenant data
	 */
	TENANT_DATA_READ,
	/**
	 * User can manage all tenant data
	 */
	TENANT_DATA_WRITE,
	/**
	 * Only for intern service usage
	 */
	SERVICE_INTERN
}
