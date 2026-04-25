package com.codingshuttle.distributed_lovable.common_lib.event;

import lombok.Builder;
//Kafka Response Event
@Builder
public record FileStoreResponseEvent(
        String sagaId,
        boolean success,
        String errorMessage,
        Long projectId
) {}