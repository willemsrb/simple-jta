package nl.futureedge.simple.jta.spring.config;

/*-
 * #%L
 * Simple JTA
 * %%
 * Copyright (C) 2017 Future Edge IT
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import nl.futureedge.simple.jta.jms.XAConnectionFactoryAdapter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses an {@code connection-factory} element and creates a {@link BeanDefinition} for
 * an {@link XAConnectionFactoryAdapter}.
 */
public final class ConnectionFactoryParser extends AbstractBeanDefinitionParser {

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

    @Override
    protected boolean shouldParseNameAsAliases() {
        return true;
    }

    @Override
    protected AbstractBeanDefinition parseInternal(final Element element, final ParserContext parserContext) {
        // CONNECTION-FACTORY
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(XAConnectionFactoryAdapter.class);
        builder.addPropertyValue("uniqueName", element.getAttribute("unique-name"));
        builder.addPropertyReference("xaConnectionFactory", element.getAttribute("xa-connection-factory"));
        SpringConfigParser.handleDependsOn(builder, element);

        return builder.getBeanDefinition();
    }
}
