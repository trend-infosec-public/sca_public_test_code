use mysql;
INSERT INTO mysql.user (Host,User,Password) VALUES('%','TC_88_640',PASSWORD('pa$$word'));
flush privileges;

DROP SCHEMA `stonesoup_TC_88_640` ;
CREATE SCHEMA `stonesoup_TC_88_640`;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `stonesoup_crud` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE stonesoup_crud;
DROP TABLE IF EXISTS stonesoup;

CREATE TABLE stonesoup (
	id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
	value VARCHAR(128),
	CONSTRAINT pk_stonesoup PRIMARY KEY(id)
);
