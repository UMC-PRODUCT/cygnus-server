package com.umc.product.test.adapter.in.web;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.umc.product.certificate.application.port.in.query.PreviewCertificatePdfUseCase;
import com.umc.product.certificate.application.port.in.query.dto.CertificatePdfPreviewInfo;
import com.umc.product.global.security.annotation.Public;
import com.umc.product.test.adapter.in.web.dto.CertificatePdfPreviewRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/test/certificates")
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.test-api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Public
@Tag(name = "Test | 인증서 PDF", description = "개발 환경에서 인증서 PDF 템플릿을 미리보기합니다.")
public class CertificatePdfTestController {

    private final PreviewCertificatePdfUseCase previewCertificatePdfUseCase;

    @GetMapping(value = "/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(operationId = "TEST-CERTIFICATE-001", summary = "인증서 PDF 템플릿 미리보기")
    public ResponseEntity<byte[]> preview(@Valid @ModelAttribute CertificatePdfPreviewRequest request) {
        CertificatePdfPreviewInfo previewInfo = previewCertificatePdfUseCase.preview(
            request.toQuery(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString())
        );

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(previewInfo.fileName()))
            .body(previewInfo.content());
    }

    private String contentDisposition(String fileName) {
        return ContentDisposition.inline()
            .filename(fileName, StandardCharsets.UTF_8)
            .build()
            .toString();
    }
}
