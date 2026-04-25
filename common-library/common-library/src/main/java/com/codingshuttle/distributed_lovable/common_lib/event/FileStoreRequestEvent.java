package com.codingshuttle.distributed_lovable.common_lib.event;
//Event for Kafka
public record FileStoreRequestEvent(
        Long projectId,
        String sagaId,
        String filePath,
        String content,
        Long userId
) {}