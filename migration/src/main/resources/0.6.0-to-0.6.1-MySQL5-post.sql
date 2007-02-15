# 0.6.0-to-0.6.1-MySQL5-post.sql
# remove old data
alter table item drop foreign key FK317B137014CFFB;
alter table item drop index FK317B137014CFFB;
alter table item drop column parentid;

# update server version
update server_properties SET propertyvalue='${pom.version}' WHERE propertyname='cosmo.schemaVersion';