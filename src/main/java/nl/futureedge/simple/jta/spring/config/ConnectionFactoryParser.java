package nl.futureedge.simple.jta.spring.config;


import static nl.futureedge.simple.jta.spring.config.SpringConfigParser.isEmpty;

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

        final String jtaTransactionManager = element.getAttribute("jta-transaction-manager");
        if (!isEmpty(jtaTransactionManager)) {
            builder.addPropertyReference("jtaTransactionManager", jtaTransactionManager);
        }
        builder.addPropertyValue("uniqueName", element.getAttribute("unique-name"));
        builder.addPropertyReference("xaConnectionFactory", element.getAttribute("xa-connection-factory"));
        SpringConfigParser.handleDependsOn(builder, element);

        return builder.getBeanDefinition();
    }
}
