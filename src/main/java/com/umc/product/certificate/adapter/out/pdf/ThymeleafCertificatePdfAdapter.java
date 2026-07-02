package com.umc.product.certificate.adapter.out.pdf;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.umc.product.certificate.application.port.out.RenderCertificatePdfPort;
import com.umc.product.certificate.application.port.out.dto.CertificatePdfRenderCommand;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.CertificateType;
import com.umc.product.certificate.domain.exception.CertificateErrorCode;
import com.umc.product.certificate.domain.exception.CertificateException;

@Component
public class ThymeleafCertificatePdfAdapter implements RenderCertificatePdfPort {

    private static final String TEMPLATE_CONFIG_RESOURCE_PATH = "certificate/config/certificate_template.json";
    private static final String PRETENDARD_REGULAR_RESOURCE_PATH = "certificate/fonts/Pretendard-Regular.ttf";
    private static final String PRETENDARD_MEDIUM_RESOURCE_PATH = "certificate/fonts/Pretendard-Medium.ttf";
    private static final String PRETENDARD_SEMIBOLD_RESOURCE_PATH = "certificate/fonts/Pretendard-SemiBold.ttf";
    private static final double PX_TO_PT = 0.283464567;
    private static final boolean QR_CODE_ENABLED = true;
    private static final float QR_CODE_SIZE_PT = 58F;
    private static final float QR_CODE_RIGHT_OFFSET_PT = 188F;
    private static final DateTimeFormatter ISSUE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        .withLocale(Locale.KOREA)
        .withZone(ZoneId.of("Asia/Seoul"));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] render(CertificatePdfRenderCommand command) {
        try {
            return renderCoordinateTemplate(command);
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_RENDER_FAILED, e);
        }
    }

    private byte[] renderCoordinateTemplate(CertificatePdfRenderCommand command) throws Exception {
        CertificateTemplate template = Objects.requireNonNull(command.template(), "template must not be null");
        JsonNode config = loadTemplateConfig();
        Map<String, String> fieldValues = buildFieldValues(command, template);

        try (InputStream backgroundStream = getResourceInputStream(template.backgroundResourcePath());
             PDDocument document = PDDocument.load(backgroundStream)) {
            PDPage page = document.getPage(0);
            FontSet fonts = loadFonts(document);

            try (PDPageContentStream contentStream = new PDPageContentStream(
                document,
                page,
                AppendMode.APPEND,
                true,
                true
            )) {
                JsonNode layout = config.path("layouts").path(Integer.toString(template.itemCount()));
                drawShapes(contentStream, page, layout.path("shapes"));
                drawFields(contentStream, page, fonts, collectFields(config, layout), fieldValues);
                drawQrCodeIfEnabled(document, page, contentStream, command.verificationUrl());
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private JsonNode loadTemplateConfig() throws IOException {
        try (InputStream inputStream = getResourceInputStream(TEMPLATE_CONFIG_RESOURCE_PATH)) {
            return objectMapper.readTree(inputStream);
        }
    }

    private FontSet loadFonts(PDDocument document) throws IOException {
        try (InputStream regular = getResourceInputStream(PRETENDARD_REGULAR_RESOURCE_PATH);
             InputStream medium = getResourceInputStream(PRETENDARD_MEDIUM_RESOURCE_PATH);
             InputStream semiBold = getResourceInputStream(PRETENDARD_SEMIBOLD_RESOURCE_PATH)) {
            return new FontSet(
                PDType0Font.load(document, regular),
                PDType0Font.load(document, medium),
                PDType0Font.load(document, semiBold)
            );
        }
    }

    private InputStream getResourceInputStream(String resourcePath) {
        try {
            return new ClassPathResource(resourcePath).getInputStream();
        } catch (IOException e) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_RENDER_FAILED, e);
        }
    }

    private List<FieldDefinition> collectFields(JsonNode config, JsonNode layout) {
        List<FieldDefinition> fields = new ArrayList<>();
        config.path("commonFields").forEach(field -> fields.add(FieldDefinition.from(field)));
        layout.path("fields").forEach(field -> fields.add(FieldDefinition.from(field)));
        return fields;
    }

    private Map<String, String> buildFieldValues(CertificatePdfRenderCommand command, CertificateTemplate template) {
        String generationKo = command.gisuGeneration() + "기";
        String displayAwardName = firstNonBlank(command.meritTitle(), template.awardName());

        Map<String, String> values = new LinkedHashMap<>();
        values.put("award_en_subtitle", template.englishCertificateTitle());
        values.put("award_ko_subtitle", template.koreanSubtitle(command.gisuGeneration(), displayAwardName));
        values.put("award_en_title_line1", template.englishTitleLine1(command.gisuGeneration()));
        values.put("award_en_title_line2", template.englishTitleLine2());
        values.put("award_ko_title", template.koreanTitle(displayAwardName));
        values.put("award_description", firstNonBlank(
            command.meritDescription(),
            template.defaultDescription(command.gisuGeneration(), displayAwardName)
        ));
        values.put("issue_date", ISSUE_DATE_FORMATTER.format(command.issuedAt()));
        values.put("static_issuer_value", command.issuer().displayName());
        values.put("issuanceNumber", command.issuanceNumber());
        values.put("item_label_1", "성명");
        values.put("item_value_1", command.recipientName());
        values.put("item_label_2", "소속");
        values.put("item_value_2", firstNonBlank(command.recipientSchoolName(), "-"));
        values.put("item_label_3", "기수");
        values.put("item_value_3", generationKo);
        values.put("item_label_4", itemLabel4(command));
        values.put("item_value_4", itemValue4(command, displayAwardName));
        return values;
    }

    private String itemLabel4(CertificatePdfRenderCommand command) {
        return command.type() == CertificateType.PROJECT_PARTICIPATION ? "프로젝트" : "구분";
    }

    private String itemValue4(CertificatePdfRenderCommand command, String displayAwardName) {
        if (command.type() == CertificateType.PROJECT_PARTICIPATION) {
            return firstNonBlank(command.projectName(), "-");
        }
        return firstNonBlank(displayAwardName, command.type().displayName());
    }

    private void drawShapes(PDPageContentStream contentStream, PDPage page, JsonNode shapes) throws IOException {
        float pageHeight = pageHeight(page);
        for (JsonNode shape : shapes) {
            float x = pxToPt(shape.path("xPx").asDouble());
            float y = pageHeight - pxToPt(shape.path("yPx").asDouble() + shape.path("heightPx").asDouble());
            float width = pxToPt(shape.path("widthPx").asDouble());
            float height = pxToPt(shape.path("heightPx").asDouble());
            int[] color = rgb(shape.path("strokeRGB"), 185, 185, 185);
            contentStream.setStrokingColor(pdfRgb(color));
            contentStream.setLineWidth(Math.max(pxToPt(shape.path("strokeWidthPx").asDouble(1D)), 0.1F));
            if ("rect".equals(shape.path("type").asText())) {
                contentStream.addRect(x, y, width, height);
                contentStream.stroke();
            } else {
                contentStream.moveTo(x, y);
                contentStream.lineTo(x + width, y);
                contentStream.stroke();
            }
        }
    }

    private void drawFields(
        PDPageContentStream contentStream,
        PDPage page,
        FontSet fonts,
        List<FieldDefinition> fields,
        Map<String, String> fieldValues
    ) throws IOException {
        float pageHeight = pageHeight(page);
        for (FieldDefinition field : fields) {
            String text = resolveText(field, fieldValues);
            if (text.isBlank()) {
                continue;
            }
            PDType0Font font = fonts.byStyle(field.fontStyle());
            TextLayout layout = computeLayout(field, font, text);
            if (!layout.fits() && "error".equals(field.overflowBehavior())) {
                throw new CertificateException(CertificateErrorCode.CERTIFICATE_RENDER_FAILED);
            }
            drawText(contentStream, pageHeight, field, font, layout);
        }
    }

    private String resolveText(FieldDefinition field, Map<String, String> fieldValues) {
        String value = fieldValues.get(field.key());
        if (value != null) {
            return value;
        }
        return field.staticField() ? field.defaultText() : "";
    }

    private TextLayout computeLayout(FieldDefinition field, PDType0Font font, String text) throws IOException {
        float maxWidth = pxToPt(field.widthPx());
        float maxHeight = pxToPt(field.heightPx());
        float baseLetterSpacing = pxToPt(field.letterSpacingPx());
        float fontSize = field.fontSizePt();

        List<String> lines = tryFit(field, font, text, fontSize, baseLetterSpacing, maxWidth, maxHeight);
        if (lines != null) {
            return new TextLayout(lines, fontSize, baseLetterSpacing, true);
        }

        for (float shrinkSize = fontSize - 0.5F; shrinkSize >= field.minFontSizePt(); shrinkSize -= 0.5F) {
            lines = tryFit(field, font, text, shrinkSize, baseLetterSpacing, maxWidth, maxHeight);
            if (lines != null) {
                return new TextLayout(lines, shrinkSize, baseLetterSpacing, true);
            }
        }
        return new TextLayout(List.of(text), field.minFontSizePt(), baseLetterSpacing, false);
    }

    private List<String> tryFit(
        FieldDefinition field,
        PDType0Font font,
        String text,
        float fontSize,
        float letterSpacing,
        float maxWidth,
        float maxHeight
    ) throws IOException {
        float lineHeight = fontSize * field.lineHeightMultiplier();
        if (!field.allowWrap()) {
            if (textWidth(font, text, fontSize, letterSpacing) <= maxWidth && lineHeight <= maxHeight + 0.5F) {
                return List.of(text);
            }
            return null;
        }
        List<String> lines = wrapText(font, text, fontSize, letterSpacing, maxWidth, field.maxLines());
        if (lines.size() > field.maxLines() || lineHeight * lines.size() > maxHeight + 0.5F) {
            return null;
        }
        return lines;
    }

    private List<String> wrapText(
        PDType0Font font,
        String text,
        float fontSize,
        float letterSpacing,
        float maxWidth,
        int maxLines
    ) throws IOException {
        String[] tokens = text.split("\\s+");
        if (tokens.length == 0) {
            return List.of(text);
        }

        List<String> lines = new ArrayList<>();
        String current = "";
        for (String token : tokens) {
            String candidate = current.isBlank() ? token : current + " " + token;
            if (textWidth(font, candidate, fontSize, letterSpacing) <= maxWidth) {
                current = candidate;
                continue;
            }
            if (!current.isBlank()) {
                lines.add(current);
                if (lines.size() >= maxLines) {
                    return lines;
                }
            }
            current = fitToken(font, token, fontSize, letterSpacing, maxWidth, lines, maxLines);
        }
        if (!current.isBlank()) {
            lines.add(current);
        }
        return lines;
    }

    private String fitToken(
        PDType0Font font,
        String token,
        float fontSize,
        float letterSpacing,
        float maxWidth,
        List<String> lines,
        int maxLines
    ) throws IOException {
        String current = "";
        for (int offset = 0; offset < token.length(); ) {
            int codePoint = token.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            String candidate = current + character;
            if (textWidth(font, candidate, fontSize, letterSpacing) <= maxWidth) {
                current = candidate;
            } else {
                if (!current.isBlank()) {
                    lines.add(current);
                    if (lines.size() >= maxLines) {
                        return character;
                    }
                }
                current = character;
            }
            offset += Character.charCount(codePoint);
        }
        return current;
    }

    private void drawText(
        PDPageContentStream contentStream,
        float pageHeight,
        FieldDefinition field,
        PDType0Font font,
        TextLayout layout
    ) throws IOException {
        contentStream.setNonStrokingColor(pdfRgb(field.colorRgb()));
        for (int i = 0; i < layout.lines().size(); i++) {
            String line = layout.lines().get(i);
            float x = textX(field, font, line, layout.fontSizePt(), layout.letterSpacingPt());
            float lineTop = pxToPt(field.yPx()) + i * layout.fontSizePt() * field.lineHeightMultiplier();
            float y = pageHeight - lineTop - layout.fontSizePt() * 0.75F;
            drawLine(contentStream, font, layout.fontSizePt(), layout.letterSpacingPt(), x, y, line);
        }
    }

    private float textX(
        FieldDefinition field,
        PDType0Font font,
        String line,
        float fontSize,
        float letterSpacing
    ) throws IOException {
        float x = pxToPt(field.xPx());
        float fieldWidth = pxToPt(field.widthPx());
        float textWidth = textWidth(font, line, fontSize, letterSpacing);
        return switch (field.textAlignHorizontal()) {
            case "RIGHT" -> x + fieldWidth - textWidth;
            case "CENTER" -> x + (fieldWidth - textWidth) / 2F;
            default -> x;
        };
    }

    private void drawLine(
        PDPageContentStream contentStream,
        PDType0Font font,
        float fontSize,
        float letterSpacing,
        float x,
        float y,
        String line
    ) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        if (Math.abs(letterSpacing) < 0.01F) {
            contentStream.showText(line);
        } else {
            showTextWithLetterSpacing(contentStream, font, fontSize, letterSpacing, line);
        }
        contentStream.endText();
    }

    private void showTextWithLetterSpacing(
        PDPageContentStream contentStream,
        PDType0Font font,
        float fontSize,
        float letterSpacing,
        String line
    ) throws IOException {
        for (int offset = 0; offset < line.length(); ) {
            int codePoint = line.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            contentStream.showText(character);
            contentStream.newLineAtOffset(textWidth(font, character, fontSize, 0F) + letterSpacing, 0F);
            offset += Character.charCount(codePoint);
        }
    }

    private void drawQrCodeIfEnabled(
        PDDocument document,
        PDPage page,
        PDPageContentStream contentStream,
        String verificationUrl
    ) throws Exception {
        if (!QR_CODE_ENABLED) {
            return;
        }
        BufferedImage qrImage = createQrCodeImage(verificationUrl, 180);
        PDImageXObject imageObject = LosslessFactory.createFromImage(document, qrImage);
        PDRectangle mediaBox = page.getMediaBox();
        contentStream.drawImage(
            imageObject,
            mediaBox.getWidth() - QR_CODE_SIZE_PT - QR_CODE_RIGHT_OFFSET_PT,
            36F,
            QR_CODE_SIZE_PT,
            QR_CODE_SIZE_PT
        );
    }

    private BufferedImage createQrCodeImage(String verificationUrl, int size) throws Exception {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(
            verificationUrl,
            BarcodeFormat.QR_CODE,
            size,
            size,
            Map.of(
                EncodeHintType.CHARACTER_SET, "UTF-8",
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1
            )
        );
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private float textWidth(PDType0Font font, String text, float fontSize, float letterSpacing) throws IOException {
        return font.getStringWidth(text) / 1000F * fontSize
            + letterSpacing * Math.max(0, text.codePointCount(0, text.length()) - 1);
    }

    private float pageHeight(PDPage page) {
        return page.getMediaBox().getHeight();
    }

    private float pxToPt(double px) {
        return (float) (px * PX_TO_PT);
    }

    private int[] rgb(JsonNode node, int defaultR, int defaultG, int defaultB) {
        if (!node.isArray() || node.size() < 3) {
            return new int[] {defaultR, defaultG, defaultB};
        }
        return new int[] {node.get(0).asInt(), node.get(1).asInt(), node.get(2).asInt()};
    }

    private PDColor pdfRgb(int[] rgb) {
        return new PDColor(
            new float[] {rgb[0] / 255F, rgb[1] / 255F, rgb[2] / 255F},
            PDDeviceRGB.INSTANCE
        );
    }

    private String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return fallback;
    }

    private record FontSet(
        PDType0Font regular,
        PDType0Font medium,
        PDType0Font semiBold
    ) {

        private PDType0Font byStyle(String fontStyle) {
            return switch (fontStyle) {
                case "SemiBold" -> semiBold;
                case "Medium" -> medium;
                default -> regular;
            };
        }
    }

    private record FieldDefinition(
        String key,
        double xPx,
        double yPx,
        double widthPx,
        double heightPx,
        String fontStyle,
        float fontSizePt,
        float lineHeightMultiplier,
        double letterSpacingPx,
        String textAlignHorizontal,
        int maxLines,
        boolean allowWrap,
        float minFontSizePt,
        String overflowBehavior,
        int[] colorRgb,
        String defaultText,
        boolean staticField
    ) {

        private static FieldDefinition from(JsonNode node) {
            return new FieldDefinition(
                node.path("key").asText(),
                node.path("xPx").asDouble(),
                node.path("yPx").asDouble(),
                node.path("widthPx").asDouble(),
                node.path("heightPx").asDouble(),
                node.path("fontStyle").asText("Regular"),
                (float) node.path("fontSizePt").asDouble(10D),
                (float) node.path("lineHeightMultiplier").asDouble(1.2D),
                node.path("letterSpacingPx").asDouble(0D),
                node.path("textAlignHorizontal").asText("LEFT"),
                node.path("maxLines").asInt(1),
                node.path("allowWrap").asBoolean(false),
                (float) node.path("minFontSizePt").asDouble(6D),
                node.path("overflowBehavior").asText("error"),
                rgb(node.path("colorRGB"), 0, 0, 0),
                node.path("defaultText").asText(""),
                node.path("static").asBoolean(false)
            );
        }

        private static int[] rgb(JsonNode node, int defaultR, int defaultG, int defaultB) {
            if (!node.isArray() || node.size() < 3) {
                return new int[] {defaultR, defaultG, defaultB};
            }
            return new int[] {node.get(0).asInt(), node.get(1).asInt(), node.get(2).asInt()};
        }
    }

    private record TextLayout(
        List<String> lines,
        float fontSizePt,
        float letterSpacingPt,
        boolean fits
    ) {
    }
}
