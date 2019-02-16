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

package de.codecentric.boot.admin.server.services;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.values.Endpoint;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import de.codecentric.boot.admin.server.web.client.InstanceWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.logging.Level;

/**
 * @author dutianzhao
 */
public class ServiceRegsitryUpdater {
    private static final Logger log = LoggerFactory.getLogger(ServiceRegsitryUpdater.class);
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE = new ParameterizedTypeReference<Map<String, Object>>() {
    };
    private final InstanceRepository repository;
    private final InstanceWebClient instanceWebClient;
    private final StatusUpdater statusUpdater;

    public ServiceRegsitryUpdater(InstanceRepository repository, InstanceWebClient instanceWebClient, StatusUpdater statusUpdater) {
        this.repository = repository;
        this.instanceWebClient = instanceWebClient;
        this.statusUpdater = statusUpdater;
    }

    public Mono<Void> outOfService(InstanceId id) {
        return repository.computeIfPresent(id, (key, instance) -> this.updateStatus(instance, StatusInfo.STATUS_OUT_OF_SERVICE)).then();
    }

    public Mono<Void> upService(InstanceId id) {
        return repository.computeIfPresent(id, (key, instance) -> this.updateStatus(instance, StatusInfo.STATUS_UP)).then();
    }

    protected Mono<Instance> updateStatus(Instance instance, String status) {
        Assert.hasText(status, "status must not be blank");
        if (instance.getStatusInfo().isOffline() || instance.getStatusInfo().isUnknown()) {
            return Mono.empty();
        }
        if (!instance.getEndpoints().isPresent(Endpoint.SERVICE_REGISTRY)) {
            return Mono.empty();
        }

        log.debug("Update status for {}", instance);

        StatusInfo statusInfo = StatusInfo.valueOf(status);

        Mono<Instance> result = instanceWebClient.instance(instance)
                                .post()
                                .uri(Endpoint.SERVICE_REGISTRY)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .body(Mono.just(statusInfo), StatusInfo.class)
                                .exchange()
                                .log(log.getName(), Level.FINEST)
                                .flatMap(response -> Mono.just(instance))
                                .onErrorResume(ex -> Mono.just(logError(instance, ex)));

        statusUpdater.updateStatus(instance.getId());

        return result;
    }

    protected Instance logError(Instance instance, Throwable ex) {
        log.warn("Couldn't retrieve env for {}", instance, ex);
        return instance;
    }
}
