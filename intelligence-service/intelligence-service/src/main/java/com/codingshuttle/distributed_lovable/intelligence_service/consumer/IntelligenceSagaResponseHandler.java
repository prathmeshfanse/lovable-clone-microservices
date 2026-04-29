package com.codingshuttle.distributed_lovable.intelligence_service.consumer;

import com.codingshuttle.distributed_lovable.common_lib.enums.ChatEventStatus;
import com.codingshuttle.distributed_lovable.common_lib.event.FileStoreResponseEvent;
import com.codingshuttle.distributed_lovable.intelligence_service.repository.ChatEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntelligenceSagaResponseHandler {

    private final ChatEventRepository chatEventRepository;

    @Transactional
    @KafkaListener(topics = "file-store-response-event", groupId = "intelligence-group")
    public void consumeSagaResponse(FileStoreResponseEvent response) {

        chatEventRepository.findBySagaId(response.sagaId()).ifPresent(chatEvent ->{
            if(!ChatEventStatus.PENDING.equals(chatEvent.getStatus())){
                log.info("Response for saga {} already handled. Skipping...",response.sagaId());
                return;
            }

            if(response.success()){
                chatEvent.setStatus(ChatEventStatus.CONFIRMED);
                log.info("Saga {} CONFIRMED",response.sagaId());
            }else{
                log.warn("Saga {} FAILED. Deleting event",response.sagaId());
                chatEvent.setStatus(ChatEventStatus.FAILED);
            }
        });
    }
}
