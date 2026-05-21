-- 💡 기존에 혹시 생성되다 만 찌꺼기 테이블이 있다면 지우고 새로 만듭니다.
DROP TABLE IF EXISTS chat;

CREATE TABLE chat
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(255),

    content    TEXT NOT NULL,
    type       varchar(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성 (일부 인메모리 DB 호환성을 위해 별도 문장으로 분리)
CREATE INDEX idx_user_id ON chat (user_id);