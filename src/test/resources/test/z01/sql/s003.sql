create table t1
(
    t1c1 number(5)
);

create table t2
(
    t2c1 number(5)
);

create view v2 select * from t2;

insert into s1
select t1c1,t2c1,c.a as rc0,c.b as rc1, rc1+rc0 as rc2, rc1+rc2 as rc3 from t1,v2;