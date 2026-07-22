package com.umc.product.storage.adapter.out.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.out.dto.StorageObjectInfo;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

@ExtendWith(MockitoExtension.class)
class S3StorageAdapterTest {

    @Mock
    S3Client s3Client;

    @Mock
    S3Presigner s3Presigner;

    @Mock
    SsmClient ssmClient;

    @Mock
    PresignedPutObjectRequest presignedPutObjectRequest;

    @Mock
    PresignedGetObjectRequest presignedGetObjectRequest;

    @Test
    @DisplayName("Presigned PUT 생성 시 요청 파일 크기를 Content-Length로 서명한다")
    void Presigned_PUT_생성_시_요청_파일_크기를_Content_Length로_서명한다() throws Exception {
        // given
        S3StorageAdapter sut = adapter();
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        given(presignedPutObjectRequest.url()).willReturn(URI.create("https://storage.example.com/upload").toURL());
        given(s3Presigner.presignPutObject(captor.capture())).willReturn(presignedPutObjectRequest);

        // when
        FileUploadInfo result = sut.generateUploadUrl("private/portfolio/file.pdf", "application/pdf", 1024L, 15L);

        // then
        assertThat(result.uploadUrl()).isEqualTo("https://storage.example.com/upload");
        assertThat(result.uploadMethod()).isEqualTo("PUT");
        assertThat(result.headers()).containsEntry("Content-Type", "application/pdf");
        assertThat(result.headers()).doesNotContainKey("Content-Length");
        assertThat(result.expiresAt()).isAfter(LocalDateTime.now());
        assertThat(captor.getValue().putObjectRequest().contentLength()).isEqualTo(1024L);
        assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("서버에서 생성한 파일 바이트를 S3 객체로 저장한다")
    void 서버에서_생성한_파일_바이트를_S3_객체로_저장한다() {
        // given
        S3StorageAdapter sut = adapter();
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // when
        sut.uploadObject("private/certificate/file.pdf", "application/pdf", new byte[]{1, 2, 3});

        // then
        verify(s3Client).putObject(captor.capture(), org.mockito.ArgumentMatchers.any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo("private/certificate/file.pdf");
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(captor.getValue().contentLength()).isEqualTo(3L);
    }

    @Test
    @DisplayName("CloudFront를 사용하지 않으면 다운로드용 Presigned GET URL을 생성한다")
    void CloudFront를_사용하지_않으면_다운로드용_Presigned_GET_URL을_생성한다() throws Exception {
        // given
        S3StorageAdapter sut = adapter();
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        given(presignedGetObjectRequest.url()).willReturn(URI.create("https://storage.example.com/download").toURL());
        given(s3Presigner.presignGetObject(captor.capture())).willReturn(presignedGetObjectRequest);

        // when
        String result = sut.generateAccessUrl("private/certificate/file.pdf", 60L);

        // then
        assertThat(result).isEqualTo("https://storage.example.com/download");
        assertThat(captor.getValue().signatureDuration()).isEqualTo(java.time.Duration.ofMinutes(60L));
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("private/certificate/file.pdf");
    }

    @Test
    @DisplayName("다운로드 URL 생성 실패 시 원인 예외를 보존한다")
    void 다운로드_URL_생성_실패_시_원인_예외를_보존한다() {
        // given
        S3StorageAdapter sut = adapter();
        RuntimeException failure = new RuntimeException("presign failed");
        given(s3Presigner.presignGetObject(org.mockito.ArgumentMatchers.any(GetObjectPresignRequest.class)))
            .willThrow(failure);

        // when & then
        assertThatThrownBy(() -> sut.generateAccessUrl("private/certificate/file.pdf", 60L))
            .isInstanceOf(StorageException.class)
            .hasCause(failure)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_URL_GENERATION_FAILED);
    }

    @Test
    @DisplayName("CloudFront가 켜져 있어도 서명 키가 없으면 Presigned GET URL로 대체한다")
    void CloudFront가_켜져_있어도_서명_키가_없으면_Presigned_GET_URL로_대체한다() throws Exception {
        // given
        S3StorageAdapter sut = adapter(new S3StorageProperties.CloudFront("cdn.example.com", true, null, null, null));
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        given(presignedGetObjectRequest.url()).willReturn(URI.create("https://storage.example.com/download").toURL());
        given(s3Presigner.presignGetObject(captor.capture())).willReturn(presignedGetObjectRequest);

        // when
        String result = sut.generateAccessUrl("private/certificate/file.pdf", 60L);

        // then
        assertThat(result).isEqualTo("https://storage.example.com/download");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("private/certificate/file.pdf");
        verify(ssmClient, never()).getParameter(org.mockito.ArgumentMatchers.any(GetParameterRequest.class));
    }

    @Test
    @DisplayName("이스케이프된 PEM private key로 CloudFront Signed URL을 생성한다")
    void 이스케이프된_PEM_private_key로_CloudFront_Signed_URL을_생성한다() throws Exception {
        // given
        String privateKey = pkcs8PrivateKeyPem().replace("\n", "\\n");
        S3StorageAdapter sut = adapter(new S3StorageProperties.CloudFront(
            "cdn.example.com",
            true,
            "K1234567890",
            privateKey,
            null
        ));

        // when
        String result = sut.generateAccessUrl("private/certificate/file.pdf", 60L);

        // then
        assertThat(result)
            .startsWith("https://cdn.example.com/private/certificate/file.pdf")
            .contains("Key-Pair-Id=K1234567890");
    }

    @Test
    @DisplayName("CloudFront 배포 도메인이 URL 형식이어도 CDN URL로 시작하는 Signed URL을 생성한다")
    void CloudFront_배포_도메인이_URL_형식이어도_CDN_URL로_시작하는_Signed_URL을_생성한다() throws Exception {
        // given
        String privateKey = pkcs8PrivateKeyPem().replace("\n", "\\n");
        S3StorageAdapter sut = adapter(new S3StorageProperties.CloudFront(
            "https://cdn.example.com/certificate/",
            true,
            "K1234567890",
            privateKey,
            null
        ));

        // when
        String result = sut.generateAccessUrl("private/certificate/file name.pdf", 60L);

        // then
        assertThat(result)
            .startsWith("https://cdn.example.com/certificate/private/certificate/file%20name.pdf")
            .contains("Key-Pair-Id=K1234567890");
    }

    @Test
    @DisplayName("SSM SecureString private key로 CloudFront Signed URL을 생성하고 값을 캐시한다")
    void SSM_SecureString_private_key로_CloudFront_Signed_URL을_생성하고_값을_캐시한다() throws Exception {
        // given
        String privateKey = pkcs8PrivateKeyPem().replace("\n", "\\n");
        S3StorageAdapter sut = adapter(new S3StorageProperties.CloudFront(
            "cdn.example.com",
            true,
            "K1234567890",
            null,
            "/umc/common/cloudfront/private-key"
        ));
        ArgumentCaptor<GetParameterRequest> captor = ArgumentCaptor.forClass(GetParameterRequest.class);
        given(ssmClient.getParameter(captor.capture())).willReturn(GetParameterResponse.builder()
            .parameter(Parameter.builder()
                .value(privateKey)
                .build())
            .build());

        // when
        String firstResult = sut.generateAccessUrl("private/certificate/file.pdf", 60L);
        String secondResult = sut.generateAccessUrl("private/certificate/file.pdf", 60L);

        // then
        assertThat(firstResult)
            .startsWith("https://cdn.example.com/private/certificate/file.pdf")
            .contains("Key-Pair-Id=K1234567890");
        assertThat(secondResult)
            .startsWith("https://cdn.example.com/private/certificate/file.pdf")
            .contains("Key-Pair-Id=K1234567890");
        assertThat(captor.getValue().name()).isEqualTo("/umc/common/cloudfront/private-key");
        assertThat(captor.getValue().withDecryption()).isTrue();
        verify(ssmClient).getParameter(org.mockito.ArgumentMatchers.any(GetParameterRequest.class));
    }

    @Test
    @DisplayName("HeadObject 결과를 S3 객체 정보로 반환한다")
    void HeadObject_결과를_S3_객체_정보로_반환한다() {
        // given
        S3StorageAdapter sut = adapter();
        given(s3Client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
            .willReturn(HeadObjectResponse.builder()
                .contentLength(1024L)
                .contentType("application/pdf")
                .build());

        // when
        Optional<StorageObjectInfo> result = sut.findObjectInfoByStorageKey("private/portfolio/file.pdf");

        // then
        assertThat(result).hasValue(StorageObjectInfo.of("private/portfolio/file.pdf", 1024L, "application/pdf"));
    }

    @Test
    @DisplayName("S3 객체가 없으면 빈 객체 정보를 반환하고 exists는 false를 반환한다")
    void S3_객체가_없으면_빈_객체_정보를_반환하고_exists는_false를_반환한다() {
        // given
        S3StorageAdapter sut = adapter();
        given(s3Client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
            .willThrow(NoSuchKeyException.builder().build());

        // when & then
        assertThat(sut.findObjectInfoByStorageKey("private/portfolio/missing.pdf")).isEmpty();
        assertThat(sut.exists("private/portfolio/missing.pdf")).isFalse();
    }

    @Test
    @DisplayName("HeadObject 조회 중 404가 아닌 S3Exception은 StorageException으로 변환한다")
    void HeadObject_조회_중_404가_아닌_S3Exception은_StorageException으로_변환한다() {
        // given
        S3StorageAdapter sut = adapter();
        given(s3Client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
            .willThrow(S3Exception.builder().statusCode(500).build());

        // when & then
        assertThatThrownBy(() -> sut.findObjectInfoByStorageKey("private/portfolio/file.pdf"))
            .isInstanceOf(StorageException.class)
            .hasCauseInstanceOf(S3Exception.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_METADATA_READ_FAILED);
    }

    @Test
    @DisplayName("파일 크기 없는 legacy 업로드 URL도 PUT 서명으로 생성한다")
    void 파일_크기_없는_업로드_URL을_생성한다() throws Exception {
        S3StorageAdapter sut = adapter();
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        given(presignedPutObjectRequest.url()).willReturn(URI.create("https://storage.example.com/upload").toURL());
        given(s3Presigner.presignPutObject(captor.capture())).willReturn(presignedPutObjectRequest);

        assertThat(sut.generateUploadUrl("key", "text/plain", 15L).uploadUrl())
            .isEqualTo("https://storage.example.com/upload");
        assertThat(captor.getValue().putObjectRequest().contentLength()).isNull();
    }

    @Test
    @DisplayName("업로드 URL 생성과 객체 저장 실패는 원인을 보존한 storage 예외로 변환한다")
    void 업로드_실패를_변환한다() {
        S3StorageAdapter sut = adapter();
        RuntimeException presignFailure = new RuntimeException("presign fail");
        RuntimeException uploadFailure = new RuntimeException("upload fail");
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willThrow(presignFailure);
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).willThrow(uploadFailure);

        assertThatThrownBy(() -> sut.generateUploadUrl("key", "text/plain", 15L))
            .isInstanceOf(StorageException.class)
            .hasCause(presignFailure)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_URL_GENERATION_FAILED);
        assertThatThrownBy(() -> sut.uploadObject("key", "text/plain", new byte[]{1}))
            .isInstanceOf(StorageException.class)
            .hasCause(uploadFailure)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("HeadObject의 S3 404도 미존재로 정규화한다")
    void S3_404를_미존재로_정규화한다() {
        S3StorageAdapter sut = adapter();
        given(s3Client.headObject(any(HeadObjectRequest.class)))
            .willThrow(S3Exception.builder().statusCode(404).build());

        assertThat(sut.findObjectInfoByStorageKey("missing")).isEmpty();
    }

    @Test
    @DisplayName("객체 삭제 요청은 bucket과 key를 전달하고 외부 실패를 변환한다")
    void 객체_삭제의_성공과_실패를_검증한다() {
        S3StorageAdapter sut = adapter();
        sut.delete("private/file.pdf");
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo("private/file.pdf");

        RuntimeException failure = new RuntimeException("delete fail");
        willThrow(failure).given(s3Client).deleteObject(any(DeleteObjectRequest.class));
        assertThatThrownBy(() -> sut.delete("private/fail.pdf"))
            .isInstanceOf(StorageException.class)
            .hasCause(failure)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_DELETE_FAILED);
    }

    @Test
    @DisplayName("헤더 없는 PKCS8와 PKCS1 PEM private key를 모두 파싱한다")
    void CloudFront_private_key_형식을_파싱한다() throws Exception {
        String rawPkcs8 = Base64.getMimeEncoder().encodeToString(keyPair().getPrivate().getEncoded());
        S3StorageAdapter rawAdapter = adapter(new S3StorageProperties.CloudFront(
            "cdn.example.com", true, "K1234567890", rawPkcs8, null));
        S3StorageAdapter pkcs1Adapter = adapter(new S3StorageProperties.CloudFront(
            "cdn.example.com", true, "K1234567890", pkcs1PrivateKeyPem(), null));

        assertThat(rawAdapter.generateAccessUrl("private/file.pdf", 1L)).contains("Key-Pair-Id=K1234567890");
        assertThat(pkcs1Adapter.generateAccessUrl("private/file.pdf", 1L)).contains("Key-Pair-Id=K1234567890");
    }

    @Test
    @DisplayName("잘못된 CDN 도메인·private key와 미지원 PEM 객체는 서명 실패로 정규화한다")
    void CloudFront_설정과_key_파싱_실패를_정규화한다() throws Exception {
        S3StorageAdapter invalidDomain = adapter(new S3StorageProperties.CloudFront(
            "https:///missing-host", true, "key", pkcs8PrivateKeyPem(), null));
        S3StorageAdapter invalidKey = adapter(new S3StorageProperties.CloudFront(
            "cdn.example.com", true, "key", "not-base64", null));
        S3StorageAdapter unsupportedPem = adapter(new S3StorageProperties.CloudFront(
            "cdn.example.com", true, "key", publicKeyPem(), null));

        assertCdnSigningFailed(() -> invalidDomain.generateAccessUrl("file", 1L));
        assertCdnSigningFailed(() -> invalidKey.generateAccessUrl("file", 1L));
        assertCdnSigningFailed(() -> unsupportedPem.generateAccessUrl("file", 1L));
    }

    @Test
    @DisplayName("SSM private key가 비거나 조회가 실패하면 서명 실패로 변환한다")
    void SSM_private_key_실패를_변환한다() {
        S3StorageProperties.CloudFront cloudFront = new S3StorageProperties.CloudFront(
            "cdn.example.com", true, "key", null, "/key");
        S3StorageAdapter sut = adapter(cloudFront);
        given(ssmClient.getParameter(any(GetParameterRequest.class))).willReturn(GetParameterResponse.builder()
            .parameter(Parameter.builder().value(" ").build())
            .build());
        assertCdnSigningFailed(() -> sut.generateAccessUrl("file", 1L));

        S3StorageAdapter failed = adapter(cloudFront);
        given(ssmClient.getParameter(any(GetParameterRequest.class)))
            .willThrow(new RuntimeException("ssm fail"));
        assertCdnSigningFailed(() -> failed.generateAccessUrl("file", 1L));
    }

    private S3StorageAdapter adapter() {
        return adapter(new S3StorageProperties.CloudFront("cdn.example.com", false, null, null, null));
    }

    private S3StorageAdapter adapter(S3StorageProperties.CloudFront cloudFront) {
        return new S3StorageAdapter(
            s3Client,
            s3Presigner,
            ssmClient,
            properties(cloudFront),
            new OperationalMetrics(new SimpleMeterRegistry())
        );
    }

    private S3StorageProperties properties(S3StorageProperties.CloudFront cloudFront) {
        return new S3StorageProperties(
            "test-bucket",
            "ap-northeast-2",
            "access-key",
            "secret-key",
            cloudFront
        );
    }

    private String pkcs8PrivateKeyPem() throws Exception {
        KeyPair keyPair = keyPair();
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
            .encodeToString(keyPair.getPrivate().getEncoded());
        return """
            -----BEGIN PRIVATE KEY-----
            %s
            -----END PRIVATE KEY-----
            """.formatted(encoded);
    }

    private String pkcs1PrivateKeyPem() throws Exception {
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(keyPair().getPrivate().getEncoded());
        StringWriter output = new StringWriter();
        try (PemWriter writer = new PemWriter(output)) {
            writer.writeObject(new PemObject("RSA PRIVATE KEY", privateKeyInfo.parsePrivateKey().toASN1Primitive().getEncoded()));
        }
        return output.toString();
    }

    private String publicKeyPem() throws Exception {
        StringWriter output = new StringWriter();
        try (PemWriter writer = new PemWriter(output)) {
            writer.writeObject(new PemObject("PUBLIC KEY", keyPair().getPublic().getEncoded()));
        }
        return output.toString();
    }

    private KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private void assertCdnSigningFailed(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.CDN_SIGNING_FAILED);
    }
}
