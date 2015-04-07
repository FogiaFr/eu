CREATE TABLE counter (
  id         BIGINT NOT NULL AUTO_INCREMENT,
  type       VARCHAR(255),
  id_country BIGINT,
  id_game    BIGINT,
  id_stack   BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE country (
  id   BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE event_political (
  id      BIGINT NOT NULL AUTO_INCREMENT,
  turn    INTEGER,
  id_game BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE game (
  id     BIGINT NOT NULL AUTO_INCREMENT,
  status VARCHAR(255),
  turn   INTEGER,
  PRIMARY KEY (id)
);

CREATE TABLE player (
  id         BIGINT NOT NULL AUTO_INCREMENT,
  id_country BIGINT,
  id_game    BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE province (
  id      BIGINT NOT NULL AUTO_INCREMENT,
  name    VARCHAR(255),
  terrain VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE province_eu (
  income       INTEGER,
  port         BOOLEAN,
  praesidiable BOOLEAN,
  id           BIGINT NOT NULL,
  id_country   BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE relation (
  id               BIGINT NOT NULL AUTO_INCREMENT,
  type             VARCHAR(255),
  id_player_first  BIGINT,
  id_game          BIGINT,
  id_player_second BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE stack (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  id_province BIGINT,
  PRIMARY KEY (id)
);

ALTER TABLE counter
ADD INDEX FK_COUNTER_COUNTRY (id_country),
ADD CONSTRAINT FK_COUNTER_COUNTRY
FOREIGN KEY (id_country)
REFERENCES country (id);

ALTER TABLE counter
ADD INDEX FK_COUNTER_GAME (id_game),
ADD CONSTRAINT FK_COUNTER_GAME
FOREIGN KEY (id_game)
REFERENCES game (id);

ALTER TABLE counter
ADD INDEX FK_COUNTER_STACK (id_stack),
ADD CONSTRAINT FK_COUNTER_STACK
FOREIGN KEY (id_stack)
REFERENCES stack (id);

ALTER TABLE event_political
ADD INDEX FK_EP_GAME (id_game),
ADD CONSTRAINT FK_EP_GAME
FOREIGN KEY (id_game)
REFERENCES game (id);

ALTER TABLE player
ADD INDEX FK_PLAYER_COUNTRY (id_country),
ADD CONSTRAINT FK_PLAYER_COUNTRY
FOREIGN KEY (id_country)
REFERENCES country (id);

ALTER TABLE player
ADD INDEX FK_PLAYER_GAME (id_game),
ADD CONSTRAINT FK_PLAYER_GAME
FOREIGN KEY (id_game)
REFERENCES game (id);

ALTER TABLE province_eu
ADD INDEX FK_PROVINCE_EU_COUNTRY (id_country),
ADD CONSTRAINT FK_PROVINCE_EU_COUNTRY
FOREIGN KEY (id_country)
REFERENCES country (id);

ALTER TABLE province_eu
ADD INDEX FK_PROVINCE_EU_PROVINCE (id),
ADD CONSTRAINT FK_PROVINCE_EU_PROVINCE
FOREIGN KEY (id)
REFERENCES province (id);

ALTER TABLE relation
ADD INDEX FK_RELATION_PLAYER_1 (id_player_first),
ADD CONSTRAINT FK_RELATION_PLAYER_1
FOREIGN KEY (id_player_first)
REFERENCES player (id);

ALTER TABLE relation
ADD INDEX FK_RELATION_GAME (id_game),
ADD CONSTRAINT FK_RELATION_GAME
FOREIGN KEY (id_game)
REFERENCES game (id);

ALTER TABLE relation
ADD INDEX FK_RELATION_PLAYER_2 (id_player_second),
ADD CONSTRAINT FK_RELATION_PLAYER_2
FOREIGN KEY (id_player_second)
REFERENCES player (id);

ALTER TABLE stack
ADD INDEX FK_STACK_PROVINCE (id_province),
ADD CONSTRAINT FK_STACK_PROVINCE
FOREIGN KEY (id_province)
REFERENCES province (id);
