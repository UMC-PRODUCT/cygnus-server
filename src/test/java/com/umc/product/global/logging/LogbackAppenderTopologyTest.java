package com.umc.product.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 마스킹을 우회하는 배선이 생기지 않는지 감시한다. {@code root}가 참조하는 어펜더는
 * {@link SanitizingAppender} 뿐이어야 하고, 이 구조가 깨지면 로그가 정제 없이 나가거나 사라지는데
 * 둘 다 오류 없이 조용히 일어난다.
 *
 * <p>logback 을 기동하지 않고 XML 만 검사한다. {@code <springProfile>}을 처리하는
 * {@code SpringBootJoranConfigurator}는 package-private 이고, {@code LogbackLoggingSystem}으로 우회하면
 * 전역 {@code LoggerContext}를 건드려 다른 테스트에 영향을 준다.
 */
class LogbackAppenderTopologyTest {

    private static final String CONFIG = "/logback-spring.xml";
    private static final String SANITIZING_APPENDER = SanitizingAppender.class.getName();

    private static Document config;

    @BeforeAll
    static void parseConfig() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try (InputStream stream = LogbackAppenderTopologyTest.class.getResourceAsStream(CONFIG)) {
            assertThat(stream).as(CONFIG + " 를 찾을 수 없다").isNotNull();
            config = factory.newDocumentBuilder().parse(stream);
        }
    }

    @Test
    @DisplayName("모든 프로파일의 root 는 정제 어펜더만 참조한다")
    void root_는_정제_어펜더만_참조한다() {
        List<Element> profiles = elementsByTag(config.getDocumentElement(), "springProfile");
        assertThat(profiles).as("springProfile 이 하나도 없다").isNotEmpty();

        for (Element profile : profiles) {
            String name = profile.getAttribute("name");
            List<Element> roots = elementsByTag(profile, "root");
            assertThat(roots).as("%s 프로파일의 root", name).hasSize(1);

            List<String> refs = appenderRefs(roots.getFirst());
            assertThat(refs)
                .as("%s 프로파일의 root 는 정제 어펜더 하나만 참조해야 한다", name)
                .hasSize(1);

            String referenced = refs.getFirst();
            assertThat(appenderClass(profile, referenced))
                .as("%s 프로파일의 root 가 참조하는 %s", name, referenced)
                .isEqualTo(SANITIZING_APPENDER);
        }
    }

    @Test
    @DisplayName("정제 어펜더 아래에 실제 출력 어펜더가 붙어 있다")
    void 정제_어펜더가_출력_어펜더를_감싼다() {
        for (Element profile : elementsByTag(config.getDocumentElement(), "springProfile")) {
            String name = profile.getAttribute("name");

            for (Element appender : elementsByTag(profile, "appender")) {
                if (!SANITIZING_APPENDER.equals(appender.getAttribute("class"))) {
                    continue;
                }
                List<String> delegates = appenderRefs(appender);
                assertThat(delegates)
                    .as("%s 프로파일의 정제 어펜더에 하위 어펜더가 없어 로그가 유실된다", name)
                    .isNotEmpty();
                for (String delegate : delegates) {
                    assertThatCode(() -> topLevelAppender(delegate))
                        .as("%s 프로파일이 참조하는 %s", name, delegate)
                        .doesNotThrowAnyException();
                }
            }
        }
    }

    private Element topLevelAppender(String name) {
        return directChildren(config.getDocumentElement(), "appender").stream()
            .filter(appender -> name.equals(appender.getAttribute("name")))
            .findFirst()
            .orElseThrow(() -> new AssertionError(name + " 어펜더 정의를 찾을 수 없다"));
    }

    /** 어펜더는 프로파일 안에도, 최상위에도 정의될 수 있다. */
    private String appenderClass(Element profile, String name) {
        return elementsByTag(profile, "appender").stream()
            .filter(appender -> name.equals(appender.getAttribute("name")))
            .findFirst()
            .orElseGet(() -> topLevelAppender(name))
            .getAttribute("class");
    }

    private List<String> appenderRefs(Element parent) {
        return directChildren(parent, "appender-ref").stream()
            .map(ref -> ref.getAttribute("ref"))
            .toList();
    }

    private List<Element> elementsByTag(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        List<Element> elements = new ArrayList<>(nodes.getLength());
        for (int index = 0; index < nodes.getLength(); index++) {
            elements.add((Element) nodes.item(index));
        }
        return elements;
    }

    private List<Element> directChildren(Element parent, String tag) {
        List<Element> children = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node.getNodeType() == Node.ELEMENT_NODE && tag.equals(node.getNodeName())) {
                children.add((Element) node);
            }
        }
        return children;
    }
}
