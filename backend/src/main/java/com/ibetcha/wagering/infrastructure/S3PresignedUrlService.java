package com.ibetcha.wagering.infrastructure;

import com.ibetcha.wagering.application.EvidenceStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@Profile("!test")
public class S3PresignedUrlService implements EvidenceStoragePort {

    private final String bucketName;
    private final S3Presigner presigner;

    public S3PresignedUrlService(
            @Value("${ibetcha.aws.s3.evidence-bucket-name}") String bucketName,
            @Value("${ibetcha.aws.region:eu-west-1}") String region) {
        this.bucketName = bucketName;
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public String generateUploadUrl(String s3Key, String contentType, int expiresInSeconds) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expiresInSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }
}
