/*
 * Copyright 2025-2026 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osmerion.omittable.spring.webflux;

import com.osmerion.omittable.Omittable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OmittableRequestParamMethodArgumentResolverTest {

    private OmittableRequestParamMethodArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        ConfigurableBeanFactory factory = mock(ConfigurableBeanFactory.class);
        resolver = new OmittableRequestParamMethodArgumentResolver(factory, ReactiveAdapterRegistry.getSharedInstance());
    }

    @Test
    @DisplayName("Should support Omittable parameters annotated with @RequestParam")
    void supportsParameter() throws Exception {
        MethodParameter supported = getParam("testSupported", Omittable.class);
        MethodParameter noAnnotation = getParam("testNoAnnotation", Omittable.class);
        MethodParameter notOmittable = getParam("testNotOmittable", String.class);

        assertTrue(resolver.supportsParameter(supported));
        assertFalse(resolver.supportsParameter(noAnnotation));
        assertFalse(resolver.supportsParameter(notOmittable));
    }

    @Test
    @DisplayName("Should resolve to Omittable.absent() when parameter is missing")
    void resolveAbsent() throws Exception {
        MethodParameter param = getParam("testSupported", Omittable.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        Object result = resolver.resolveNamedValue("userId", param, exchange);

        assertEquals(Omittable.absent(), result);
    }

    @Test
    @DisplayName("Should resolve single value to Omittable.of(String)")
    void resolveSingleValue() throws Exception {
        MethodParameter param = getParam("testSupported", Omittable.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/?userId=123").build()
        );

        Object result = resolver.resolveNamedValue("userId", param, exchange);

        assertInstanceOf(Omittable.class, result);
        assertEquals(Omittable.of("123"), result);
    }

    @Test
    @DisplayName("Should resolve multiple values to Omittable.of(List)")
    void resolveMultiValue() throws Exception {
        MethodParameter param = getParam("testSupported", Omittable.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/?userId=123&userId=456").build()
        );

        Object result = resolver.resolveNamedValue("userId", param, exchange);

        assertEquals(Omittable.of(List.of("123", "456")), result);
    }

    @Test
    @DisplayName("Should resolve values using the '[]' suffix")
    void resolveArraySuffix() throws Exception {
        MethodParameter param = getParam("testSupported", Omittable.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/?userId[]=123&userId[]=456").build()
        );

        Object result = resolver.resolveNamedValue("userId", param, exchange);

        assertEquals(Omittable.of(List.of("123", "456")), result);
    }

    // --- Helper logic to extract MethodParameters from a dummy controller ---

    private MethodParameter getParam(String methodName, Class<?>... types) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName, types);
        MethodParameter param = new MethodParameter(method, 0);
        param.initParameterNameDiscovery(null); 
        return param;
    }

    @SuppressWarnings("unused")
    private static class TestController {
        void testSupported(@RequestParam(name = "userId") Omittable<String> userId) {}
        void testNoAnnotation(Omittable<String> userId) {}
        void testNotOmittable(@RequestParam String userId) {}
    }

}
