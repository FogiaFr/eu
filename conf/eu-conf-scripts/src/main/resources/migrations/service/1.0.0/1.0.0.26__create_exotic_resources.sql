CREATE TABLE COLONY (
  ID         BIGINT NOT NULL AUTO_INCREMENT,
  ID_COUNTER    BIGINT,
  R_REGION  VARCHAR(255),
  COLONY  BIT,
  LEVEL    INTEGER,
  PRIMARY KEY (ID)
);

ALTER TABLE COLONY
ADD INDEX FK_COLONY_COUNTER (ID_COUNTER),
ADD CONSTRAINT FK_COLONY_COUNTER
FOREIGN KEY (ID_COUNTER)
REFERENCES COUNTER (ID);

CREATE TABLE EXOTIC_RESOURCES (
  ID         BIGINT NOT NULL AUTO_INCREMENT,
  ID_COLONY    BIGINT,
  RESOURCE  VARCHAR(255),
  NUMBER    INTEGER,
  PRIMARY KEY (ID)
);

ALTER TABLE EXOTIC_RESOURCES
ADD INDEX FK_EXOTIC_RESOURCES_COLONY (ID_COLONY),
ADD CONSTRAINT FK_EXOTIC_RESOURCES_COLONY
FOREIGN KEY (ID_COLONY)
REFERENCES COLONY (ID);

ALTER TABLE T_MNU CHANGE COLUMN COUNTRY R_COUNTRY VARCHAR(255);

ALTER TABLE TRADE_FLEET CHANGE COLUMN COUNTRY R_COUNTRY VARCHAR(255);