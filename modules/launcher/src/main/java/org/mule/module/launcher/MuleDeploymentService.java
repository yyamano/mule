/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mule.util.SplashScreen.miniSplash;

import org.mule.config.StartupContext;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationClassLoaderFactory;
import org.mule.module.launcher.application.ApplicationFactory;
import org.mule.module.launcher.application.CompositeApplicationClassLoaderFactory;
import org.mule.module.launcher.application.DefaultApplicationFactory;
import org.mule.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.module.launcher.domain.ApplicationDomainClassLoaderFactory;
import org.mule.module.launcher.domain.ApplicationDomainFactory;
import org.mule.module.launcher.domain.MuleApplicationDomainClassLoaderFactory;
import org.mule.module.launcher.domain.MuleApplicationDomainFactory;
import org.mule.module.launcher.util.DebuggableReentrantLock;
import org.mule.module.launcher.util.ElementAddedEvent;
import org.mule.module.launcher.util.ElementRemovedEvent;
import org.mule.module.launcher.util.ObservableList;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.ArrayUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.StringUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleDeploymentService implements DeploymentService
{
    public static final String APP_ANCHOR_SUFFIX = "-anchor.txt";
    public static final String ZIP_FILE_SUFFIX = ".zip";
    public static final IOFileFilter ZIP_APPS_FILTER = new AndFileFilter(new SuffixFileFilter(ZIP_FILE_SUFFIX), FileFileFilter.FILE);

    protected static final int DEFAULT_CHANGES_CHECK_INTERVAL_MS = 5000;

    private final ApplicationDomainFactory applicationDomainFactory;

    public static final String CHANGE_CHECK_INTERVAL_PROPERTY = "mule.launcher.changeCheckInterval";

    protected ScheduledExecutorService appDirMonitorTimer;

    protected transient final Log logger = LogFactory.getLog(getClass());
    // fair lock
    private ReentrantLock deploymentInProgressLock = new DebuggableReentrantLock(true);

    private ObservableList<Application> applications = new ObservableList<Application>();
    private final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();
    private final File domainDir = MuleContainerBootstrapUtils.getMuleDomainsDir();

    private List<StartupListener> startupListeners = new ArrayList<StartupListener>();

    private CompositeDeploymentListener deploymentListener = new CompositeDeploymentListener();
    private ArtifactDeployer artifactDeployer;

    public MuleDeploymentService(PluginClassLoaderManager pluginClassLoaderManager)
    {
        ApplicationDomainClassLoaderFactory applicationDomainClassLoaderFactory = new MuleApplicationDomainClassLoaderFactory();

        ApplicationClassLoaderFactory applicationClassLoaderFactory = new MuleApplicationClassLoaderFactory(applicationDomainClassLoaderFactory);
        applicationClassLoaderFactory = new CompositeApplicationClassLoaderFactory(applicationClassLoaderFactory, pluginClassLoaderManager);

        applicationDomainFactory = new MuleApplicationDomainFactory();

        DefaultApplicationFactory appFactory = new DefaultApplicationFactory(applicationClassLoaderFactory, applicationDomainFactory);
        appFactory.setDeploymentListener(deploymentListener);

        DefaultMuleDeployer deployer = new DefaultMuleDeployer();
        deployer.setApplicationFactory(appFactory);
        artifactDeployer = new ArtifactDeployer(deploymentListener, deployer, appFactory, applications, deploymentInProgressLock);
    }

    @Override
    public void start()
    {
        DeploymentStatusTracker deploymentStatusTracker = new DeploymentStatusTracker();
        addDeploymentListener(deploymentStatusTracker);

        StartupSummaryDeploymentListener summaryDeploymentListener = new StartupSummaryDeploymentListener(deploymentStatusTracker);
        addStartupListener(summaryDeploymentListener);

        deleteAllAnchors();

        // mule -app app1:app2:app3 will restrict deployment only to those specified apps
        final Map<String, Object> options = StartupContext.get().getStartupOptions();
        String appString = (String) options.get("app");

        String[] explodedDomains = domainDir.list(DirectoryFileFilter.DIRECTORY);
        String[] packagedDomains = domainDir.list(ZIP_APPS_FILTER);

        deployPackedDomains(packagedDomains);
        deployExplodedDomains(explodedDomains);

        //Collection<MuleApplicationDomain> allDomains = applicationDomainFactory.createAllDomains();
        //
        //for (MuleApplicationDomain domain : allDomains)
        //{
        //    try
        //    {
        //        domain.start();
        //    }
        //    catch (MuleException e)
        //    {
        //        logger.warn(String.format("Failure deploying domain %s", domain.getName()),e);
        //    }
        //}

        if (appString == null)
        {
            String[] explodedApps = appsDir.list(DirectoryFileFilter.DIRECTORY);
            String[] packagedApps = appsDir.list(ZIP_APPS_FILTER);

            deployPackedApps(packagedApps);
            deployExplodedApps(explodedApps);
        }
        else
        {
            String[] apps = appString.split(":");
            apps = removeDuplicateAppNames(apps);

            for (String app : apps)
            {
                try
                {
                    File applicationFile = new File(appsDir, app + ZIP_FILE_SUFFIX);

                    if (applicationFile.exists() && applicationFile.isFile())
                    {
                        artifactDeployer.deployPackagedArtifact(app + ZIP_FILE_SUFFIX);
                    }
                    else
                    {
                        artifactDeployer.deployExplodedArtifact(app);
                    }
                }
                catch (Exception e)
                {
                    // Ignore and continue
                }
            }
        }

        for (StartupListener listener : startupListeners)
        {
            try
            {
                listener.onAfterStartup();
            }
            catch (Throwable t)
            {
                logger.error(t);
            }
        }

        // only start the monitor thread if we launched in default mode without explicitly
        // stated applications to launch
        if (!(appString != null))
        {
            scheduleChangeMonitor(appsDir);
        }
        else
        {
            if (logger.isInfoEnabled())
            {
                logger.info(miniSplash("Mule is up and running in a fixed app set mode"));
            }
        }
    }

    private void deployExplodedDomains(String[] explodedDomains)
    {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void deployPackedDomains(String[] packagedDomains)
    {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void deleteAllAnchors()
    {
        deleteAnchorsFromDirectory(domainDir);
        deleteAnchorsFromDirectory(appsDir);
    }

    private void deleteAnchorsFromDirectory(final File directory)
    {
        // Deletes any leftover anchor files from previous shutdowns
        String[] anchors = directory.list(new SuffixFileFilter(APP_ANCHOR_SUFFIX));
        for (String anchor : anchors)
        {
            // ignore result
            new File(directory, anchor).delete();
        }
    }

    private String[] removeDuplicateAppNames(String[] apps)
    {
        List<String> appNames = new LinkedList<String>();

        for (String appName : apps)
        {
            if (!appNames.contains(appName))
            {
                appNames.add(appName);
            }
        }

        return appNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    protected void scheduleChangeMonitor(File appsDir)
    {
        final int reloadIntervalMs = getChangesCheckIntervalMs();
        appDirMonitorTimer = Executors.newSingleThreadScheduledExecutor(new AppDeployerMonitorThreadFactory());

        appDirMonitorTimer.scheduleWithFixedDelay(new AppDirWatcher(appsDir),
                                                  0,
                                                  reloadIntervalMs,
                                                  TimeUnit.MILLISECONDS);

        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Mule is up and kicking (every %dms)", reloadIntervalMs)));
        }
    }

    public static int getChangesCheckIntervalMs()
    {
        try
        {
            String value = System.getProperty(CHANGE_CHECK_INTERVAL_PROPERTY);
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return DEFAULT_CHANGES_CHECK_INTERVAL_MS;
        }
    }

    @Override
    public void stop()
    {
        stopAppDirMonitorTimer();

        deploymentInProgressLock.lock();
        try
        {
            // tear down apps in reverse order
            Collections.reverse(applications);

            for (Application application : applications)
            {
                try
                {
                    application.stop();
                    application.dispose();
                }
                catch (Throwable t)
                {
                    logger.error(t);
                }
            }
        }
        finally
        {
            deploymentInProgressLock.unlock();
        }
    }

    private void stopAppDirMonitorTimer()
    {
        if (appDirMonitorTimer != null)
        {
            appDirMonitorTimer.shutdown();
            try
            {
                appDirMonitorTimer.awaitTermination(getChangesCheckIntervalMs(), TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Application findApplication(String appName)
    {
        return (Application) CollectionUtils.find(applications, new BeanPropertyValueEqualsPredicate("appName", appName));
    }

    @Override
    public List<Application> getApplications()
    {
        return Collections.unmodifiableList(applications);
    }

    /**
     * @return URL/lastModified of apps which previously failed to deploy
     */
    public Map<URL, Long> getApplicationsZombieMap()
    {
        return artifactDeployer.getApplicationsZombieMap();
    }

    protected MuleDeployer getDeployer()
    {
        return artifactDeployer.getDeployer();
    }

    public void setAppFactory(ApplicationFactory appFactory)
    {
        this.artifactDeployer.setAppFactory(appFactory);
    }

    public void setDeployer(MuleDeployer deployer)
    {
        this.artifactDeployer.setDeployer(deployer);
    }

    public ApplicationFactory getAppFactory()
    {
        return artifactDeployer.getAppFactory();
    }

    @Override
    public ReentrantLock getLock() {
        return deploymentInProgressLock;
    }

    protected void undeploy(Application app)
    {
        artifactDeployer.undeploy(app);
    }

    @Override
    public void undeploy(String appName)
    {
        artifactDeployer.undeploy(appName);
    }

    @Override
    public void deploy(URL appArchiveUrl) throws IOException
    {
        artifactDeployer.deploy(appArchiveUrl);
    }

    @Override
    public void addStartupListener(StartupListener listener)
    {
        this.startupListeners.add(listener);
    }

    @Override
    public void removeStartupListener(StartupListener listener)
    {
        this.startupListeners.remove(listener);
    }

    @Override
    public void addDeploymentListener(DeploymentListener listener)
    {
        deploymentListener.addDeploymentListener(listener);
    }

    @Override
    public void removeDeploymentListener(DeploymentListener listener)
    {
        deploymentListener.removeDeploymentListener(listener);
    }

    private void deployPackedApps(String[] zips)
    {
        for (String zip : zips)
        {
            try
            {
                artifactDeployer.deployPackagedArtifact(zip);
            }
            catch (Exception e)
            {
                // Ignore and continue
            }
        }
    }

    private void deployExplodedApps(String[] apps)
    {

        for (String addedApp : apps)
        {
            try
            {
                artifactDeployer.deployExplodedArtifact(addedApp);
            }
            catch (DeploymentException e)
            {
                // Ignore and continue
            }
        }
    }

    /**
     * Returns the list of anchor file names for the deployed apps
     *
     * @return a non null list of file names
     */
    private String[] findExpectedAnchorFiles()
    {
        String[] appAnchors = new String[applications.size()];
        int i = 0;

        for (Application application : applications)
        {
            appAnchors[i++] = application.getAppName() + APP_ANCHOR_SUFFIX;
        }

        return appAnchors;
    }

    /**
     * Not thread safe. Correctness is guaranteed by a single-threaded executor.
     */
    protected class AppDirWatcher implements Runnable
    {
        protected File appsDir;

        protected volatile boolean dirty;

        public AppDirWatcher(final File appsDir)
        {
            this.appsDir = appsDir;
            applications.addPropertyChangeListener(new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent e)
                {
                    if (e instanceof ElementAddedEvent || e instanceof ElementRemovedEvent)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Deployed applications set has been modified, flushing state.");
                        }
                        dirty = true;
                    }
                }
            });
        }

        // Cycle is:
        //   undeploy removed apps
        //   deploy archives
        //   deploy exploded
        public void run()
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Checking for changes...");
                }
                // use non-barging lock to preserve fairness, according to javadocs
                // if there's a lock present - wait for next poll to do anything
                if (!deploymentInProgressLock.tryLock(0, TimeUnit.SECONDS))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Another deployment operation in progress, will skip this cycle. Owner thread: " +
                                     ((DebuggableReentrantLock) deploymentInProgressLock).getOwner());
                    }
                    return;
                }

                undeployRemovedApps();

                // list new apps
                String[] apps = appsDir.list(DirectoryFileFilter.DIRECTORY);

                final String[] zips = appsDir.list(ZIP_APPS_FILTER);

                deployPackedApps(zips);

                // re-scan exploded apps and update our state, as deploying Mule app archives might have added some
                if (zips.length > 0 || dirty)
                {
                    apps = appsDir.list(DirectoryFileFilter.DIRECTORY);
                }

                deployExplodedApps(apps);
            }
            catch (InterruptedException e)
            {
                // preserve the flag for the thread
                Thread.currentThread().interrupt();
            }
            finally
            {
                if (deploymentInProgressLock.isHeldByCurrentThread())
                {
                    deploymentInProgressLock.unlock();
                }
                dirty = false;
            }
        }

        private void undeployRemovedApps()
        {
            // we care only about removed anchors
            String[] currentAnchors = appsDir.list(new SuffixFileFilter(APP_ANCHOR_SUFFIX));
            if (logger.isDebugEnabled())
            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Current anchors:%n"));
                for (String currentAnchor : currentAnchors)
                {
                    sb.append(String.format("  %s%n", currentAnchor));
                }
                logger.debug(sb.toString());
            }

            String[] appAnchors = findExpectedAnchorFiles();
            @SuppressWarnings("unchecked")
            final Collection<String> deletedAnchors = CollectionUtils.subtract(Arrays.asList(appAnchors), Arrays.asList(currentAnchors));
            if (logger.isDebugEnabled())
            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Deleted anchors:%n"));
                for (String deletedAnchor : deletedAnchors)
                {
                    sb.append(String.format("  %s%n", deletedAnchor));
                }
                logger.debug(sb.toString());
            }

            for (String deletedAnchor : deletedAnchors)
            {
                String appName = StringUtils.removeEnd(deletedAnchor, APP_ANCHOR_SUFFIX);
                try
                {
                    if (findApplication(appName) != null)
                    {
                        undeploy(appName);
                    }
                    else if (logger.isDebugEnabled())
                    {
                        logger.debug(String.format("Application [%s] has already been undeployed via API", appName));
                    }
                }
                catch (Throwable t)
                {
                    logger.error("Failed to undeploy application: " + appName, t);
                }
            }
        }
    }

}
