package nl.futureedge.simple.jta.spring.config;

import nl.futureedge.simple.jta.jdbc.xa.JdbcXADataSourceFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses an {@code xa-data-source} element and creates a {@link BeanDefinition} for an
 * {@link JdbcXADataSourceFactory}.
 */
public final class XaDataSourceParser extends AbstractBeanDefinitionParser {

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
        // XA-DATA-SOURCE Factory
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(JdbcXADataSourceFactory.class);

        builder.addPropertyValue("driver", element.getAttribute("driver"));
        builder.addPropertyValue("host", element.getAttribute("host"));
        builder.addPropertyValue("port", element.getAttribute("port"));
        builder.addPropertyValue("database", element.getAttribute("database"));
        builder.addPropertyValue("user", element.getAttribute("user"));
        builder.addPropertyValue("password", element.getAttribute("password"));

        SpringConfigParser.handleDependsOn(builder, element);

        return builder.getBeanDefinition();
    }
}
