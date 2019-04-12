package de.ascendro.f4m.service.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class F4MEnumUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(F4MEnumUtils.class);

	private F4MEnumUtils() {
	}

	public static <E extends Enum<E>> E getEnum(Class<E> enumClass, String value) {
		if (StringUtils.isNotBlank(value)) {
			try {
				return Enum.valueOf(enumClass, value);
			} catch (IllegalArgumentException e) {
				LOGGER.error("Invalid {} value [{}]", enumClass.getSimpleName(), value);
				return null;
			}
		} else {
			return null;
		}
	}

}
