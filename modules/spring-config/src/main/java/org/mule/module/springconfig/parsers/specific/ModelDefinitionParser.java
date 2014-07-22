/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.mule.module.springconfig.parsers.delegate.InheritDefinitionParser;
import org.mule.module.springconfig.parsers.generic.NamedDefinitionParser;
import org.mule.module.springconfig.parsers.generic.OrphanDefinitionParser;
import org.mule.module.springconfig.parsers.processors.ProvideDefaultName;
import org.mule.model.seda.SedaModel;

@Deprecated
public class ModelDefinitionParser extends InheritDefinitionParser
{

    public ModelDefinitionParser()
    {
        super(makeOrphan(), new NamedDefinitionParser());
    }

    private static OrphanDefinitionParser makeOrphan()
    {
        OrphanDefinitionParser orphan = new OrphanDefinitionParser(SedaModel.class, true);
        orphan.registerPreProcessor(new ProvideDefaultName("model"));
        return orphan;
    }

}
