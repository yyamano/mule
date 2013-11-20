package org.mule.module.launcher.domain;

import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.notification.ClusterNodeNotificationListener;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ManagementNotificationListener;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.notification.ClusterNodeNotification;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.CustomNotification;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.ManagementNotification;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.context.notification.ServerNotificationManager;

public class DomainMuleContextBuilder extends DefaultMuleContextBuilder
{
    private final String domainId;

    public DomainMuleContextBuilder(String domainId)
    {
        this.domainId = domainId;
    }

    @Override
    protected MuleConfiguration getMuleConfiguration()
    {
        DefaultMuleConfiguration defaultMuleConfiguration = new DefaultMuleConfiguration();
        defaultMuleConfiguration.setDomainId(domainId);
        defaultMuleConfiguration.setId(domainId);
        return defaultMuleConfiguration;
    }

    @Override
    protected ServerNotificationManager createNotificationManager()
    {
        ServerNotificationManager manager = new ServerNotificationManager();
        manager.addInterfaceToType(MuleContextNotificationListener.class,
                                   MuleContextNotification.class);
        manager.addInterfaceToType(SecurityNotificationListener.class,
                                   SecurityNotification.class);
        manager.addInterfaceToType(ManagementNotificationListener.class,
                                   ManagementNotification.class);
        manager.addInterfaceToType(CustomNotificationListener.class, CustomNotification.class);
        manager.addInterfaceToType(ConnectionNotificationListener.class,
                                   ConnectionNotification.class);
        manager.addInterfaceToType(ExceptionNotificationListener.class,
                                   ExceptionNotification.class);
        manager.addInterfaceToType(ClusterNodeNotificationListener.class, ClusterNodeNotification.class);
        return manager;
    }
}
