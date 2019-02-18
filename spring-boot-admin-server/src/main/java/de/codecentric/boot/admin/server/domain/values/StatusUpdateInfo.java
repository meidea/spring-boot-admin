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

import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Instance status with details fetched from the info endpoint.
 *
 * @author Johannes Edmeier
 */
@lombok.Data
public class StatusUpdateInfo implements Serializable {

    private final String status;

    public StatusUpdateInfo(String status) {
        Assert.hasText(status, "'status' must not be empty.");
        this.status = status.toUpperCase();
    }
}
