create table t1
(
    c1 number(20)
);

create table t2
(
    c2 number(20)
);


update a1 from t1 a1,t2 a2 set c1=c1+c2;
