package nl.futureedge.simple.jta.spring.config;


import java.util.List;
import nl.futureedge.simple.jta.JtaTransactionManager;
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

        // TRANSACTION-STORE
        final String transactionStoreBeanName = addTransactionStore(id == null ? null : id + "-jtaTransaction", element, parserContext);

        // JTA-TRANSACTION-MANAGER
        final BeanDefinitionBuilder jtaTransactionManagerBuilder = BeanDefinitionBuilder.rootBeanDefinition(JtaTransactionManager.class);
        jtaTransactionManagerBuilder.addPropertyValue("uniqueName", element.getAttribute("unique-name"));
        jtaTransactionManagerBuilder.addPropertyReference("jtaTransactionStore", transactionStoreBeanName);
        SpringConfigParser.handleDependsOn(jtaTransactionManagerBuilder, element);

        final BeanDefinition jtaTransactionManager = jtaTransactionManagerBuilder.getBeanDefinition();
        final String jtaTransactionManagerBeanName = register(id == null ? null : id + "-jtaTransactionManager", jtaTransactionManager, parserContext);

        // SPRING-JTA-TRANSACTION-MANAGER
        final BeanDefinitionBuilder
                springJtaTransactionManagerBuilder =
                BeanDefinitionBuilder.rootBeanDefinition(org.springframework.transaction.jta.JtaTransactionManager.class);
        springJtaTransactionManagerBuilder.addPropertyReference("transactionManager", jtaTransactionManagerBeanName);
        springJtaTransactionManagerBuilder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
        return springJtaTransactionManagerBuilder.getBeanDefinition();
    }

    private String addTransactionStore(final String id, final Element element, final ParserContext parserContext) {
        final List<Element> jdbcTransactionStores = DomUtils.getChildElementsByTagName(element, "jdbc-transaction-store");
        if (!jdbcTransactionStores.isEmpty()) {
            return addJdbcTransactionStore(id, jdbcTransactionStores.iterator().next(), parserContext);
        }

        final List<Element> fileTransactionStores = DomUtils.getChildElementsByTagName(element, "file-transaction-store");
        if (!fileTransactionStores.isEmpty()) {
            return addFileTransactionStore(id, fileTransactionStores.iterator().next(), parserContext);
        }

        // Should not happen, the XSD should make sure of that
        return null;
    }

    private String addFileTransactionStore(final String id, final Element element, final ParserContext parserContext) {
        // FILE-TRANSACTION-STORE
        final BeanDefinitionBuilder fileTransactionStoreBuilder = BeanDefinitionBuilder.rootBeanDefinition(FileTransactionStore.class);

        fileTransactionStoreBuilder.addPropertyValue("baseDirectory", element.getAttribute("location"));
        final String storeAll = element.getAttribute("store-all-states");
        if (storeAll != null && !"".equals(storeAll)) {
            fileTransactionStoreBuilder.addPropertyValue("storeAll", storeAll);
        }
        SpringConfigParser.handleDependsOn(fileTransactionStoreBuilder, element);

        final BeanDefinition fileTransactionStore = fileTransactionStoreBuilder.getBeanDefinition();
        return register(id, fileTransactionStore, parserContext);
    }


    private String addJdbcTransactionStore(final String id, final Element element, final ParserContext parserContext) {
        // JDBC-TRANSACTION-STORE
        final BeanDefinitionBuilder jdbcTransactionStoreBuilder = BeanDefinitionBuilder.rootBeanDefinition(JdbcTransactionStore.class);

        final String create = element.getAttribute("create");
        if (create != null && !"".equals(create)) {
            jdbcTransactionStoreBuilder.addPropertyValue("create", create);
        }
        jdbcTransactionStoreBuilder.addPropertyValue("driver", element.getAttribute("driver"));
        jdbcTransactionStoreBuilder.addPropertyValue("url", element.getAttribute("url"));
        jdbcTransactionStoreBuilder.addPropertyValue("user", element.getAttribute("user"));
        jdbcTransactionStoreBuilder.addPropertyValue("password", element.getAttribute("password"));
        final String storeAll = element.getAttribute("store-all-states");
        if (storeAll != null && !"".equals(storeAll)) {
            jdbcTransactionStoreBuilder.addPropertyValue("storeAll", storeAll);
        }
        SpringConfigParser.handleDependsOn(jdbcTransactionStoreBuilder, element);

        final BeanDefinition jdbcTransactionStore = jdbcTransactionStoreBuilder.getBeanDefinition();
        return register(id, jdbcTransactionStore, parserContext);
    }

    private String register(final String id, final BeanDefinition definition, final ParserContext parserContext) {
        final String name = id == null ? parserContext.getReaderContext().generateBeanName(definition) : id;
        parserContext.getRegistry().registerBeanDefinition(name, definition);
        return name;
    }
}
