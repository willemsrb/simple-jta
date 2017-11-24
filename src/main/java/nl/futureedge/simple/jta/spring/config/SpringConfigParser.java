package nl.futureedge.simple.jta.spring.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * Spring configuration utilities.
 */
public final class SpringConfigParser {

    private SpringConfigParser() {
        throw new IllegalStateException("Class should not be instantiated");
    }

    public static void handleDependsOn(final BeanDefinitionBuilder builder, final Element element) {
        final String dependsOn = element.getAttribute("depends-on");
        if (dependsOn != null && !"".equals(dependsOn)) {
            final String[] beanNames = dependsOn.trim().split(",");
            for (final String beanName : beanNames) {
                if (!"".equals(beanName.trim())) {
                    builder.addDependsOn(beanName.trim());
                }
            }
        }
    }

    public static boolean isEmpty(final String value) {
        return value == null || "".equals(value);
    }

    public static String whenEmpty(final String value, final String whenEmpty) {
        return isEmpty(value) ? whenEmpty : value;
    }
}
