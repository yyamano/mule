
package org.mule.context;

import org.mule.api.MuleRuntimeException;

/**
 *
 */
public class DefaultMuleDomainFactory
{

    public static final String DOMAIN_CONTEXT_BUILDER = "org.mule.config.spring.MuleApplicationDomainContextBuilder";
    private ApplicationDomainContextBuilder applicationDomainContextBuilder;

    public MuleApplicationDomain createMuleDomain(String domain, ClassLoader classLoader)
    {
        //TODO review how to get this instance without depending on spring module
        try
        {
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            try
            {
                if (applicationDomainContextBuilder == null)
                {
                    applicationDomainContextBuilder = (ApplicationDomainContextBuilder) Class.forName(DOMAIN_CONTEXT_BUILDER).newInstance();
                }
                applicationDomainContextBuilder.setDomain(domain);
                applicationDomainContextBuilder.setClassLoader(classLoader);
                return applicationDomainContextBuilder.build();
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    public void setApplicationDomainContextBuilder(ApplicationDomainContextBuilder applicationDomainContextBuilder)
    {
        this.applicationDomainContextBuilder = applicationDomainContextBuilder;
    }
}
