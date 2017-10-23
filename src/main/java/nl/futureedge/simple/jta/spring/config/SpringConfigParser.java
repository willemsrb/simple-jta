package nl.futureedge.simple.jta.spring.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

public final class SpringConfigParser {
    
    public static void handleDependsOn(final BeanDefinitionBuilder builder, final Element element) {
        final String dependsOn = element.getAttribute("depends-on");
        if (dependsOn != null && !"".equals(dependsOn)) {
            final String[] beanNames = dependsOn.trim().split(",");
            for (String beanName : beanNames) {
                if (!"".equals(beanName.trim())) {
                    builder.addDependsOn(beanName.trim());
                }
            }
        }
    }
}
