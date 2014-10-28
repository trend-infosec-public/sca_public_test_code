-- create table(s)
use contractor;

drop table if exists Contractor; 
create table Contractor (
    id int NOT NULL AUTO_INCREMENT,
    classified boolean,
    first varchar(255),
    last varchar(255),
    title varchar(255),
    primary key (id)
);

-- insert test data
insert into Contractor (classified, first, last, title) values (false, 'Gob', 'Bluth', 'Magician');
insert into Contractor (classified, first, last, title) values (false, 'Tobias', 'Funke', 'Actor');
insert into Contractor (classified, first, last, title) values (true, 'Michael', 'Bluth', 'CEO');
insert into Contractor (classified, first, last, title) values (true, 'George', 'Bluth Sr.', 'Fugitive');

        