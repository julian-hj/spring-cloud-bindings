/*
 * Copyright 2020 the original author or authors.
 *
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
 */

package org.springframework.cloud.bindings.boot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.cloud.bindings.Binding;
import org.springframework.cloud.bindings.Bindings;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.bindings.boot.BindingsEnvironmentPostProcessor.BINDINGS_PROPERTY_SOURCE_NAME;

@DisplayName("Bindings EnvironmentPostProcessor")
final class BindingsEnvironmentPostProcessorTest {

    private final SpringApplication application = new SpringApplication();

    private final MockEnvironment environment = new MockEnvironment();

    @Test
    @DisplayName("is disabled by default")
    @ClearSystemProperty(key = "org.springframework.cloud.bindings.boot.enable")
    void disabledByDefault() {
        new BindingsEnvironmentPostProcessor(
                new Bindings(
                        new Binding("test-name", Paths.get("test-path"),
                                Collections.emptyMap(), Collections.emptyMap())
                ),
                (environment, properties) -> properties.put("test-key", "test-value")
        ).postProcessEnvironment(environment, application);

        assertThat(environment.getPropertySources()).hasSize(1);
    }

    @Nested
    @DisplayName("when enabled")
    @SetSystemProperty(key = "org.springframework.cloud.bindings.boot.enable", value = "true")
    final class Enabled {

        @Test
        @DisplayName("does not create PropertySource if no bindings")
        void noBindings() {
            new BindingsEnvironmentPostProcessor(new Bindings()).postProcessEnvironment(environment, application);

            assertThat(environment.getPropertySources()).hasSize(1);
        }

        @Test
        @DisplayName("does not create PropertySource if no properties")
        void noProperties() {
            new BindingsEnvironmentPostProcessor(
                    new Bindings(
                            new Binding("test-name", Paths.get("test-path"),
                                    Collections.emptyMap(), Collections.emptyMap())
                    )
            ).postProcessEnvironment(environment, application);

            assertThat(environment.getPropertySources()).hasSize(1);
        }

        @Test
        @DisplayName("creates PropertySource with properties")
        void containsProperties() {
            new BindingsEnvironmentPostProcessor(
                    new Bindings(
                            new Binding("test-name", Paths.get("test-path"),
                                    Collections.emptyMap(), Collections.emptyMap())
                    ),
                    (environment, properties) -> properties.put("test-key", "test-value")
            ).postProcessEnvironment(environment, application);

            assertThat(environment.getPropertySources()).hasSize(2);
            assertThat(environment.getProperty("test-key")).isEqualTo("test-value");
        }

        @Test
        @DisplayName("adds PropertySource after CommandLinePropertySource")
        void withCommandLinePropertySource() {
            environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource());

            new BindingsEnvironmentPostProcessor(
                    new Bindings(
                            new Binding("test-name", Paths.get("test-path"),
                                    Collections.emptyMap(), Collections.emptyMap())
                    ),
                    (environment, properties) -> properties.put("test-key", "test-value")
            ).postProcessEnvironment(environment, application);

            PropertySource<?> propertySource = environment.getPropertySources().get(BINDINGS_PROPERTY_SOURCE_NAME);
            assertThat(propertySource).isNotNull();
            assertThat(environment.getPropertySources().precedenceOf(propertySource)).isEqualTo(1);
        }

        @Test
        @DisplayName("adds PropertySource first")
        void withoutCommandLinePropertySource() {
            new BindingsEnvironmentPostProcessor(
                    new Bindings(
                            new Binding("test-name", Paths.get("test-path"),
                                    Collections.emptyMap(), Collections.emptyMap())
                    ),
                    (environment, properties) -> properties.put("test-key", "test-value")
            ).postProcessEnvironment(environment, application);

            PropertySource<?> propertySource = environment.getPropertySources().get(BINDINGS_PROPERTY_SOURCE_NAME);
            assertThat(propertySource).isNotNull();
            assertThat(environment.getPropertySources().precedenceOf(propertySource)).isEqualTo(0);
        }

        @Test
        @DisplayName("has order before ConfigFileApplicationListener")
        void order() {
            assertThat(new BindingsEnvironmentPostProcessor(new Bindings()).getOrder())
                    .isLessThan(ConfigFileApplicationListener.DEFAULT_ORDER);
        }

        @Test
        @DisplayName("included implementations are registered")
        void includedImplementations() {
            assertThat(new BindingsEnvironmentPostProcessor().processors).hasSize(8);
        }

    }

}
