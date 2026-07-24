package com.umc.product.support;

import java.nio.file.Path;

import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

public final class PostgisTestImageResolver {

    private static final String TEST_POSTGIS_IMAGE_PROPERTY = "umc.test.postgis.image";
    private static final String TEST_POSTGIS_IMAGE_ENV = "UMC_TEST_POSTGIS_IMAGE";
    private static final String OFFICIAL_POSTGIS_IMAGE = "postgis/postgis:18-3.6";
    private static final String LOCAL_ARM64_POSTGIS_IMAGE = "umc-product-postgis-test:18.2-postgis";
    private static final Path ARM64_POSTGIS_DOCKERFILE = Path.of("docker/test/postgis/Dockerfile");

    private PostgisTestImageResolver() {
    }

    public static DockerImageName resolve() {
        String configuredImage = configuredPostgisImage();
        if (configuredImage != null) {
            return postgresCompatibleImage(configuredImage);
        }
        if (isArm64()) {
            return buildArm64PostgisImage();
        }
        return postgresCompatibleImage(OFFICIAL_POSTGIS_IMAGE);
    }

    private static String configuredPostgisImage() {
        String image = System.getProperty(TEST_POSTGIS_IMAGE_PROPERTY);
        if (image == null || image.isBlank()) {
            image = System.getenv(TEST_POSTGIS_IMAGE_ENV);
        }
        return image == null || image.isBlank() ? null : image.trim();
    }

    private static boolean isArm64() {
        String osArch = System.getProperty("os.arch");
        return "aarch64".equals(osArch) || "arm64".equals(osArch);
    }

    private static DockerImageName buildArm64PostgisImage() {
        try {
            String imageName = new ImageFromDockerfile(LOCAL_ARM64_POSTGIS_IMAGE, false)
                .withDockerfile(ARM64_POSTGIS_DOCKERFILE)
                .get();
            return postgresCompatibleImage(imageName);
        } catch (RuntimeException e) {
            throw new IllegalStateException("arm64 테스트 환경용 PostGIS 이미지를 빌드하지 못했습니다.", e);
        }
    }

    private static DockerImageName postgresCompatibleImage(String imageName) {
        return DockerImageName.parse(imageName).asCompatibleSubstituteFor("postgres");
    }
}
