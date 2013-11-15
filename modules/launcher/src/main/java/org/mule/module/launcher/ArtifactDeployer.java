package org.mule.module.launcher;

import static org.mule.util.SplashScreen.miniSplash;

import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.artifact.Artifact;
import org.mule.module.launcher.artifact.ArtifactFactory;
import org.mule.module.launcher.util.ObservableList;
import org.mule.util.CollectionUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class ArtifactDeployer<T extends Artifact>
{

    public static final String ARTIFACT_NAME_PROPERTY = "artifactName";
    protected transient final Log logger = LogFactory.getLog(getClass());
    public static final String ZIP_FILE_SUFFIX = ".zip";
    public static final String ANOTHER_DEPLOYMENT_OPERATION_IS_IN_PROGRESS = "Another deployment operation is in progress";
    public static final String INSTALL_OPERATION_HAS_BEEN_INTERRUPTED = "Install operation has been interrupted";

    private Map<String, ZombieFile> applicationZombieMap = new HashMap<String, ZombieFile>();
    private File artifactDir;
    private ObservableList<T> artifacts;
    private CompositeDeploymentListener deploymentListener;
    private ReentrantLock deploymentInProgressLcok;
    protected MuleDeployer<T> deployer;
    protected ArtifactFactory<T> artifactFactory;

    public ArtifactDeployer(CompositeDeploymentListener deploymentListener, MuleDeployer deployer, ArtifactFactory artifactFactory, ObservableList<T> artifacts, ReentrantLock lock)
    {
        this.deploymentListener = deploymentListener;
        this.deployer = deployer;
        this.artifactFactory = artifactFactory;
        this.artifacts = artifacts;
        this.deploymentInProgressLcok = lock;
        this.artifactDir = artifactFactory.getArtifactDir();
    }


    public void deployPackagedArtifact(String zip) throws Exception
    {
        URL url;
        File appZip;

        final String appName = StringUtils.removeEnd(zip, ZIP_FILE_SUFFIX);

        appZip = new File(artifactDir, zip);
        url = appZip.toURI().toURL();

        ZombieFile zombieFile = applicationZombieMap.get(appName);
        if (zombieFile != null)
        {
            if (zombieFile.isFor(url) && !zombieFile.updatedZombieApp())
            {
                // Skips the file because it was already deployed with failure
                return;
            }
        }

        // check if this app is running first, undeploy it then
        T app = (T) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, appName));
        if (app != null)
        {
            undeploy(appName);
        }

        deploy(url);
    }

    public void deployExplodedArtifact(String artifactName) throws DeploymentException
    {
        @SuppressWarnings("rawtypes")
        Collection<String> deployedAppNames = CollectionUtils.collect(artifacts, new BeanToPropertyValueTransformer(ARTIFACT_NAME_PROPERTY));

        if (deployedAppNames.contains(artifactName) && (!applicationZombieMap.containsKey(artifactName)))
        {
            return;
        }

        ZombieFile zombieFile = applicationZombieMap.get(artifactName);

        if ((zombieFile != null) && (!zombieFile.updatedZombieApp()))
        {
            return;
        }

        deployExplodedApp(artifactName);
    }

    public Map<String, ZombieFile> getApplicationZombieMap()
    {
        return applicationZombieMap;
    }

    private void deployExplodedApp(String addedApp) throws DeploymentException
    {
        if (logger.isInfoEnabled())
        {
            logger.info("================== New Exploded Application: " + addedApp);
        }

        T artifact;
        try
        {
            artifact = artifactFactory.createArtifact(addedApp);

            // add to the list of known apps first to avoid deployment loop on failure
            onArtifactInstalled(artifact);
        }
        catch (Throwable t)
        {
            final File artifactDir1 = artifactDir;
            File appDir1 = new File(artifactDir1, addedApp);

            addZombieFile(addedApp, appDir1);

            String msg = miniSplash(String.format("Failed to deploy exploded application: '%s', see below", addedApp));
            logger.error(msg, t);

            deploymentListener.onDeploymentFailure(addedApp, t);

            if (t instanceof DeploymentException)
            {
                throw (DeploymentException) t;
            }
            else
            {
                msg = "Failed to deploy application: " + addedApp;
                throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
            }
        }

        deployArtifact(artifact);
    }

    protected void onArtifactInstalled(T a)
    {
        trackArtifact(a);
    }

    public void undeploy(String appName)
    {
        if (applicationZombieMap.containsKey(appName))
        {
            return;
        }
        T app = (T) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, appName));
        undeploy(app);
    }

    public void deploy(URL appArchiveUrl) throws IOException
    {
        T artifact;

        try
        {
            try
            {
                artifact = guardedInstallFrom(appArchiveUrl);
                trackArtifact(artifact);
            }
            catch (Throwable t)
            {
                File appArchive = new File(appArchiveUrl.toURI());
                String appName = StringUtils.removeEnd(appArchive.getName(), ZIP_FILE_SUFFIX);

                //// error text has been created by the deployer already
                final String msg = miniSplash(String.format("Failed to deploy app '%s', see below", appName));
                logger.error(msg, t);

                addZombieFile(appName, appArchive);

                deploymentListener.onDeploymentFailure(appName, t);

                throw t;
            }

            deployArtifact(artifact);
        }
        catch (Throwable t)
        {
            if (t instanceof DeploymentException)
            {
                // re-throw
                throw ((DeploymentException) t);
            }

            final String msg = "Failed to deploy from URL: " + appArchiveUrl;
            throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
        }
    }

    private void guardedDeploy(T artifact)
    {
        try
        {
            if (!deploymentInProgressLcok.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }

            deployer.deploy(artifact);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if (deploymentInProgressLcok.isHeldByCurrentThread())
            {
                deploymentInProgressLcok.unlock();
            }
        }
    }

    public void deployArtifact(T artifact) throws DeploymentException
    {
        try
        {
            deploymentListener.onDeploymentStart(artifact.getArtifactName());
            guardedDeploy(artifact);
            deploymentListener.onDeploymentSuccess(artifact.getArtifactName());
            applicationZombieMap.remove(artifact.getArtifactName());
        }
        catch (Throwable t)
        {
            // error text has been created by the deployer already
            String msg = miniSplash(String.format("Failed to deploy app '%s', see below", artifact.getArtifactName()));
            logger.error(msg, t);

            addZombieApp(artifact);

            deploymentListener.onDeploymentFailure(artifact.getArtifactName(), t);
            if (t instanceof DeploymentException)
            {
                throw (DeploymentException) t;
            }
            else
            {
                msg = "Failed to deploy application: " + artifact.getArtifactName();
                throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
            }
        }
    }

    protected void addZombieApp(Artifact artifact)
    {
        final File appDir = new File(artifactFactory.getArtifactDir(), artifact.getArtifactName()) ;

        String resource = artifact.getConfigResources()[0];
        File resourceFile = new File(appDir, resource);
        ZombieFile zombieFile = new ZombieFile();

        if (resourceFile.exists())
        {
            try
            {
                zombieFile.url = resourceFile.toURI().toURL();
                zombieFile.lastUpdated = resourceFile.lastModified();

                applicationZombieMap.put(artifact.getArtifactName(), zombieFile);
            }
            catch (MalformedURLException e)
            {
                // Ignore resource
            }
        }
    }

    protected void addZombieFile(String appName, File marker)
    {
        // no sync required as deploy operations are single-threaded
        if (marker == null)
        {
            return;
        }

        if (!marker.exists())
        {
            return;
        }

        try
        {
            long lastModified = marker.lastModified();

            ZombieFile zombieFile = new ZombieFile();
            zombieFile.url = marker.toURI().toURL();
            zombieFile.lastUpdated = lastModified;

            applicationZombieMap.put(appName, zombieFile);
        }
        catch (MalformedURLException e)
        {
            logger.debug(String.format("Failed to mark an exploded app [%s] as a zombie", marker.getName()), e);
        }
    }

    public Application findApplication(String appName)
    {
        return (Application) CollectionUtils.find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, appName));
    }

    private void trackArtifact(T artifact)
    {
        Application previousApplication = findApplication(artifact.getArtifactName());
        artifacts.remove(previousApplication);

        artifacts.add(artifact);
    }

    protected void undeploy(T app)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("================== Request to Undeploy Application: " + app.getArtifactName());
        }

        try
        {
            deploymentListener.onUndeploymentStart(app.getArtifactName());

            artifacts.remove(app);
            guardedUndeploy(app);

            deploymentListener.onUndeploymentSuccess(app.getArtifactName());
        }
        catch (RuntimeException e)
        {
            deploymentListener.onUndeploymentFailure(app.getArtifactName(), e);
            throw e;
        }
    }

    private T guardedInstallFrom(URL appArchiveUrl) throws IOException
    {
        try
        {
            if (!deploymentInProgressLcok.tryLock(0, TimeUnit.SECONDS))
            {
                throw new IOException(ANOTHER_DEPLOYMENT_OPERATION_IS_IN_PROGRESS);
            }
            return deployer.installFrom(appArchiveUrl);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException(INSTALL_OPERATION_HAS_BEEN_INTERRUPTED);
        }
        finally
        {
            if (deploymentInProgressLcok.isHeldByCurrentThread())
            {
                deploymentInProgressLcok.unlock();
            }
        }
    }

    private void guardedUndeploy(T app)
    {
        try
        {
            if (!deploymentInProgressLcok.tryLock(0, TimeUnit.SECONDS))
            {
                return;
            }

            deployer.undeploy(app);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            if (deploymentInProgressLcok.isHeldByCurrentThread())
            {
                deploymentInProgressLcok.unlock();
            }
        }
    }

    public Map<URL, Long> getApplicationsZombieMap()
    {
        Map<URL, Long> result = new HashMap<URL, Long>();

        for (String app : applicationZombieMap.keySet())
        {
            ZombieFile file = applicationZombieMap.get(app);
            result.put(file.url, file.lastUpdated);
        }
        return result;
    }

    public void setDeployer(MuleDeployer deployer)
    {
        this.deployer = deployer;
    }

    public void setArtifactFactory(ArtifactFactory<T> artifactFactory)
    {
        this.artifactFactory = artifactFactory;
    }

    public ArtifactFactory getArtifactFactory()
    {
        return artifactFactory;
    }

    public MuleDeployer getDeployer()
    {
        return deployer;
    }

    private static class ZombieFile
    {
        URL url;
        Long lastUpdated;

        public boolean isFor(URL url)
        {
            return this.url.equals(url);
        }

        public boolean updatedZombieApp()
        {
            long currentTimeStamp = FileUtils.getFileTimeStamp(url);
            return lastUpdated != currentTimeStamp;
        }
    }
}
