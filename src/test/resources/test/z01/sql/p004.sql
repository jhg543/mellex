CREATE OR REPLACE PROCEDURE p (
  dept_no NUMBER
)  AS
BEGIN
  DELETE FROM dept_temp
  WHERE department_id = dept_no;
 
  IF SQL%FOUND THEN
    DBMS_OUTPUT.PUT_LINE (
      'Delete succeeded for department number ' || dept_no
    );
  ELSE
    DBMS_OUTPUT.PUT_LINE ('No department number ' || dept_no);
  END IF;
END;
/