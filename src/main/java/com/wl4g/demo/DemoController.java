/*
 * Copyright 2017 ~ 2025 the original author or authors. James Wong <jameswrong@gmail.com>
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
package com.wl4g.demo;

import static java.lang.System.currentTimeMillis;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DemoController}
 * 
 * @author James Wrong
 * @version 2022-08-01
 * @since v3.0.0
 */
@RestController
@RequestMapping("demo")
@Slf4j
public class DemoController {

    @Value("${info.app.version}")
    private String appversion;

    @RequestMapping(path = "echo", method = { RequestMethod.GET, RequestMethod.POST })
    public EchoMessage echo(HttpServletRequest request, @RequestBody(required = false) String body) {
        log.info("[{}:called:echo()] uri={}, body={}", appversion, request.getRequestURI(), body);
        EchoMessage msg = new EchoMessage();
        msg.setTimestamp(currentTimeMillis());
        msg.setAppversion(appversion);
        msg.setMethod(request.getMethod());
        msg.setPath(request.getRequestURI());
        Enumeration<String> en = request.getHeaderNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement();
            msg.getHeaders().put(name, request.getHeader(name));
        }
        msg.setBody(body);
        return msg;
    }

    @Getter
    @Setter
    public static class EchoMessage {
        private long timestamp;
        private String appversion;
        private String method;
        private String path;
        private Map<String, String> headers = new HashMap<>();
        private String body;
    }

}
