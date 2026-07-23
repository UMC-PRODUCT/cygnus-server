# Storage GraphQL IDL

Storage GraphQL은 file metadata를 임의 조회하는 API를 제공하지 않는다. upload 준비, 외부 storage
업로드 확정, 소유 파일 삭제 lifecycle만 제공한다.

실제 binary는 GraphQL multipart나 base64로 전달하지 않고 `FileUpload.uploadUrl`에 직접 전송한다.
다른 도메인은 저장된 file ID를 자신의 resource field로 소유하고 Storage batch port로 URL을 해석한다.
