-- V16 用户头像：存附件 ID（经 /attachments/{id}/download-url 取限时图片地址），可空。
ALTER TABLE sys_user ADD COLUMN avatar VARCHAR(64) NULL COMMENT '头像附件ID' AFTER name;
