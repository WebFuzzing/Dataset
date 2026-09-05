insert into USERS (id, authorization_level, date_created, email_address, name) values (1, 0, '2001-02-03', null, 'INTEGRATION TEST USER');
insert into USERS (id, authorization_level, date_created, email_address, name) values (2, 10, '2001-02-03', null, 'INTEGRATION TEST MODERATOR');
insert into USERS (id, authorization_level, date_created, email_address, name) values (3, 20, '2001-02-03', null, 'INTEGRATION TEST ADMINISTRATOR');


insert into user_identity (id, date_created, type, user_id, value) values (null, '2001-02-03', 'API', 1, 'user');
insert into user_identity (id, date_created, type, user_id, value) values (null, '2001-02-03', 'API', 2, 'moderator');
insert into user_identity (id, date_created, type, user_id, value) values (null, '2001-02-03', 'API', 3, 'administrator');
