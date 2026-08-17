package com.umc.product.storage.adapter.out.s3;

import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.storage.application.port.out.dto.StorageObjectInfo;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

/**
 * AWS S3 + CloudFront 기반 스토리지 어댑터
 *
 * <p>업로드는 S3 Presigned URL을 사용하고,
 * 다운로드는 CloudFront Signed URL을 사용합니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
@RequiredArgsConstructor
public class S3StorageAdapter implements StoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final SsmClient ssmClient;
    private final S3StorageProperties properties;
    private final OperationalMetrics operationalMetrics;

    private volatile String cachedCloudFrontPrivateKey;

    @Value("${spring.profiles.active:default}")
    private String springProfile;

    @Override
    public FileUploadInfo generateUploadUrl(
        String storageKey,
        String contentType,
        long durationMinutes
    ) {
        return generateUploadUrl(storageKey, contentType, null, durationMinutes);
    }

    @Override
    public FileUploadInfo generateUploadUrl(
        String storageKey,
        String contentType,
        long fileSize,
        long durationMinutes
    ) {
        return generateUploadUrl(storageKey, contentType, Long.valueOf(fileSize), durationMinutes);
    }

    private FileUploadInfo generateUploadUrl(
        String storageKey,
        String contentType,
        Long fileSize,
        long durationMinutes
    ) {
        long startNanos = System.nanoTime();
        try {
            PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(storageKey)
                .contentType(contentType);

            if (fileSize != null) {
                putObjectRequestBuilder.contentLength(fileSize);
            }

            PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(durationMinutes))
                .putObjectRequest(putObjectRequest)
                .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            URL signedUrl = presignedRequest.url();

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", contentType);

            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(durationMinutes);

            log.debug("S3 업로드 URL 생성: storageKey={}", storageKey);
            recordStorageMetric("CREATE_UPLOAD_URL", "success", startNanos);

            return new FileUploadInfo(
                null,  // fileId는 Service에서 설정
                signedUrl.toString(),
                "PUT",
                headers,
                expiresAt
            );
        } catch (Exception e) {
            recordStorageMetric("CREATE_UPLOAD_URL", "failure", startNanos);
            log.error("S3 업로드 URL 생성 실패: storageKey={}", storageKey, e);
            throw new StorageException(StorageErrorCode.STORAGE_URL_GENERATION_FAILED, e);
        }
    }

    @Override
    public String generateAccessUrl(String storageKey, long durationMinutes) {
        if (isCloudFrontSigningConfigured()) {
            return generateCloudFrontSignedUrl(storageKey, durationMinutes);
        }
        if (properties.cloudfront().enabled()) {
            log.warn("CloudFront가 활성화되었지만 서명 설정이 누락되어 S3 Presigned GET URL로 대체합니다.");
        }
        return generateS3DownloadUrl(storageKey, durationMinutes);
    }

    @Override
    public void uploadObject(String storageKey, String contentType, byte[] content) {
        long startNanos = System.nanoTime();
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(storageKey)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
            recordStorageMetric("UPLOAD_OBJECT", "success", startNanos);
        } catch (Exception e) {
            recordStorageMetric("UPLOAD_OBJECT", "failure", startNanos);
            log.error("S3 객체 저장 실패: storageKey={}", storageKey, e);
            throw new StorageException(StorageErrorCode.STORAGE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return findObjectInfoByStorageKey(storageKey).isPresent();
    }

    @Override
    public Optional<StorageObjectInfo> findObjectInfoByStorageKey(String storageKey) {
        long startNanos = System.nanoTime();
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(storageKey)
                .build();
            HeadObjectResponse response = s3Client.headObject(headRequest);
            recordStorageMetric("GET_OBJECT_METADATA", "success", startNanos);
            return Optional.of(StorageObjectInfo.of(
                storageKey,
                response.contentLength(),
                response.contentType()
            ));
        } catch (NoSuchKeyException e) {
            recordStorageMetric("GET_OBJECT_METADATA", "not_found", startNanos);
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                recordStorageMetric("GET_OBJECT_METADATA", "not_found", startNanos);
                return Optional.empty();
            }
            recordStorageMetric("GET_OBJECT_METADATA", "failure", startNanos);
            log.error("S3 객체 정보 조회 실패: storageKey={}", storageKey, e);
            throw new StorageException(StorageErrorCode.STORAGE_METADATA_READ_FAILED, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        long startNanos = System.nanoTime();
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(storageKey)
                .build();

            s3Client.deleteObject(deleteRequest);
            log.info("S3 파일을 삭제했습니다: storageKey={}", storageKey);
            recordStorageMetric("DELETE_OBJECT", "success", startNanos);
        } catch (Exception e) {
            recordStorageMetric("DELETE_OBJECT", "failure", startNanos);
            log.error("S3 파일 삭제 실패: storageKey={}", storageKey, e);
            throw new StorageException(StorageErrorCode.STORAGE_DELETE_FAILED, e);
        }
    }

    // ==================== PRIVATE METHODS ====================

    private String generateCloudFrontSignedUrl(String storageKey, long durationMinutes) {
        long startNanos = System.nanoTime();
        try {
            S3StorageProperties.CloudFront cloudfront = properties.cloudfront();

            Instant expirationTime = Instant.now().plusSeconds(durationMinutes * 60);

            String encodedKey = encodeStorageKey(storageKey);

            String resourceUrl = buildCloudFrontResourceUrl(cloudfront.distributionDomain(), encodedKey);

            CloudFrontUtilities cloudFrontUtilities = CloudFrontUtilities.create();

            PrivateKey privateKey = parsePrivateKey(resolveCloudFrontPrivateKey(cloudfront));

            CannedSignerRequest signerRequest = CannedSignerRequest.builder()
                .resourceUrl(resourceUrl)
                .privateKey(privateKey)
                .keyPairId(cloudfront.keyPairId())
                .expirationDate(expirationTime)
                .build();

            SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCannedPolicy(signerRequest);

            log.debug("CloudFront Signed URL을 생성했습니다: storageKey={}", storageKey);
            recordStorageMetric("CREATE_DOWNLOAD_URL", "success", startNanos);

            return signedUrl.url();
        } catch (StorageException e) {
            recordStorageMetric("CREATE_DOWNLOAD_URL", "failure", startNanos);
            throw e;
        } catch (Exception e) {
            recordStorageMetric("CREATE_DOWNLOAD_URL", "failure", startNanos);
            log.error("CloudFront Signed URL 생성 실패: storageKey={}", storageKey, e);
            throw new StorageException(StorageErrorCode.CDN_SIGNING_FAILED, e);
        }
    }

    private String generateS3DownloadUrl(String storageKey, long durationMinutes) {
        long startNanos = System.nanoTime();
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(storageKey)
                .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(durationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();
            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            recordStorageMetric("CREATE_DOWNLOAD_URL", "success", startNanos);
            return url;
        } catch (Exception e) {
            recordStorageMetric("CREATE_DOWNLOAD_URL", "failure", startNanos);
            log.error("S3 다운로드 URL 생성 실패: storageKey={}", storageKey, e);
            throw new StorageException(StorageErrorCode.STORAGE_URL_GENERATION_FAILED, e);
        }
    }

    private void recordStorageMetric(String operation, String result, long startNanos) {
        operationalMetrics.recordExternalCall(
            "STORAGE",
            operation,
            result,
            Duration.ofNanos(System.nanoTime() - startNanos)
        );
    }

    /**
     * PEM 형식의 Private Key를 파싱합니다.
     */
    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        String normalizedPrivateKey = normalizePrivateKey(privateKeyPem);
        if (normalizedPrivateKey.contains("-----BEGIN")) {
            try (PEMParser pemParser = new PEMParser(new StringReader(normalizedPrivateKey))) {
                Object pemObject = pemParser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                if (pemObject instanceof PEMKeyPair pemKeyPair) {
                    return converter.getKeyPair(pemKeyPair).getPrivate();
                }
                if (pemObject instanceof PrivateKeyInfo privateKeyInfo) {
                    return converter.getPrivateKey(privateKeyInfo);
                }
                throw new IllegalArgumentException("지원하지 않는 CloudFront private key 형식입니다.");
            }
        }

        byte[] keyBytes = Base64.getMimeDecoder().decode(normalizedPrivateKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return keyFactory.generatePrivate(keySpec);
    }

    private boolean isCloudFrontSigningConfigured() {
        S3StorageProperties.CloudFront cloudfront = properties.cloudfront();
        return cloudfront.enabled()
            && StringUtils.hasText(cloudfront.distributionDomain())
            && StringUtils.hasText(cloudfront.keyPairId())
            && (StringUtils.hasText(cloudfront.privateKey())
                || StringUtils.hasText(cloudfront.privateKeyParameterName()));
    }

    private String resolveCloudFrontPrivateKey(S3StorageProperties.CloudFront cloudfront) {
        if (StringUtils.hasText(cloudfront.privateKey())) {
            return cloudfront.privateKey();
        }

        String cachedPrivateKey = cachedCloudFrontPrivateKey;
        if (StringUtils.hasText(cachedPrivateKey)) {
            return cachedPrivateKey;
        }

        try {
            String parameterName = cloudfront.privateKeyParameterName();
            String privateKey = ssmClient.getParameter(GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build())
                .parameter()
                .value();
            if (!StringUtils.hasText(privateKey)) {
                throw new StorageException(StorageErrorCode.CDN_SIGNING_FAILED);
            }
            cachedCloudFrontPrivateKey = privateKey;
            return privateKey;
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("CloudFront private key SSM 조회 실패: parameterName={}", cloudfront.privateKeyParameterName(), e);
            throw new StorageException(StorageErrorCode.CDN_SIGNING_FAILED, e);
        }
    }

    private String normalizePrivateKey(String privateKeyPem) {
        return privateKeyPem
            .trim()
            .replace("\\n", "\n")
            .replaceAll("\\A[\"']|[\"']\\z", "");
    }

    private String encodeStorageKey(String storageKey) {

        return UriUtils.encodePath(storageKey, StandardCharsets.UTF_8);
    }

    private String buildCloudFrontResourceUrl(String distributionDomain, String encodedKey) {
        return normalizeCloudFrontBaseUrl(distributionDomain) + "/" + encodedKey;
    }

    private String normalizeCloudFrontBaseUrl(String distributionDomain) {
        String trimmedDomain = distributionDomain.trim();
        String url = hasScheme(trimmedDomain) ? trimmedDomain : "https://" + trimmedDomain;
        URI uri = URI.create(url);
        if (!StringUtils.hasText(uri.getHost())) {
            throw new StorageException(StorageErrorCode.CDN_SIGNING_FAILED);
        }

        String baseUrl = uri.getScheme() + "://" + uri.getRawAuthority();
        if (StringUtils.hasText(uri.getRawPath())) {
            baseUrl += uri.getRawPath();
        }
        return baseUrl.replaceAll("/+$", "");
    }

    private boolean hasScheme(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://");
    }

    private String parseSpringProfileToCloudFrontPath() {
        return switch (springProfile) {
            case "prod" -> "prod";
            // dev → alpha 프로필 rename 이후에도 기존 S3/CDN 객체는 dev/ 경로에 있으므로 저장 경로는 dev 를 유지한다.
            case "dev", "alpha" -> "dev";
            case "local" -> "local";
            default -> throw new StorageException(StorageErrorCode.INVALID_SPRING_PROFILE);
        };
    }
}
