package nl.futureedge.simple.jta.spring.config;


import static nl.futureedge.simple.jta.spring.config.SpringConfigParser.isEmpty;
import static nl.futureedge.simple.jta.spring.config.SpringConfigParser.whenEmpty;

import java.util.List;
import nl.futureedge.simple.jta.JtaTransactionManager;
import nl.futureedge.simple.jta.store.NoTransactionStore;
import nl.futureedge.simple.jta.store.file.FileTransactionStore;
import nl.futureedge.simple.jta.store.jdbc.JdbcTransactionStore;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;


/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses an {@code transaction-manager} element and creates a {@link BeanDefinition}
 * for an {@link org.springframework.transaction.jta.JtaTransactionManager}.
 */
public final class TransactionManagerParser extends AbstractBeanDefinitionParser {

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
        final String id = element.getAttribute("id");

        System.out.println("ID: " + id);

        // TRANSACTION-STORE
        final String transactionStoreBeanName = addTransactionStore(isEmpty(id) ? null : id + "-jtaTransactionStore", element, parserContext);

        // JTA-TRANSACTION-MANAGER
        final BeanDefinitionBuilder jtaTransactionManagerBuilder = BeanDefinitionBuilder.rootBeanDefinition(JtaTransactionManager.class);
        jtaTransactionManagerBuilder.addPropertyValue("uniqueName", element.getAttribute("unique-name"));
        jtaTransactionManagerBuilder.addPropertyReference("jtaTransactionStore", transactionStoreBeanName);
        SpringConfigParser.handleDependsOn(jtaTransactionManagerBuilder, element);

        final BeanDefinition jtaTransactionManager = jtaTransactionManagerBuilder.getBeanDefinition();
        final String defaultJtaTransactionManagerId = isEmpty(id) ? null : id + "-jtaTransactionManager";
        final String jtaTransactionManagerId = element.getAttribute("jta-transaction-manager-id");
        final String jtaTransactionManagerBeanName =
                register(whenEmpty(jtaTransactionManagerId, defaultJtaTransactionManagerId), jtaTransactionManager, parserContext);

        // SPRING-JTA-TRANSACTION-MANAGER
        final BeanDefinitionBuilder springJtaTransactionManagerBuilder =
                BeanDefinitionBuilder.rootBeanDefinition(org.springframework.transaction.jta.JtaTransactionManager.class);
        springJtaTransactionManagerBuilder.addPropertyReference("transactionManager", jtaTransactionManagerBeanName);
        springJtaTransactionManagerBuilder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
        return springJtaTransactionManagerBuilder.getBeanDefinition();
    }

    private String addTransactionStore(final String defaultId, final Element element, final ParserContext parserContext) {
        System.out.println("AddTransactionStore.defaultId: " + defaultId);


        final List<Element> jdbcTransactionStores = DomUtils.getChildElementsByTagName(element, "jdbc-transaction-store");
        if (!jdbcTransactionStores.isEmpty()) {
            return addJdbcTransactionStore(defaultId, jdbcTransactionStores.iterator().next(), parserContext);
        }

        final List<Element> fileTransactionStores = DomUtils.getChildElementsByTagName(element, "file-transaction-store");
        if (!fileTransactionStores.isEmpty()) {
            return addFileTransactionStore(defaultId, fileTransactionStores.iterator().next(), parserContext);
        }

        final List<Element> noTransactionStores = DomUtils.getChildElementsByTagName(element, "no-transaction-store");
        if (!noTransactionStores.isEmpty()) {
            return addNoTransactionStore(defaultId, fileTransactionStores.iterator().next(), parserContext);
        }

        // Should not happen, the XSD should make sure of that
        return null;
    }

    private String addJdbcTransactionStore(final String defaultId, final Element element, final ParserContext parserContext) {
        // JDBC-TRANSACTION-STORE
        final BeanDefinitionBuilder jdbcTransactionStoreBuilder = BeanDefinitionBuilder.rootBeanDefinition(JdbcTransactionStore.class);

        final String id = element.getAttribute("id");
        final String create = element.getAttribute("create");
        if (!isEmpty(create)) {
            jdbcTransactionStoreBuilder.addPropertyValue("create", create);
        }
        jdbcTransactionStoreBuilder.addPropertyValue("driver", element.getAttribute("driver"));
        jdbcTransactionStoreBuilder.addPropertyValue("url", element.getAttribute("url"));
        jdbcTransactionStoreBuilder.addPropertyValue("user", element.getAttribute("user"));
        jdbcTransactionStoreBuilder.addPropertyValue("password", element.getAttribute("password"));
        final String storeAll = element.getAttribute("store-all-states");
        if (!isEmpty(storeAll)) {
            jdbcTransactionStoreBuilder.addPropertyValue("storeAll", storeAll);
        }
        SpringConfigParser.handleDependsOn(jdbcTransactionStoreBuilder, element);

        final BeanDefinition jdbcTransactionStore = jdbcTransactionStoreBuilder.getBeanDefinition();
        return register(whenEmpty(id, defaultId), jdbcTransactionStore, parserContext);
    }

    private String addFileTransactionStore(final String defaultId, final Element element, final ParserContext parserContext) {
        // FILE-TRANSACTION-STORE
        final BeanDefinitionBuilder fileTransactionStoreBuilder = BeanDefinitionBuilder.rootBeanDefinition(FileTransactionStore.class);

        final String id = element.getAttribute("id");
        System.out.println("addFileTransactionStore.id: " + id);
        fileTransactionStoreBuilder.addPropertyValue("baseDirectory", element.getAttribute("location"));
        final String storeAll = element.getAttribute("store-all-states");
        if (!isEmpty(storeAll)) {
            fileTransactionStoreBuilder.addPropertyValue("storeAll", storeAll);
        }
        SpringConfigParser.handleDependsOn(fileTransactionStoreBuilder, element);

        final BeanDefinition fileTransactionStore = fileTransactionStoreBuilder.getBeanDefinition();
        return register(whenEmpty(id, defaultId), fileTransactionStore, parserContext);
    }


    private String addNoTransactionStore(final String defaultId, final Element element, final ParserContext parserContext) {
        // NO-TRANSACTION-STORE
        final BeanDefinitionBuilder noTransactionStoreBuilder = BeanDefinitionBuilder.rootBeanDefinition(NoTransactionStore.class);

        final String id = element.getAttribute("id");
        final String suppressWarning = element.getAttribute("suppress-warning");
        if (!isEmpty(suppressWarning)) {
            noTransactionStoreBuilder.addPropertyValue("suppressWarning", suppressWarning);
        }

        final BeanDefinition noTransactionStore = noTransactionStoreBuilder.getBeanDefinition();
        return register(whenEmpty(id, defaultId), noTransactionStore, parserContext);
    }


    private String register(final String id, final BeanDefinition definition, final ParserContext parserContext) {
        System.out.println("register.id: " + id);
        final String name = whenEmpty(id, parserContext.getReaderContext().generateBeanName(definition));
        System.out.println("register.name: " + name);
        parserContext.getRegistry().registerBeanDefinition(name, definition);
        return name;
    }
}
