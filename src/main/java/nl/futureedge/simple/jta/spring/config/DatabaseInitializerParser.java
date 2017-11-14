package nl.futureedge.simple.jta.spring.config;


import nl.futureedge.simple.jta.store.jdbc.spring.DatabaseInitializer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses an {@code initalize-database} element and creates a {@link BeanDefinition} for
 * an {@link DatabaseInitializer}.
 */
public final class DatabaseInitializerParser extends AbstractBeanDefinitionParser {

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

    @Override
    protected AbstractBeanDefinition parseInternal(final Element element, final ParserContext parserContext) {
        // DATABASE-INITIALIZER
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DatabaseInitializer.class);

        builder.addPropertyValue("driver", element.getAttribute("driver"));
        builder.addPropertyValue("url", element.getAttribute("url"));
        builder.addPropertyValue("user", element.getAttribute("user"));
        builder.addPropertyValue("password", element.getAttribute("password"));
        SpringConfigParser.handleDependsOn(builder, element);

        return builder.getBeanDefinition();
    }
}
