package org.mule.config.spring;

import org.mule.api.config.ConfigurationException;

/**
 * Spring configuration builder used to create domains.
 */
public class SpringXmlDomainConfigurationBuilder extends SpringXmlConfigurationBuilder
{

    public SpringXmlDomainConfigurationBuilder(String configResources) throws ConfigurationException
    {
        super(configResources);
        setUseMinimalConfigResource(true);
    }
}
