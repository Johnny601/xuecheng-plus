package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            // fallback triggered by the circuit breaker
            @Override
            public String upload(MultipartFile filedata, String objectName) throws IOException {
                log.debug("fallback triggered by the circuit breaker: {}",throwable.toString(), throwable);
                return null;
            }
        };
    }
}
