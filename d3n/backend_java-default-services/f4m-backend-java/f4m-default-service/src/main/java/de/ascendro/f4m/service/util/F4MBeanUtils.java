package de.ascendro.f4m.service.util;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

/**
 * Wrapper for {@link org.apache.commons.beanutils.BeanUtils}
 */
public final class F4MBeanUtils {
	private F4MBeanUtils() {}
	
	public static void copyProperties(Object dest, Object orig) {
		try {
			BeanUtils.copyProperties(dest, orig);
		} catch (IllegalAccessException e ) {
			throw new F4MFatalErrorException("Incorrect class definition for copying properties", e);
		} catch (InvocationTargetException e) {
			throw new F4MFatalErrorException("Incorrect data for copying properties", e);
		}
	}
}
