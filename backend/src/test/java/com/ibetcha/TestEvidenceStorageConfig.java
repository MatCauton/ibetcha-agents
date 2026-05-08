package com.ibetcha;

import com.ibetcha.wagering.application.EvidenceStoragePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Provides a no-AWS EvidenceStoragePort for all integration tests running with profile "test".
 * Replaces S3PresignedUrlService (which is @Profile("!test")) so no AWS credentials are needed.
 */
@Configuration
@Profile("test")
public class TestEvidenceStorageConfig {

    @Bean
    EvidenceStoragePort testEvidenceStoragePort() {
        return (s3Key, contentType, expiresInSeconds) ->
                "https://mock-s3.local/" + s3Key;
    }
}
