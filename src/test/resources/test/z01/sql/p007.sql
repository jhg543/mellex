
create procedure x as
sqlstr number;
v_report_base_id number;
begin
v_report_base_id:='AAA';
                 sqlstr := 'INSERT INTO YOURDADDY.YOURMOMMY ( ' || v_report_base_id ;
                        FOR i IN 1..90 LOOP
                  sqlstr := sqlstr || ',' || unknown(v_report_base_id);
                   END LOOP;
                   sqlstr := sqlstr || ')';
                   EXECUTE IMMEDIATE sqlstr;
end;function
/