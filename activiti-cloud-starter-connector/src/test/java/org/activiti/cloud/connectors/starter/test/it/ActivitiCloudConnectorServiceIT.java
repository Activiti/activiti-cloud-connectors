/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.connectors.starter.test.it;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.activiti.cloud.connectors.starter.model.IntegrationRequestEvent;
import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.junit.rabbit.RabbitTestSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles(ConnectorsITStreamHandlers.CONNECTOR_IT)
public class ActivitiCloudConnectorServiceIT {

    @Autowired
    private MessageChannel integrationEventsProducer;

    @ClassRule
    public static RabbitTestSupport rabbitTestSupport = new RabbitTestSupport();

    @Autowired
    private ConnectorsITStreamHandlers streamHandler;

    private final static String PROCESS_INSTANCE_ID = "processInstanceId-" + UUID.randomUUID().toString();
    private final static String PROCESS_DEFINITION_ID = "myProcessDefinitionId";
    private final static String EXECUTION_ID = "executionId-" + UUID.randomUUID().toString();

    @Before
    public void setUp() throws Exception {
        streamHandler.setExecutionId(EXECUTION_ID);
    }

    @Test
    public void integrationEventShouldBePickedByConnectorMock() throws Exception {
        //given

        Map<String, Object> variables = new HashMap<>();
        variables.put("var1",
                      "value1");
        variables.put("var2",
                      1L);

        IntegrationRequestEvent ire = new IntegrationRequestEvent(PROCESS_INSTANCE_ID,
                                                                  PROCESS_DEFINITION_ID,
                                                                  EXECUTION_ID,
                                                                  variables,
                new Service("mock-rb","mock-rb","runtime-bundle","1"),
                new Application());

        Message<IntegrationRequestEvent> message = MessageBuilder.withPayload(ire)
                .setHeader("type",
                           "Mock")
                .build();
        integrationEventsProducer.send(message);

        message = MessageBuilder.withPayload(ire)
                .setHeader("type",
                           "MockProcessRuntime")
                .build();
        integrationEventsProducer.send(message);

        await("Command should arrive")
                .untilAsserted(() ->
                                       assertThat(streamHandler.isStartProcessInstanceCmdArrived()).isTrue()
                );

        await("Should receive at least 2 integration results")
                .untilAsserted(() ->
                                       assertThat(streamHandler.getIntegrationResultEventsCounter().get()).isGreaterThanOrEqualTo(2)
                );
    }
}


