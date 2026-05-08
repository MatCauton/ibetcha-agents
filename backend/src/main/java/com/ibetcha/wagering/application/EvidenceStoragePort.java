package com.ibetcha.wagering.application;

public interface EvidenceStoragePort {

    /**
     * Generates a presigned PUT URL for direct S3 upload.
     *
     * @param s3Key          the object key in the bucket
     * @param contentType    MIME type of the object to be uploaded
     * @param expiresInSeconds how long the presigned URL remains valid
     * @return the presigned PUT URL as a String
     */
    String generateUploadUrl(String s3Key, String contentType, int expiresInSeconds);
}
