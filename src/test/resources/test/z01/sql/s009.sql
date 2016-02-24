insert into v1 (
p
)
select p  from
(
select p(casespecific) from x2 a1
) t1 group by 1

;