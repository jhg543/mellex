create table employees
(
    employee_id number,
    last_name number,
    job_id number
);

CREATE OR REPLACE FUNCTION f (x BOOLEAN, y PLS_INTEGER)
  RETURN employees.employee_id%TYPE
  AS
 
  TYPE RecordTyp IS RECORD (
    last employees.last_name%TYPE,
    id   employees.employee_id%TYPE
  );
  rec1 RecordTyp;
BEGIN
  SELECT last_name, employee_id INTO rec1
  FROM employees
  WHERE job_id = 'AD_PRES';

  DBMS_OUTPUT.PUT_LINE ('Employee #' || rec1.id || ' = ' || rec1.last);
END;
/