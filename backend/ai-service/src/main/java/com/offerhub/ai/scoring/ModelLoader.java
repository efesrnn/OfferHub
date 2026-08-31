package com.offerhub.ai.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
public class ModelLoader {

    private ModelWeights weights;

    @PostConstruct
    public void load() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("ai-model/model_weights.json");
        weights = mapper.readValue(resource.getInputStream(), ModelWeights.class);
        log.info("AI modeli yüklendi — öneri modeli test doğruluğu: {}, segment modeli test doğruluğu: {}",
                weights.recommendationModel.testAccuracy, weights.segmentModel.testAccuracy);
    }
}