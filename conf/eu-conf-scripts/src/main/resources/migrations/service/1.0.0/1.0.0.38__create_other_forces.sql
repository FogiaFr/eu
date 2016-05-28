CREATE TABLE OTHER_FORCES (
  ID         BIGINT NOT NULL AUTO_INCREMENT,
  R_PROVINCE  VARCHAR(255),
  TYPE       VARCHAR(255),
  LD    INTEGER ,
  LDE    INTEGER ,
  VETERAN    BIT,
  REPLENISH    BIT,
  ID_GAME BIGINT,
  PRIMARY KEY (ID)
);

ALTER TABLE OTHER_FORCES
ADD INDEX FK_OTHER_FORCES_GAME (ID_GAME),
ADD CONSTRAINT FK_OTHER_FORCES_GAME
FOREIGN KEY (ID_GAME)
REFERENCES GAME (ID);

ALTER TABLE COUNTRY
ADD COLUMN COL_MALUS INTEGER DEFAULT 3;

UPDATE COUNTRY SET COL_MALUS = 3;