
package org.mule.transport.nio.tcp;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.mule.api.lifecycle.CreateException;

/**
 * A factory that builds {@link TcpClient}s.
 */
public class TcpClientFactory extends BaseKeyedPoolableObjectFactory
{
    private final TcpConnector tcpConnector;

    public TcpClientFactory(final TcpConnector tcpConnector)
    {
        super();
        this.tcpConnector = tcpConnector;
    }

    @Override
    public Object makeObject(final Object key) throws Exception
    {
        final TcpClientKey tcpSocketKey = (TcpClientKey) key;
        final TcpClient tcpClient = newTcpClient(tcpSocketKey);
        tcpClient.connect();
        return tcpClient;
    }

    protected TcpClient newTcpClient(final TcpClientKey tcpSocketKey) throws CreateException
    {
        return new TcpClient(tcpConnector, tcpSocketKey.getConnectable(),
            tcpSocketKey.getEndpoint());
    }

    @Override
    public void destroyObject(final Object key, final Object obj) throws Exception
    {
        final TcpClient tcpClient = (TcpClient) obj;
        tcpClient.disconnect();
    }

    @Override
    public boolean validateObject(final Object key, final Object obj)
    {
        final TcpClient tcpClient = (TcpClient) obj;
        return tcpClient.isValid();
    }
}
