
ALTER TABLE BATTLE_COUNTER
DROP FOREIGN KEY BATTLE_COUNTER_COUNTER,
DROP FOREIGN KEY BATTLE_COUNTER_BATTLE;
DROP TABLE BATTLE_COUNTER;
ALTER TABLE BATTLE
DROP FOREIGN KEY FK_BATTLE_GAME;
DROP TABLE BATTLE;

CREATE TABLE BATTLE (
  ID         BIGINT NOT NULL AUTO_INCREMENT,
  ID_GAME  BIGINT,
  TURN    INTEGER,
  R_PROVINCE    VARCHAR(255),
  STATUS VARCHAR(255),
  END VARCHAR(255),
  WINNER VARCHAR(255),
  PHASING_FORCES BIT,
  PHASING_TECH VARCHAR(255),
  PHASING_SIZE DOUBLE,
  PHASING_LOSSESSELECTED BIT,
  PHASING_RETREATSELECTED BIT,
  PHASING_FIRECOLUMN VARCHAR(255),
  PHASING_SHOCKCOLUMN VARCHAR(255),
  PHASING_MORAL INTEGER DEFAULT 0,
  PHASING_PURSUITMOD INTEGER DEFAULT 0,
  PHASING_PURSUIT INTEGER,
  PHASING_SIZEDIFF INTEGER DEFAULT 0,
  PHASING_RETREAT INTEGER,
  PHASING_LOSSES_ROUNDLOSS INTEGER DEFAULT 0,
  PHASING_LOSSES_THIRDLOSS INTEGER DEFAULT 0,
  PHASING_LOSSES_MORALELOSS INTEGER DEFAULT 0,
  PHASING_FIRSTDAY_FIREMOD INTEGER DEFAULT 0,
  PHASING_FIRSTDAY_FIRE INTEGER,
  PHASING_FIRSTDAY_SHOCKMOD INTEGER DEFAULT 0,
  PHASING_FIRSTDAY_SHOCK INTEGER,
  PHASING_SECONDDAY_FIREMOD INTEGER DEFAULT 0,
  PHASING_SECONDDAY_FIRE INTEGER,
  PHASING_SECONDDAY_SHOCKMOD INTEGER DEFAULT 0,
  PHASING_SECONDDAY_SHOCK INTEGER,
  NONPHASING_FORCES BIT,
  NONPHASING_TECH VARCHAR(255),
  NONPHASING_SIZE DOUBLE,
  NONPHASING_LOSSESSELECTED BIT,
  NONPHASING_RETREATSELECTED BIT,
  NONPHASING_FIRECOLUMN VARCHAR(255),
  NONPHASING_SHOCKCOLUMN VARCHAR(255),
  NONPHASING_MORAL INTEGER DEFAULT 0,
  NONPHASING_PURSUITMOD INTEGER DEFAULT 0,
  NONPHASING_PURSUIT INTEGER,
  NONPHASING_SIZEDIFF INTEGER DEFAULT 0,
  NONPHASING_RETREAT INTEGER,
  NONPHASING_LOSSES_ROUNDLOSS INTEGER DEFAULT 0,
  NONPHASING_LOSSES_THIRDLOSS INTEGER DEFAULT 0,
  NONPHASING_LOSSES_MORALELOSS INTEGER DEFAULT 0,
  NONPHASING_FIRSTDAY_FIREMOD INTEGER DEFAULT 0,
  NONPHASING_FIRSTDAY_FIRE INTEGER,
  NONPHASING_FIRSTDAY_SHOCKMOD INTEGER DEFAULT 0,
  NONPHASING_FIRSTDAY_SHOCK INTEGER,
  NONPHASING_SECONDDAY_FIREMOD INTEGER DEFAULT 0,
  NONPHASING_SECONDDAY_FIRE INTEGER,
  NONPHASING_SECONDDAY_SHOCKMOD INTEGER DEFAULT 0,
  NONPHASING_SECONDDAY_SHOCK INTEGER,
  PRIMARY KEY (ID)
);

ALTER TABLE BATTLE
ADD INDEX FK_BATTLE_GAME (ID_GAME),
ADD CONSTRAINT FK_BATTLE_GAME
FOREIGN KEY (ID_GAME)
REFERENCES GAME (ID);

CREATE TABLE BATTLE_COUNTER (
  ID_BATTLE         BIGINT NOT NULL,
  ID_COUNTER  BIGINT NOT NULL,
  PHASING    BIT,
  PRIMARY KEY (ID_BATTLE, ID_COUNTER)
);

ALTER TABLE BATTLE_COUNTER
ADD INDEX BATTLE_COUNTER_BATTLE (ID_BATTLE),
ADD CONSTRAINT BATTLE_COUNTER_BATTLE
FOREIGN KEY (ID_BATTLE)
REFERENCES BATTLE (ID);

ALTER TABLE BATTLE_COUNTER
ADD INDEX BATTLE_COUNTER_COUNTER (ID_COUNTER),
ADD CONSTRAINT BATTLE_COUNTER_COUNTER
FOREIGN KEY (ID_COUNTER)
REFERENCES COUNTER (ID);

ALTER TABLE T_COMBAT_RESULT
CHANGE COLUMN ROUND_LOSS ROUNDLOSS INTEGER,
CHANGE COLUMN THIRD_LOSS THIRDLOSS INTEGER,
CHANGE COLUMN MORALE_LOSS MORALELOSS INTEGER;

ALTER TABLE COUNTER
MODIFY COLUMN VETERANS DOUBLE;