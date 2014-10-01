/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.oauth.http;

import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpEndpointListener
{

    private Logger logger = LoggerFactory.getLogger(getClass());

    public HttpEndpointListener(final String localAuthorizationUrlAsString, final String authorizationUrl, final String listenUrlAsString, final RequestCallback requestCallback)
    {
        try
        {
            URL listenUrl = new URL(listenUrlAsString);
            URL localAuthorizationUrl = new URL(localAuthorizationUrlAsString);

            final HttpRequestHandler localAuthorizationUrlHandler = new HttpRequestHandler()
            {

                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException
                {
                    response.setStatusCode(302);
                    response.setHeader("Location", authorizationUrl);
                }
            };

            final HttpRequestHandler listenUrlHandler = new HttpRequestHandler()
            {

                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException
                {
                    String requestBody;
                    if (request instanceof HttpEntityEnclosingRequest)
                    {
                        final InputStream content = ((HttpEntityEnclosingRequest) request).getEntity().getContent();
                        requestBody = IOUtils.toString(content);
                    }
                    else
                    {
                        requestBody = request.getRequestLine().getUri();
                    }
                    requestCallback.process(requestBody);
                    response.setStatusCode(HttpStatus.SC_OK);
                }
            };

            HttpProcessor httpproc = HttpProcessorBuilder.create()
                    .add(new ResponseDate())
                    .add(new ResponseServer("Test/1.1"))
                    .add(new ResponseContent())
                    .add(new ResponseConnControl()).build();

            // Set up request handlers
            UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
            reqistry.register(listenUrl.getPath(), listenUrlHandler);
            reqistry.register(localAuthorizationUrl.getPath(), localAuthorizationUrlHandler);

            // Set up the HTTP service
            HttpService httpService = new HttpService(httpproc, reqistry);

            SSLServerSocketFactory sf = null;
            if (localAuthorizationUrlAsString.startsWith("https"))
            //if (port == 8443)
            {
                // Initialize SSL context
                URL url = getClass().getClassLoader().getResource("keystore.jks");
                if (url == null)
                {
                    System.out.println("Keystore not found");
                    System.exit(1);
                }
                KeyStore keystore = KeyStore.getInstance("jks");
                keystore.load(url.openStream(), "keyStorePassword".toCharArray());
                KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm());
                kmfactory.init(keystore, "secret".toCharArray());
                KeyManager[] keymanagers = kmfactory.getKeyManagers();
                SSLContext sslcontext = SSLContext.getInstance("TLS");
                sslcontext.init(keymanagers, null, null);
                sf = sslcontext.getServerSocketFactory();
            }

            Thread t = new RequestListenerThread(listenUrl.getPort(), httpService, sf);
            t.setDaemon(false);
            t.start();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public interface RequestCallback
    {

        void process(String requestBody);

    }

    static class RequestListenerThread extends Thread
    {

        private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
        private final ServerSocket serversocket;
        private final HttpService httpService;
        private ExecutorService executors = Executors.newFixedThreadPool(1000);

        public RequestListenerThread(
                final int port,
                final HttpService httpService,
                final SSLServerSocketFactory sf) throws IOException
        {
            this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
            this.serversocket = sf != null ? sf.createServerSocket(port) : new ServerSocket(port);
            this.httpService = httpService;
        }

        @Override
        public void run()
        {
            System.out.println("Listening on port " + this.serversocket.getLocalPort());
            while (!Thread.interrupted())
            {
                try
                {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    //System.out.println("Incoming connection from " + socket.getInetAddress());
                    HttpServerConnection conn = this.connFactory.createConnection(socket);

                    // Start worker thread
                    executors.execute(new WorkerThread(this.httpService, conn));
                }
                catch (InterruptedIOException ex)
                {
                    break;
                }
                catch (IOException e)
                {
                    System.err.println("I/O error initialising connection thread: "
                                       + e.getMessage());
                    break;
                }
            }
        }
    }

    static class WorkerThread implements Runnable
    {

        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(
                final HttpService httpservice,
                final HttpServerConnection conn)
        {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }

        @Override
        public void run()
        {
            System.out.println("New connection thread");
            HttpContext context = new BasicHttpContext(null);
            try
            {
                while (!Thread.interrupted() && this.conn.isOpen())
                {
                    this.httpservice.handleRequest(this.conn, context);
                }
            }
            catch (ConnectionClosedException ex)
            {
                System.err.println("Client closed connection");
            }
            catch (IOException ex)
            {
                System.err.println("I/O error: " + ex.getMessage());
            }
            catch (HttpException ex)
            {
                System.err.println("Unrecoverable HTTP protocol violation: " + ex.getMessage());
            }
            finally
            {
                try
                {
                    this.conn.shutdown();
                }
                catch (IOException ignore)
                {
                }
            }
        }

    }

}
