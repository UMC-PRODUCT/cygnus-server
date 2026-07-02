package com.umc.product.storage.adapter.out.s3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3StorageProperties properties) {
        return S3Client.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(resolveCredentials(properties))
            .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3StorageProperties properties) {
        return S3Presigner.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(resolveCredentials(properties))
            .build();
    }

    @Bean
    public SsmClient ssmClient(S3StorageProperties properties) {
        return SsmClient.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(resolveCredentials(properties))
            .build();
    }

    private AwsCredentialsProvider resolveCredentials(S3StorageProperties properties) {
        if (properties.accessKeyId() != null && !properties.accessKeyId().isBlank()
            && properties.secretAccessKey() != null && !properties.secretAccessKey().isBlank()) {
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())
            );
        }

        return DefaultCredentialsProvider.builder().build();
    }
}
