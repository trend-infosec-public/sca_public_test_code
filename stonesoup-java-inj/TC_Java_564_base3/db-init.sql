-- create table(s)
use transactions;

drop table if exists PendingTransactions; 
create table PendingTransactions (
	id int NOT NULL AUTO_INCREMENT,
    trans_from varchar(255),
    trans_to varchar(255),
    amount int,
    trans_date varchar(10),
    primary key (id)
);

-- insert test data
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Danny','Mark',5,'2012-03-22');
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Danny','Ivan',15,'2012-03-22');
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Danny','Matt',25,'2012-03-22');
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Danny','Jon',35,'2012-03-22');

insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Bryan','Mark',55,'2012-03-22');
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Bryan','Ivan',65,'2012-03-22');
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Bryan','Matt',75,'2012-03-22');
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Bryan','Jon',85,'2012-03-22');

insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Chris','Mark',15,'2012-03-22');
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Chris','Ivan',45,'2012-03-22');
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Chris','Matt',225,'2012-03-22');
insert into PendingTransactions (trans_from, trans_to, amount, trans_date) values ('Chris','Jon',55,'2012-03-22');
