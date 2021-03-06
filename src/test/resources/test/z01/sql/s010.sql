

select c1.a,b,c from c1,c2 where x1>y1 and x2>y2;

select c1.a,b,c from c1,c2 group by 2;

select * from c1 inner join c2 on c1.x1 = c2.y1;

select c2.y1,c1.* from c1,c2;

with cte1 as (select y1+y2 as dd from c2) select dd from cte1;
 
with cte1 as (select y1+y2 as dd,x1+x2 as ee from c2,c1), cte2 as (select dd+ee as ff from cte1) select ff from cte2;

--select x3 from c1;

select r3+r4 as r5,r1+r2 as r3,x1 as r1, x2 as r4, y1 as r2 from c1,c2;


select count(*) from c1,c2;

select a1.a from c1 a1;

select x1 from c1 where exists (select * from c2 where c1.a=c2.c);
