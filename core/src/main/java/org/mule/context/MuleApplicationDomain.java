package org.mule.context;

public class MuleApplicationDomain
{

    private String domain;
    private Object context;

    public MuleApplicationDomain(String domain, Object context)
    {
        this.domain = domain;
        this.context = context;
    }

    public String getDomain()
    {
        return domain;
    }

    public Object getContext()
    {
        return context;
    }
}
