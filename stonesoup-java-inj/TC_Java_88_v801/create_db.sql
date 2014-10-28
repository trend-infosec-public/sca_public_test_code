USE stonesoup_crud;

DROP TABLE IF EXISTS stonesoup;

CREATE TABLE stonesoup (
	id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
	value VARCHAR(128),
	CONSTRAINT pk_stonesoup PRIMARY KEY(id)
);
