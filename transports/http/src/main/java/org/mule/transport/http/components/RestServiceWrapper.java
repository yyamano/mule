/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.components;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mule.DefaultMuleEventContext;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.component.AbstractComponent;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.StringUtils;

/**
 * This service can used to proxy REST style services as local Mule Components. It
 * can be configured with a service URL plus a number of properties that allow you to
 * configure the parameters and error conditions on the service.
 */
public class RestServiceWrapper extends AbstractComponent
{
    private String serviceUrl;
    private Map<String, String> requiredParams = new HashMap<String, String>();
    private Map<String, String> optionalParams = new HashMap<String, String>();
    private String httpMethod = HttpConstants.METHOD_GET;
    private List<String> payloadParameterNames;
    private Filter errorFilter;

    public String getServiceUrl()
    {
        return serviceUrl;
    }

    public void setServiceUrl(final String serviceUrl)
    {
        this.serviceUrl = serviceUrl;
    }

    public Map<String, String> getRequiredParams()
    {
        return requiredParams;
    }

    /**
     * Required params that are pulled from the message. If these params don't exist
     * the call will fail Note that you can use
     * {@link org.mule.api.expression.ExpressionEvaluator} expressions such as xpath,
     * header, xquery, etc
     * 
     * @param requiredParams
     */
    public void setRequiredParams(final Map<String, String> requiredParams)
    {
        this.requiredParams = requiredParams;
    }

    /**
     * Optional params that are pulled from the message. If these params don't exist
     * execution will continue. Note that you can use {@link ExpressionEvaluator}
     * expressions such as xpath, header, xquery, etc
     */
    public Map<String, String> getOptionalParams()
    {
        return optionalParams;
    }

    public void setOptionalParams(final Map<String, String> optionalParams)
    {
        this.optionalParams = optionalParams;
    }

    public String getHttpMethod()
    {
        return httpMethod;
    }

    public void setHttpMethod(final String httpMethod)
    {
        this.httpMethod = httpMethod;
    }

    public List<String> getPayloadParameterNames()
    {
        return payloadParameterNames;
    }

    public void setPayloadParameterNames(final List<String> payloadParameterNames)
    {
        this.payloadParameterNames = payloadParameterNames;
    }

    public Filter getFilter()
    {
        return errorFilter;
    }

    public void setFilter(final Filter errorFilter)
    {
        this.errorFilter = errorFilter;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (serviceUrl == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("serviceUrl"), this);
        }

        if (!muleContext.getExpressionManager().isExpression(serviceUrl))
        {
            try
            {
                new URI(serviceUrl);
            }
            catch (final URISyntaxException urise)
            {
                throw new InitialisationException(urise, this);
            }
        }

        if (errorFilter == null)
        {
            // We'll set a default filter that checks the return code
            errorFilter = new ExpressionFilter("#[header:INBOUND:http.status!=200]");
            logger.info("Setting default error filter to ExpressionFilter('#[header:INBOUND:http.status!=200]')");
        }
    }

    @Override
    public Object doInvoke(final MuleEvent event) throws Exception
    {
        Object requestBody;

        final Object request = event.getMessage().getPayload();
        String tempUrl = serviceUrl;
        MuleMessage result;
        if (muleContext.getExpressionManager().isExpression(serviceUrl))
        {
            muleContext.getExpressionManager().validateExpression(serviceUrl);
            tempUrl = muleContext.getExpressionManager().parse(serviceUrl, event, true);
        }

        final StringBuilder urlBuffer = new StringBuilder(tempUrl);

        if (HttpConstants.METHOD_GET.equalsIgnoreCase(httpMethod)
            || HttpConstants.METHOD_DELETE.equalsIgnoreCase(httpMethod))
        {
            requestBody = NullPayload.getInstance();

            setRESTParams(urlBuffer, event, request, requiredParams, false, null);
            setRESTParams(urlBuffer, event, request, optionalParams, true, null);
        }
        else
        {
            final StringBuffer requestBodyBuffer = new StringBuffer();
            if (event.getMessage().getOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE) == null)
            {
            	event.getMessage().setOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE,
                	HttpConstants.FORM_URL_ENCODED_CONTENT_TYPE);
            }
            setRESTParams(urlBuffer, event, request, requiredParams, false, requestBodyBuffer);
            setRESTParams(urlBuffer, event, request, optionalParams, true, requestBodyBuffer);
            requestBody = requestBodyBuffer.toString();
        }

        tempUrl = urlBuffer.toString();
        logger.info("Invoking REST service: " + tempUrl);

        event.getMessage().setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, httpMethod);

        final EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(tempUrl, muleContext);
        endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
        final OutboundEndpoint outboundEndpoint = endpointBuilder.buildOutboundEndpoint();

        final MuleEventContext eventContext = new DefaultMuleEventContext(event);
        result = eventContext.sendEvent(new DefaultMuleMessage(requestBody, event.getMessage(), muleContext),
            outboundEndpoint);
        if (isErrorPayload(result))
        {
            handleException(new RestServiceException(CoreMessages.failedToInvokeRestService(tempUrl), event),
                result);
        }

        return result;
    }

    protected String getSeparator(final String url)
    {
        return (url.indexOf("?") > -1) ? "&" : "?";
    }

    protected String updateSeparator(final String sep)
    {
        return ("?".equals(sep) || StringUtils.EMPTY.equals(sep)) ? "&" : sep;
    }

    // if requestBodyBuffer is null, it means that the request is a GET, otherwise it
    // is a POST and requestBodyBuffer must contain the body of the http method at
    // the end of this function call
    protected void setRESTParams(final StringBuilder url,
                                 final MuleEvent event,
                                 final Object body,
                                 final Map<String, String> args,
                                 final boolean optional,
                                 final StringBuffer requestBodyBuffer)
    {
        String sep;

        if (requestBodyBuffer == null)
        {
            sep = getSeparator(url.toString());
        }
        else if (requestBodyBuffer.length() > 0)
        {
            sep = "&";
        }
        else
        {
            sep = StringUtils.EMPTY;
        }

        for (final Entry<String, String> entry : args.entrySet())
        {
            final String name = entry.getKey();
            final String exp = entry.getValue();
            Object value = null;

            if (muleContext.getExpressionManager().isExpression(exp))
            {
                muleContext.getExpressionManager().validateExpression(exp);
                try
                {
                    value = muleContext.getExpressionManager().evaluate(exp, event);
                }
                catch (final RequiredValueException e)
                {
                    // ignore
                }
            }
            else
            {
                value = exp;
            }

            if (value == null)
            {
                if (!optional)
                {
                    throw new IllegalArgumentException(CoreMessages.propertyIsNotSetOnEvent(exp).toString());
                }
            }
            else if (requestBodyBuffer != null) // implies this is a POST
            {
                requestBodyBuffer.append(sep);
                requestBodyBuffer.append(name).append('=').append(value);
            }
            else
            {
                url.append(sep);
                url.append(name).append('=').append(value);
            }

            sep = updateSeparator(sep);
        }

        if (!optional && payloadParameterNames != null)
        {
            if (body instanceof Object[])
            {
                final Object[] requestArray = (Object[]) body;
                for (int i = 0; i < payloadParameterNames.size(); i++)
                {
                    if (requestBodyBuffer != null)
                    {
                        requestBodyBuffer.append(sep)
                            .append(payloadParameterNames.get(i))
                            .append('=')
                            .append(requestArray[i].toString());
                    }
                    else
                    {
                        url.append(sep)
                            .append(payloadParameterNames.get(i))
                            .append('=')
                            .append(requestArray[i].toString());
                    }

                    sep = updateSeparator(sep);
                }
            }
            else
            {
                if (payloadParameterNames.get(0) != null)
                {
                    if (requestBodyBuffer != null)
                    {
                        requestBodyBuffer.append(payloadParameterNames.get(0))
                            .append('=')
                            .append(body.toString());
                    }
                    else
                    {
                        url.append(sep)
                            .append(payloadParameterNames.get(0))
                            .append('=')
                            .append(body.toString());
                    }
                }
            }
        }
    }

    protected boolean isErrorPayload(final MuleMessage message)
    {
        return errorFilter != null && errorFilter.accept(message);
    }

    protected void handleException(final RestServiceException e, final MuleMessage result) throws Exception
    {
        throw e;
    }
}
