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
package com.osmerion.omittable.spring.web;

import com.osmerion.omittable.Omittable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class OmittableRequestParamMethodArgumentResolverTest {

    private OmittableRequestParamMethodArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new OmittableRequestParamMethodArgumentResolver();
    }

    @Test
    @DisplayName("Should support Omittable with @RequestParam")
    void supportsParameter() throws Exception {
        MethodParameter supported = getParam("testSupported", Omittable.class);
        MethodParameter noAnnotation = getParam("testNoAnnotation", Omittable.class);
        MethodParameter wrongType = getParam("testWrongType", String.class);

        assertTrue(resolver.supportsParameter(supported));
        assertFalse(resolver.supportsParameter(noAnnotation));
        assertFalse(resolver.supportsParameter(wrongType));
    }

    @Test
    @DisplayName("Should return Omittable.absent() when parameter is missing")
    void resolveAbsent() throws Exception {
        MethodParameter param = getParam("testSupported", Omittable.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Object result = resolver.resolveName("userId", param, webRequest);

        assertEquals(Omittable.absent(), result);
    }

    @Test
    @DisplayName("Should resolve single value to Omittable.of(String)")
    void resolveSingleValue() throws Exception {
        MethodParameter param = getParam("testSupported", Omittable.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "123");
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Object result = resolver.resolveName("userId", param, webRequest);

        assertEquals(Omittable.of("123"), result);
    }

    @Test
    @DisplayName("Should resolve multiple values to Omittable.of(String[])")
    void resolveMultiValue() throws Exception {
        MethodParameter param = getParam("testSupported", Omittable.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "123");
        request.addParameter("userId", "456");
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Object result = resolver.resolveName("userId", param, webRequest);

        // Note: Servlet-based resolver returns String[] for multiple values
        assertInstanceOf(Omittable.class, result);
        assertArrayEquals(new String[]{"123", "456"}, (String[]) ((Omittable<?>) result).orElseThrow());
    }

    @Test
    @DisplayName("Should resolve values with '[]' suffix")
    void resolveArraySuffix() throws Exception {
        MethodParameter param = getParam("testSupported", Omittable.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId[]", "abc");
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Object result = resolver.resolveName("userId", param, webRequest);

        assertEquals(Omittable.of("abc"), result);
    }

    // --- Helpers ---

    private MethodParameter getParam(String methodName, Class<?>... types) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName, types);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private static class TestController {
        void testSupported(@RequestParam(name = "userId") Omittable<String> userId) {}
        void testNoAnnotation(Omittable<String> userId) {}
        void testWrongType(@RequestParam String userId) {}
    }

}
