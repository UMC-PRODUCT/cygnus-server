package com.umc.product.storage.adapter.out.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

class S3ConfigTest {

    @Test
    @DisplayName("S3와 SSM 정적 자격증명을 별도로 해석한다")
    void S3와_SSM_정적_자격증명을_별도로_해석한다() {
        // given
        S3Config sut = new S3Config();
        S3StorageProperties storageProperties = storageProperties();
        AppSsmProperties ssmProperties = new AppSsmProperties("us-east-1", "ssm-access-key", "ssm-secret-key");

        // when
        AwsCredentialsProvider s3CredentialsProvider = sut.resolveS3Credentials(storageProperties);
        AwsCredentialsProvider ssmCredentialsProvider = sut.resolveSsmCredentials(ssmProperties);

        // then
        assertThat(s3CredentialsProvider.resolveCredentials().accessKeyId()).isEqualTo("s3-access-key");
        assertThat(s3CredentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("s3-secret-key");
        assertThat(ssmCredentialsProvider.resolveCredentials().accessKeyId()).isEqualTo("ssm-access-key");
        assertThat(ssmCredentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("ssm-secret-key");
        assertThat(sut.resolveSsmRegion(storageProperties, ssmProperties)).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("SSM 정적 자격증명이 없으면 기본 자격증명 체인과 S3 리전을 사용한다")
    void SSM_정적_자격증명이_없으면_기본_자격증명_체인과_S3_리전을_사용한다() {
        // given
        S3Config sut = new S3Config();
        S3StorageProperties storageProperties = storageProperties();
        AppSsmProperties ssmProperties = new AppSsmProperties("", "", "");

        // when
        AwsCredentialsProvider ssmCredentialsProvider = sut.resolveSsmCredentials(ssmProperties);
        String ssmRegion = sut.resolveSsmRegion(storageProperties, ssmProperties);

        // then
        assertThat(ssmCredentialsProvider).isInstanceOf(DefaultCredentialsProvider.class);
        assertThat(ssmRegion).isEqualTo("ap-northeast-2");
    }

    private S3StorageProperties storageProperties() {
        return new S3StorageProperties(
            "test-bucket",
            "ap-northeast-2",
            "s3-access-key",
            "s3-secret-key",
            new S3StorageProperties.CloudFront("cdn.example.com", false, null, null, null)
        );
    }
}
