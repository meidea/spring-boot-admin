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
import de.codecentric.boot.admin.server.domain.values.EnvInfo;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.web.client.InstanceWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.logging.Level;

import static de.codecentric.boot.admin.server.utils.MediaType.ACTUATOR_V2_MEDIATYPE;

/**
 * @author dutianzhao
 */
public class EnvUpdater {
    private static final Logger log = LoggerFactory.getLogger(EnvUpdater.class);
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE = new ParameterizedTypeReference<Map<String, Object>>() {
    };
    private final InstanceRepository repository;
    private final InstanceWebClient instanceWebClient;

    public EnvUpdater(InstanceRepository repository, InstanceWebClient instanceWebClient) {
        this.repository = repository;
        this.instanceWebClient = instanceWebClient;
    }

    public Mono<Void> updateEnv(InstanceId id) {
        return repository.computeIfPresent(id, (key, instance) -> this.doUpdateEnv(instance)).then();
    }

    protected Mono<Instance> doUpdateEnv(Instance instance) {
        if (instance.getStatusInfo().isOffline() || instance.getStatusInfo().isUnknown()) {
            return Mono.empty();
        }
        if (!instance.getEndpoints().isPresent(Endpoint.ENV)) {
            return Mono.empty();
        }

        log.debug("Update env for {}", instance);
        return instanceWebClient.instance(instance)
                                .get()
                                .uri(Endpoint.ENV)
                                .exchange()
                                .log(log.getName(), Level.FINEST)
                                .flatMap(response -> convertEnv(instance, response))
                                .onErrorResume(ex -> Mono.just(convertEnv(instance, ex)))
                                .map(instance::withEnvInfo);
    }

    protected Mono<EnvInfo> convertEnv(Instance instance, ClientResponse response) {
        if (response.statusCode().is2xxSuccessful() &&
            response.headers()
                    .contentType()
                    .map(mt -> mt.isCompatibleWith(MediaType.APPLICATION_JSON) ||
                               mt.isCompatibleWith(ACTUATOR_V2_MEDIATYPE))
                    .orElse(false)) {
            return response.bodyToMono(RESPONSE_TYPE).map(EnvInfo::from).defaultIfEmpty(EnvInfo.empty());
        }
        log.info("Couldn't retrieve env for {}: {}", instance, response.statusCode());
        return response.bodyToMono(Void.class).then(Mono.just(EnvInfo.empty()));
    }

    protected EnvInfo convertEnv(Instance instance, Throwable ex) {
        log.warn("Couldn't retrieve env for {}", instance, ex);
        return EnvInfo.empty();
    }
}
