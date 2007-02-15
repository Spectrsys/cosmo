# 0.6.0-to-0.6.1-Derby-pre.sql
# create new tables
create table collection_item (itemid bigint not null, collectionid bigint not null, primary key (itemid, collectionid))
alter table collection_item add constraint FK3F30F8145361D2A6 foreign key (itemid) references item
alter table collection_item add constraint FK3F30F8148B8DC8EF foreign key (collectionid) references item

create table tombstones (id bigint not null, removedate bigint not null, itemuid varchar(255) not null, collectionid bigint not null, primary key (id))
alter table tombstones add constraint FK40CA41FE8B8DC8EF foreign key (collectionid) references item

# alter existing tables

# migrate data
insert into collection_item (itemid, collectionid) select id, parentid from item where parentid is not null