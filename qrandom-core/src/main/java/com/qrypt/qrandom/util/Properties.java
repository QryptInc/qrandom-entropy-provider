package com.qrypt.qrandom.util;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Map;
import java.util.function.Function;

/**
 * adopted fom bc-java
 */
public class Properties {

    private static final ThreadLocal threadProperties = new ThreadLocal();

    public static <T> T getProperty(String property, T defaultValue, Function<String,T> converter) {
        String value = getPropertyValue(property);
        if (value == null) {
            if (defaultValue == null) {
                //need to throw proper exception when system property is not defined and no default value exist
                throw new IllegalArgumentException("Property "+property+" is not defined anywhere and no default value specified");
            }
            return defaultValue;
        }

        try {
            return converter.apply(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Return the String value of the property propertyName. Property valuation
     * starts with java.security, then thread local, then system properties.
     *
     * @param propertyName name of property.
     * @return value of property as a String, null if not defined.
     */
    private static String getPropertyValue(final String propertyName)
    {
        String val = (String)AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return Security.getProperty(propertyName);
            }
        });
        if (val != null)
        {
            return val;
        }

        Map localProps = (Map)threadProperties.get();
        if (localProps != null)
        {
            String p = (String)localProps.get(propertyName);
            if (p != null)
            {
                return p;
            }
        }

        return (String)AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return System.getProperty(propertyName);
            }
        });
    }
}
