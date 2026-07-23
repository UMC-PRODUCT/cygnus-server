# Chat GraphQL IDL

Chat은 engine resource인 `ChatRoom`과 `ChatMessage`를 제공한다. 전체 room 소유권은 Chat이 아니라
각 consumer domain에 있으므로 `chatRooms(ids:)`는 consumer가 가진 room ID 집합만 받는다.

메시지 실시간 전송·read event는 WebSocket transport를 유지한다. GraphQL은 room snapshot과
cursor 기반 message history만 조회한다.
