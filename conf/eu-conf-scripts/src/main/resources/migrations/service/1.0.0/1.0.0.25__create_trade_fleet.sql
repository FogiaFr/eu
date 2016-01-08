CREATE TABLE TRADE_FLEET (
  ID         BIGINT NOT NULL AUTO_INCREMENT,
  COUNTRY    VARCHAR(255),
  R_PROVINCE    VARCHAR(255),
  LEVEL    INTEGER,
  ID_GAME BIGINT,
  PRIMARY KEY (ID)
);

ALTER TABLE TRADE_FLEET
ADD INDEX FK_TRADE_FLEET_GAME (ID_GAME),
ADD CONSTRAINT FK_TRADE_FLEET_GAME
FOREIGN KEY (ID_GAME)
REFERENCES GAME (ID);

ALTER TABLE T_GOLD CHANGE COLUMN PROVINCE R_PROVINCE VARCHAR(255);