import Handlebars from "handlebars";

const verificationTemplate = Handlebars.compile(`
<!doctype html>
<html lang="ko">
  <body>
    <h1>UMC 이메일 인증</h1>
    <p>아래 인증 코드를 입력해 주세요.</p>
    <strong>{{verificationCode}}</strong>
  </body>
</html>
`);

const recruitingTemplate = Handlebars.compile(`
<!doctype html>
<html lang="ko">
  <body>
    <p>{{applicantName}}님, UMC 면접 가능 일정을 제출해 주세요.</p>
    <p>제출 폼 ID: {{availabilityFormId}}</p>
    <p>{{contactText}}</p>
  </body>
</html>
`);

export function renderVerificationEmail(verificationCode: string): string {
  return verificationTemplate({ verificationCode });
}

export function renderRecruitingEmail(input: {
  applicantName: string;
  availabilityFormId: number;
  contactText: string;
}): string {
  return recruitingTemplate(input);
}
