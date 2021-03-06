CREATE TABLE BORDER (
  ID               BIGINT NOT NULL AUTO_INCREMENT,
  TYPE             VARCHAR(255),
  ID_PROVINCE_FROM BIGINT,
  ID_PROVINCE_TO   BIGINT,
  PRIMARY KEY (ID)
);

ALTER TABLE BORDER
ADD INDEX FK_BORDER_PROVINCE_FROM (ID_PROVINCE_FROM),
ADD CONSTRAINT FK_BORDER_PROVINCE_FROM
FOREIGN KEY (ID_PROVINCE_FROM)
REFERENCES PROVINCE (ID);

ALTER TABLE BORDER
ADD INDEX FK_BORDER_PROVINCE_TO (ID_PROVINCE_TO),
ADD CONSTRAINT FK_BORDER_PROVINCE_TO
FOREIGN KEY (ID_PROVINCE_TO)
REFERENCES PROVINCE (ID);