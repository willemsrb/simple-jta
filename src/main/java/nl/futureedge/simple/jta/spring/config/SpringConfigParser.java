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
}
