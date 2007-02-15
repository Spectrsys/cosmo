# 0.6.0-to-0.6.1-MySQL5-pre.sql

# create new tables
create table collection_item (itemid bigint not null, collectionid bigint not null, primary key (itemid, collectionid)) ENGINE=InnoDB;
alter table collection_item add index FK3F30F8145361D2A6 (itemid), add constraint FK3F30F8145361D2A6 foreign key (itemid) references item (id);
alter table collection_item add index FK3F30F8148B8DC8EF (collectionid), add constraint FK3F30F8148B8DC8EF foreign key (collectionid) references item (id);

create table tombstones (id bigint not null auto_increment, removedate bigint not null, itemuid varchar(255) not null, collectionid bigint not null, primary key (id)) ENGINE=InnoDB;
alter table tombstones add index FK40CA41FE8B8DC8EF (collectionid), add constraint FK40CA41FE8B8DC8EF foreign key (collectionid) references item (id);

# alter existing tables
alter table stamp drop index itemid;
alter table stamp add unique itemid (itemid, stamptype, isactive);

# migrate data
insert into collection_item (itemid, collectionid) select id, parentid from item where parentid is not null;