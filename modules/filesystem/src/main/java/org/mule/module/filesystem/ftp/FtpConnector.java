/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.filesystem.ftp;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.config.PoolingProfile;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.filesystem.FileSystemConnector;
import org.mule.module.filesystem.FileSystemEntry;
import org.mule.module.filesystem.FileSystemModuleUtils;
import org.mule.module.filesystem.FileSystemParams;
import org.mule.module.filesystem.FileSystemSearchCriteria;
import org.mule.module.filesystem.ftp.exception.FtpException;
import org.mule.transport.ftp.FtpConnector;
import org.mule.util.StringUtils;
import org.mule.util.pool.CommonsPoolObjectPool;
import org.mule.util.pool.ObjectPool;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FtpConnector implements FileSystemConnector, MuleContextAware, Lifecycle
{

    private static final Logger logger = LoggerFactory.getLogger(FtpConnector.class);

    private Map<String, ObjectPool> pools = new HashMap<String, ObjectPool>();

    private FileSystemParams fileSystemParams;
    private PoolingProfile poolingProfile;
    private boolean passive = true;
    private boolean binary = true;
    private int responseTimeout = 0;
    private MuleContext muleContext;

    @Override
    public List<FileSystemEntry> listFiles(final FileSystemSearchCriteria searchCriteira,
                                           FileSystemParams params) throws MuleException
    {
        return new FtpTemplate<List<FileSystemEntry>>()
        {

            @Override
            protected List<FileSystemEntry> doExecute(final FTPClient client, final FileSystemParams params)
                throws Exception
            {
                FtpSearchFilter filter = new FtpSearchFilter(searchCriteira);
                String path = params.getPath();
                FTPFile[] files = client.listFiles(path, filter);

                if (searchCriteira.isRecursive())
                {
                    List<FileSystemEntry> retVal = new ArrayList<FileSystemEntry>();
                    for (FTPFile file : files)
                    {
                        FileSystemEntry entry = FtpUtils.parse(file, path);
                        retVal.add(entry);

                        if (file.isDirectory())
                        {
                            FileSystemParams recursiveParms = new FileSystemParams();
                            params.setPath(params.getPath() + file.getName());
                            entry.getChilds().addAll(
                                this.doExecute(client, FileSystemModuleUtils.merge(recursiveParms, params)));
                        }
                    }

                    return retVal;
                }
                else
                {
                    return FtpUtils.parse(files, path);
                }
            }

            protected String exceptionDescription(Exception e, FileSystemParams params)
            {
                return String.format("Exception found trying to list files from location %s",
                    params.getPath());
            }

        }.execute(params);
    }

    /**
     * Gets or creates a new FTPClient that logs in and changes the working directory
     * using the data provided in <code>endpoint</code>.
     */
    private FTPClient getFtpClient(ObjectPool pool, FileSystemParams params) throws IOException
    {
        FTPClient client = null;
        try
        {
            client = (FTPClient) pool.borrowObject();
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new FtpException(
                MessageFactory.createStaticMessage("Exception found while trying to get ftp"), e);
        }
        client.setDataTimeout(this.responseTimeout);

        this.enterActiveOrPassiveMode(client);
        this.setupFileType(client);

        String path = params.getPath();

        // only change directory if one was configured
        if (StringUtils.isNotBlank(path))
        {
            // MULE-2400: if the path begins with '~' we must strip the first '/' to
            // make things
            // work with FTPClient
            if ((path.length() >= 2) && (path.charAt(1) == '~'))
            {
                path = path.substring(1);
            }

            if (!client.changeWorkingDirectory(path))
            {
                throw new IOException(MessageFormat.format(
                    "Failed to change working directory to {0}. Ftp error: {1}", path, client.getReplyCode()));
            }
        }
        return client;
    }

    public void releaseClient(FTPClient client, ObjectPool pool, FileSystemParams params) throws Exception
    {

        if (client != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("<<< releasing client for " + params);
            }

            if (!client.completePendingCommand())
            {
                client.logout();
                client.disconnect();
                throw new IOException("FTP Stream failed to complete pending request");
            }
        }

        pool.returnObject(client);
    }

    private void destroyClient(FTPClient client, ObjectPool pool, FileSystemParams params) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("destroying client for " + params);
        }
        try
        {
            releaseClient(client, pool, params);
        }
        catch (Exception e)
        {
            // no way to test if pool is closed except try to access it
            logger.debug(e.getMessage());
        }
        finally {
            pool.destroyObject(client);
        }
    }

    private void enterActiveOrPassiveMode(FTPClient client)
    {
        if (this.passive)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Entering FTP passive mode");
            }
            client.enterLocalPassiveMode();
        }
        else
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Entering FTP active mode");
            }
            client.enterLocalActiveMode();
        }
    }

    private void setupFileType(FTPClient client)
    {
        int type;
        if (this.binary)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Using FTP BINARY type");
            }
            type = org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;
        }
        else
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Using FTP ASCII type");
            }
            type = org.apache.commons.net.ftp.FTP.ASCII_FILE_TYPE;
        }

        client.setFileType(type);
    }

    private ObjectPool getPool(FileSystemParams params) throws MuleException
    {
        String key = FtpUtils.toKey(params);
        synchronized (this.pools)
        {
            ObjectPool pool = this.pools.get(key);
            if (pool == null)
            {
                pool = new CommonsPoolObjectPool(null, this.poolingProfile, this.muleContext);
                try
                {
                    pool.initialise();
                }
                catch (InitialisationException e)
                {
                    throw new FtpException(
                        MessageFactory.createStaticMessage("Could not initialize ftp connection pool for params: "
                                                           + params), e);
                }
                this.pools.put(key, pool);
            }
            return pool;
        }
    }

    private abstract class FtpTemplate<T>
    {

        public final T execute(FileSystemParams params)
        {
            ObjectPool pool = getPool(params);
            FTPClient client = null;
            try
            {
                client = getFtpClient(pool, params);
                T retVal = this.doExecute(client, params);
                releaseClient(client, pool, params);
            }
            catch (Exception e)
            {
                destroyClient(client, pool, params);
                throw handleException(this.exceptionDescription(e, params), e);
            }
        }

        protected abstract T doExecute(FTPClient client, FileSystemParams params) throws Exception;

        protected abstract String exceptionDescription(Exception e, FileSystemParams params);
    }

    private MuleException handleException(String message, Throwable cause) throws MuleException
    {
        return new FtpException(MessageFactory.createStaticMessage(message), cause);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void setPassive(boolean passive)
    {
        this.passive = passive;
    }

    public void setBinary(boolean binary)
    {
        this.binary = binary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystemParams getFileSystemParams()
    {
        return this.fileSystemParams;
    }

    public void setFileSystemParams(FileSystemParams fileSystemParams)
    {
        this.fileSystemParams = fileSystemParams;
    }
}
