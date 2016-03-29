create table c1
(
   a number,
   b number,
   x1 number,
   x2 number
);

create table c2
(
   a number,
   c number,
   y1 number,
   y2 number
);

insert into c1
select c1.a,b,c,y2 from c1,c2 where x1>y1 and x2>y2;
