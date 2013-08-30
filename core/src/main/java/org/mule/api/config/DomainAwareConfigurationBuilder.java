package org.mule.api.config;

public interface DomainAwareConfigurationBuilder extends ConfigurationBuilder
{

    /**
     * TODO check how to avoid depending on Object
     * @param domainContext
     */
    public void setDomainContext(Object domainContext);

}
