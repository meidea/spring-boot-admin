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

    private final Map<String, Object> values;

    private EnvInfo(Map<String, Object> values) {
        if (values.isEmpty()) {
            this.values = Collections.emptyMap();
        } else {
            this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
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
            Object firstValue = propertySourcesMap.get("properties");
            if (null == firstValue) {
                continue;
            }
            Map<String, Object> firstMap = (Map<String, Object>) firstValue;
            if (!firstMap.containsKey(secondKey)) {
                continue;
            }

            Object secondValue = firstMap.get(secondKey);
            if (null == secondValue) {
                continue;
            }
            Map<String, Object> secondMap = (Map<String, Object>) secondValue;
            return String.valueOf(secondMap.get("value"));
        }
        return null;
    }
}
