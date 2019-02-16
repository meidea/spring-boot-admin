/*
 * Copyright 2014-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.boot.admin.server.domain.values;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dutianzhao
 */
@lombok.Data
public class EnvInfo implements Serializable {
    private static final EnvInfo EMPTY = new EnvInfo(Collections.emptyMap());

    private String cloud;

    private String serverPort;
    private String ipAddress;
    private String hostname;

    private final Map<String, Object> values;

    private EnvInfo(Map<String, Object> values) {
        if (values.isEmpty()) {
            this.values = Collections.emptyMap();
        } else {
            this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            this.serverPort = fetchValue("commandLineArgs", "server.port");
            if (!StringUtils.hasText(serverPort)) {
                this.serverPort = fetchValue("server.ports", "local.server.port");
            }
            this.ipAddress = fetchValue("springCloudClientHostInfo", "spring.cloud.client.ipAddress");
            this.hostname = fetchValue("springCloudClientHostInfo", "spring.cloud.client.hostname");
            this.cloud = fetchCloud(hostname);
        }
    }

    public static EnvInfo from(@Nullable Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return empty();
        }
        return new EnvInfo(values);
    }

    public static EnvInfo empty() {
        return EMPTY;
    }

    @JsonAnyGetter
    public Map<String, Object> getValues() {
        return this.values;
    }

    public String fetchValue(String firstKey, String secondKey) {
        if (!StringUtils.hasText(firstKey)) {
            return null;
        }
        Object propertySources = values.get("propertySources");
        if (null == propertySources) {
            return null;
        }
        List<?> propertySourceList = (List<?>) propertySources;
        for (Object propertySource : propertySourceList) {
            Map<String, Object> propertySourcesMap = (Map<String, Object>) propertySource;
            if (!propertySourcesMap.get("name").equals(firstKey)) {
                continue;
            }
            Object value = propertySourcesMap.get("properties");
            if (null == value) {
                continue;
            }
            Map<String, Object> first = (Map<String, Object>) value;
            if (!first.containsKey(secondKey)) {
                continue;
            }
            return (String) first.get(secondKey);
        }
        return null;
    }

    public String fetchCloud(String hostname) {
        if (!StringUtils.hasText(hostname)) {
            return null;
        }
        if (hostname.endsWith(".aws.dm") || hostname.endsWith(".aws.dm.vipkid.com.cn")) {
            return "ali";
        } else if (hostname.endsWith(".ten.dm") || hostname.endsWith(".ten.dm.vipkid.com.cn")) {
            return "ten";
        } else if (hostname.endsWith(".ali.dm") || hostname.endsWith(".ali.dm.vipkid.com.cn")) {
            return "ali";
        } else {
            return null;
        }
    }
}
