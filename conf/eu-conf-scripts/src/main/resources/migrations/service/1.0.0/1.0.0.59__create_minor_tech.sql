CREATE TABLE MINOR_TECH (
  ID_GAME  BIGINT,
  CULTURE VARCHAR(255),
  T_LAND_TECH VARCHAR(255),
  T_NAVAL_TECH VARCHAR(255),
  PRIMARY KEY (ID_GAME, CULTURE)
);

INSERT INTO MINOR_TECH (ID_GAME, CULTURE, T_LAND_TECH, T_NAVAL_TECH)
  SELECT ID, 'LATIN', 'MEDIEVAL', 'CARRACK' FROM GAME;
INSERT INTO MINOR_TECH (ID_GAME, CULTURE, T_LAND_TECH, T_NAVAL_TECH)
  SELECT ID, 'ORTHODOX', 'MEDIEVAL', 'CARRACK' FROM GAME;
INSERT INTO MINOR_TECH (ID_GAME, CULTURE, T_LAND_TECH, T_NAVAL_TECH)
  SELECT ID, 'ISLAM', 'MEDIEVAL', 'CARRACK' FROM GAME;
INSERT INTO MINOR_TECH (ID_GAME, CULTURE, T_LAND_TECH, T_NAVAL_TECH)
  SELECT ID, 'ROTW', 'MEDIEVAL', 'CARRACK' FROM GAME;
INSERT INTO MINOR_TECH (ID_GAME, CULTURE, T_LAND_TECH, T_NAVAL_TECH)
  SELECT ID, 'MEDIEVAL', 'MEDIEVAL', 'CARRACK' FROM GAME;
