package com.umc.product.form.domain;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 객관식 응답에서 사용자가 '선택한 하나의 보기'를 나타낸다.
 * <p>
 * Answer <-> AnswerChoice 는 1:N 관계지만
 * AnswerChoice 측에서 {@link #answer} 를 {@link jakarta.persistence.ManyToOne} 으로만 참조한다.
 *
 * <h3>왜 별도 엔티티로 분리했는가</h3>
 * <ul>
 *   <li>주관식(SHORT_TEXT, LONG_TEXT) -> {@link Answer#getTextValue()} 사용, AnswerChoice 0건.</li>
 *   <li>RADIO / DROPDOWN -> AnswerChoice 1건.</li>
 *   <li>CHECKBOX -> 선택한 보기 개수만큼 AnswerChoice N건.</li>
 *   <li>SCHEDULE / FILE / PORTFOLIO -> AnswerChoice 0건 (값은 Answer 쪽에 저장).</li>
 * </ul>
 * 즉 CHECKBOX 처럼 한 답변에 보기가 여러 개 붙을 수 있는 경우를,
 * Answer 한 줄에 모두 넣는 대신, 보기 하나당 한 줄로 풀어 저장하기 위한 자식 테이블이다.
 *
 * <h3>왜 Answer 테이블의 Array 컬럼이 아니라 별도 테이블로 분리했는가</h3>
 * <ul>
 *   <li><b>FK 무결성</b> — {@link QuestionOption} 에 FK 를 걸어 옵션 삭제 시 자동으로 끊어지게 처리할 수 있다.
 *       Array 컬럼이었으면 옵션이 삭제돼도 Array 안의 옵션 ID 가 그대로 남아 dangling reference 가 된다.</li>
 *   <li><b>옵션별 집계 쿼리 단순</b> — {@code GROUP BY question_option_id} 한 줄로 끝.
 *       Array 였으면 매번 {@code unnest()} 를 거쳐야 해서 인덱스 활용도 떨어지고 쿼리도 복잡해진다.</li>
 *   <li><b>응답 시점 텍스트 스냅샷</b> — {@link #answeredAsContent} 에 응답 당시의 보기 텍스트를 같이 저장한다.
 *       Array 로 가면 ID 배열 + 텍스트 배열 두 개를 길이 맞춰 동기화해야 해서 한쪽만 어긋나면 데이터가 깨진다.</li>
 * </ul>
 *
 * <h3>{@link #answeredAsContent} 가 따로 있는 이유</h3>
 * 응답 시점의 보기 텍스트를 스냅샷 으로 보관한다. 이후 {@link QuestionOption#getContent()} 가 수정/삭제되더라도 응답이 가리킨 그 시점의 의미가 보존된다.
 * {@link #questionOption} 은 {@code ON DELETE SET NULL} 이라 원본 보기가 삭제되면 끊어지지만 answeredAsContent 는 남는다.
 */
@Entity
@Table(name = "answer_choice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerChoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    // 길이 변경 시 QuestionOption.content도 함께 변경
    @Column(name = "answered_as_content", nullable = false, length = 500)
    private String answeredAsContent;

    // 사용자가 선택한 '보기'의 ID를 참조
    // TODO: repository에 JPQL 만들기
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_option_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private QuestionOption questionOption;

    public static AnswerChoice create(Answer answer, QuestionOption questionOption) {
        return new AnswerChoice(answer, questionOption);
    }

    public AnswerChoice(Answer answer, QuestionOption questionOption) {
        this.answer = answer;
        this.questionOption = questionOption;
        this.answeredAsContent = questionOption.getContent();
    }
}
