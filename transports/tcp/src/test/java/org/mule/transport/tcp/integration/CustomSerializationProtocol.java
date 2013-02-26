/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.integration;

import org.mule.transport.tcp.protocols.DirectProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang.SerializationUtils;

public class CustomSerializationProtocol extends DirectProtocol
{

    @Override
    public void write(final OutputStream os, final Object data) throws IOException
    {
        if (data instanceof NonSerializableMessageObject)
        {
            final NonSerializableMessageObject in = (NonSerializableMessageObject) data;

            // do serialization... will use normal Serialization to simplify code...
            final MessageObject serializableObject = new MessageObject(in.i, in.s, in.b);

            write(os, SerializationUtils.serialize(serializableObject));
        }
        else
        {
            super.write(os, data);
        }
    }

    @Override
    public Object read(final InputStream is) throws IOException
    {
        final byte[] tmp = (byte[]) super.read(is);

        if (tmp == null)
        {
            return null;
        }
        else
        {
            final MessageObject serializableObject = (MessageObject) SerializationUtils.deserialize(tmp);
            return new NonSerializableMessageObject(serializableObject.i, serializableObject.s,
                serializableObject.b);
        }
    }

}
