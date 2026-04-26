package com.codingshuttle.distributed_lovable.workspace_service.consumer;

import com.codingshuttle.distributed_lovable.common_lib.event.FileStoreRequestEvent;
import com.codingshuttle.distributed_lovable.common_lib.event.FileStoreResponseEvent;
import com.codingshuttle.distributed_lovable.workspace_service.entity.ProcessedEvent;
import com.codingshuttle.distributed_lovable.workspace_service.repository.ProcessedEventRepository;
import com.codingshuttle.distributed_lovable.workspace_service.service.ProjectFileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Transactional
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageConsumer {

    private final ProjectFileService projectFileService;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "file-storage-request-event", groupId = "workspace-group")
    public void consumeFileEvent(FileStoreRequestEvent fileStoreRequestEvent){

        //Idempotency check
        if(processedEventRepository.existsById(fileStoreRequestEvent.sagaId())){
            log.info("Duplicate Saga Detected: {}. Resending Previous Ack.", fileStoreRequestEvent.sagaId());
            sendResponse(fileStoreRequestEvent, true, null);
            return;
        }

        try{
            log.info("Saving file: {}", fileStoreRequestEvent.filePath());
            projectFileService.saveFile(fileStoreRequestEvent.projectId(), fileStoreRequestEvent.filePath(), fileStoreRequestEvent.content());
            //Idempotency saving id in repo
            processedEventRepository.save(new ProcessedEvent(
                    fileStoreRequestEvent.sagaId(), LocalDateTime.now()
            ));

            sendResponse(fileStoreRequestEvent, true, null);

        }catch (Exception e){
            log.error("Error while saving file: {}", fileStoreRequestEvent.filePath(), e.getMessage());
            sendResponse(fileStoreRequestEvent, false, e.getMessage());
        }
    }

    private void sendResponse(FileStoreRequestEvent fileStoreRequestEvent, boolean success, String err) {
        FileStoreResponseEvent response = FileStoreResponseEvent.builder()
                .sagaId(fileStoreRequestEvent.sagaId())
                .projectId(fileStoreRequestEvent.projectId())
                .success(success)
                .errorMessage(err)
                .build();
        kafkaTemplate.send("file-store-response-event", response);

    }
}
