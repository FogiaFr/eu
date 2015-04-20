CREATE TABLE GAME (
  ID     BIGINT NOT NULL AUTO_INCREMENT,
  STATUS VARCHAR(255),
  TURN   INTEGER,
  PRIMARY KEY (ID)
);

CREATE TABLE COUNTER (
  ID         BIGINT NOT NULL AUTO_INCREMENT,
  TYPE       VARCHAR(255),
  ID_COUNTRY BIGINT,
  ID_STACK   BIGINT,
  PRIMARY KEY (ID)
);

CREATE TABLE COUNTRY (
  ID   BIGINT NOT NULL AUTO_INCREMENT,
  NAME VARCHAR(255),
  PRIMARY KEY (ID)
);

CREATE TABLE EVENT_POLITICAL (
  ID      BIGINT NOT NULL AUTO_INCREMENT,
  TURN    INTEGER,
  ID_GAME BIGINT,
  PRIMARY KEY (ID)
);

CREATE TABLE PLAYER (
  ID         BIGINT NOT NULL AUTO_INCREMENT,
  ID_COUNTRY BIGINT,
  ID_GAME    BIGINT,
  PRIMARY KEY (ID)
);

CREATE TABLE PROVINCE (
  ID      BIGINT NOT NULL AUTO_INCREMENT,
  NAME    VARCHAR(255),
  TERRAIN VARCHAR(255),
  PRIMARY KEY (ID)
);

CREATE TABLE PROVINCE_EU (
  INCOME       INTEGER,
  PORT         BIT,
  PRAESIDIABLE BIT,
  ID           BIGINT NOT NULL,
  ID_COUNTRY   BIGINT,
  PRIMARY KEY (ID)
);

CREATE TABLE RELATION (
  ID               BIGINT NOT NULL AUTO_INCREMENT,
  TYPE             VARCHAR(255),
  ID_PLAYER_FIRST  BIGINT,
  ID_GAME          BIGINT,
  ID_PLAYER_SECOND BIGINT,
  PRIMARY KEY (ID)
);

CREATE TABLE STACK (
  ID          BIGINT NOT NULL AUTO_INCREMENT,
  ID_PROVINCE BIGINT,
  ID_GAME     BIGINT,
  PRIMARY KEY (ID)
);

ALTER TABLE COUNTER
ADD INDEX FK_COUNTER_COUNTRY (ID_COUNTRY),
ADD CONSTRAINT FK_COUNTER_COUNTRY
FOREIGN KEY (ID_COUNTRY)
REFERENCES COUNTRY (ID);

ALTER TABLE COUNTER
ADD INDEX FK_COUNTER_STACK (ID_STACK),
ADD CONSTRAINT FK_COUNTER_STACK
FOREIGN KEY (ID_STACK)
REFERENCES STACK (ID);

ALTER TABLE EVENT_POLITICAL
ADD INDEX FK_EP_GAME (ID_GAME),
ADD CONSTRAINT FK_EP_GAME
FOREIGN KEY (ID_GAME)
REFERENCES GAME (ID);

ALTER TABLE PLAYER
ADD INDEX FK_PLAYER_COUNTRY (ID_COUNTRY),
ADD CONSTRAINT FK_PLAYER_COUNTRY
FOREIGN KEY (ID_COUNTRY)
REFERENCES COUNTRY (ID);

ALTER TABLE PLAYER
ADD INDEX FK_PLAYER_GAME (ID_GAME),
ADD CONSTRAINT FK_PLAYER_GAME
FOREIGN KEY (ID_GAME)
REFERENCES GAME (ID);

ALTER TABLE PROVINCE_EU
ADD INDEX FK_PROVINCE_EU_COUNTRY (ID_COUNTRY),
ADD CONSTRAINT FK_PROVINCE_EU_COUNTRY
FOREIGN KEY (ID_COUNTRY)
REFERENCES COUNTRY (ID);

ALTER TABLE PROVINCE_EU
ADD INDEX FK_PROVINCE_EU_PROVINCE (ID),
ADD CONSTRAINT FK_PROVINCE_EU_PROVINCE
FOREIGN KEY (ID)
REFERENCES PROVINCE (ID);

ALTER TABLE RELATION
ADD INDEX FK_RELATION_PLAYER_1 (ID_PLAYER_FIRST),
ADD CONSTRAINT FK_RELATION_PLAYER_1
FOREIGN KEY (ID_PLAYER_FIRST)
REFERENCES PLAYER (ID);

ALTER TABLE RELATION
ADD INDEX FK_RELATION_GAME (ID_GAME),
ADD CONSTRAINT FK_RELATION_GAME
FOREIGN KEY (ID_GAME)
REFERENCES GAME (ID);

ALTER TABLE RELATION
ADD INDEX FK_RELATION_PLAYER_2 (ID_PLAYER_SECOND),
ADD CONSTRAINT FK_RELATION_PLAYER_2
FOREIGN KEY (ID_PLAYER_SECOND)
REFERENCES PLAYER (ID);

ALTER TABLE STACK
ADD INDEX FK_STACK_PROVINCE (ID_PROVINCE),
ADD CONSTRAINT FK_STACK_PROVINCE
FOREIGN KEY (ID_PROVINCE)
REFERENCES PROVINCE (ID);

ALTER TABLE STACK
ADD INDEX FK_STACK_GAME (ID_GAME),
ADD CONSTRAINT FK_STACK_GAME
FOREIGN KEY (ID_GAME)
REFERENCES GAME (ID);
