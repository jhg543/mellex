
UPDATE 
    P1 
FROM 
    ${GLKJMART}.PRVT_LOAN_FNC_IMT_TBL AS P1,
    ${GLKJMART}.PRVT_LNDR_ACC_TBL AS P2 
SET 
    LnDR_Expn_Amt = COALESCE(P2.Ln_Acr_DprcnRsrv_Amt,0),
    Last_Mod_Tm = current_timestamp(0) 
WHERE 
    P1.MULTI_TENANCY_ID = '${MULTI_ID}'
    AND P1.Acg_Dt = CAST('$TXNDATE' AS DATE FORMAT 'YYYY-MM-DD') 
    AND P2.MULTI_TENANCY_ID = '${MULTI_ID}' 
    AND P2.Acg_Dt =  CAST('$TXNDATE' AS DATE FORMAT 'YYYY-MM-DD')
    AND P1.Acc_ID = P2.Acc_ID
    AND P1.CcyCd = P2.CcyCd
    --AND P1.CCBIns_ID = P2.BookEntr_InsID 
;


   
CREATE VOLATILE TABLE VT_NEW_DATA,NO LOG
AS 
(
  SELECT A.Account_Num                                    --协议                         
        ,A.Account_Modifier_Num                           --协议修饰符                   
        ,'SA' As Acct_Cat_Cd                              --账户类型                     
        ,Substr(A.Account_Num,1,9) As Mgmt_Org_Id         --管理机构                     
        ,Substr(A.Account_Num,1,9) As Acct_Org_Id         --核算机构                     
        ,B.Ecif_Cust_No                                   --ECIF客户编号                 
        ,A.Agt_Name                                       --协议名称                     
        ,A.Currency_Cd                                    --币种                         
        ,B.Biz_Type_Cd                                    --业务种类                     
        ,Substr(A.Account_Num,15,8) As Gl_Acct_Cd         --核算码                       
        ,A.Signe_Dt                                       --开户日期                     
  FROM ${DWBVIEW}.T03_AGREEMENT A                         --协议表                       
  INNER JOIN ${DWMASMART}.MAS_ACCT_BASE_INFO_H B          --账户基本信息历史表           
  ON SUBSTR(A.Account_Num,1,9) = B.Acct_Org_Id                                           
  AND SUBSTR(A.Account_Num,15,8) = B.Gl_Acct_Cd                                          
      AND B.Eff_Flg = '0'                                 --有效                         
      AND B.Provision_Acct_Type_Cd = '20'                 --备抵户类型代码 20-被备抵户   
      AND B.Acct_Status_Cd = '101'                        --状态正常                     
      AND B.Start_Date <= '${TXNDATE}'                                                     
      AND B.End_Date > '${TXNDATE}'                                                        
  WHERE SUBSTR(A.Account_Num,13,2)  = '91'                --91-内部账户                  
      AND A.Account_Modifier_Num = '01'
  GROUP BY 1,2,3,4,5,6,7,8,9,10,11
)WITH DATA
PRIMARY INDEX(ACCOUNT_NUM,ACCOUNT_MODIFIER_NUM)
ON COMMIT PRESERVE ROWS
;
--.IF ERRORCODE <> 0 THEN .GOTO ERRORQUIT;

--通过被备抵户客户编号构造备抵户客户编号
CREATE VOLATILE TABLE VT_NEW_DATA_2,NO LOG
AS 
(
   SELECT AAA.Account_Num                     --协议             
         ,AAA.Account_Modifier_Num            --协议修饰符       
         ,AAA.Acct_Cat_Cd                     --账户类型         
         ,AAA.Mgmt_Org_Id                     --管理机构         
         ,AAA.Acct_Org_Id                     --核算机构         
         ,AAA.Ecif_Cust_No                    --ECIF客户编号     
         ,AAA.Agt_Name                        --协议名称         
         ,AAA.Currency_Cd                     --币种             
         ,AAA.Biz_Type_Cd                     --业务种类         
         ,AAA.Gl_Acct_Cd                      --核算码           
         ,AAA.Signe_Dt                        --开户日期         
         ,AAA.GRP_CORP_NO                     --集团编号         
         ,AAA.ECIF_CUST_NO_2                  --虚拟集团客户编号
   FROM (SELECT A.Account_Num                                                                  --协议          
               ,A.Account_Modifier_Num                                                         --协议修饰符    
               ,A.Acct_Cat_Cd                                                                  --账户类型      
               ,A.Mgmt_Org_Id                                                                  --管理机构      
               ,A.Acct_Org_Id                                                                  --核算机构      
               ,A.Ecif_Cust_No                                                                 --ECIF客户编号  
               ,A.Agt_Name                                                                     --协议名称  
               ,A.Currency_Cd                                                                  --币种
               ,A.Biz_Type_Cd                                                                  --业务种类      
               ,A.Gl_Acct_Cd                                                                   --核算码        
               ,A.Signe_Dt                                                                     --开户日期      
               ,COALESCE(B.GRP_CORP_NO,'NA')  AS GRP_CORP_NO                                   --集团编号        
               ,COALESCE('CIFCUV#'||A.ACCT_ORG_ID||B.GRP_CORP_NO,'NA') AS ECIF_CUST_NO_2       --虚拟集团客户编号
          FROM VT_NEW_DATA  A
          LEFT JOIN ${DWMASMART}.MASH_GRP_CUST_REL B        --客户与集团关系表
            ON SUBSTR(A.ECIF_CUST_NO,6) = B.ECIF_CUST_NO
           AND B.GRP_CORP_TYPE_CD = '01'                    --战略性集团 
           AND B.AS_OF_DT = DATE'${TXNDATE}'
         ) AAA
   QUALIFY ROW_NUMBER() OVER (PARTITION BY AAA.Account_Num,AAA.ACCOUNT_MODIFIER_NUM ORDER BY AAA.GRP_CORP_NO ) = 1
)
WITH DATA
PRIMARY INDEX(ACCOUNT_NUM,ACCOUNT_MODIFIER_NUM)
ON COMMIT PRESERVE ROWS 
;
--.IF ERRORCODE <> 0 THEN .GOTO ERRORQUIT;

/*使用备抵户与集团归属关系维表更新备抵户客户编号*/
UPDATE A
FROM VT_NEW_DATA_2 A , ${DWMASMART}.MA2DW_TRANSFER_GRP B
 SET GRP_CORP_NO = B.GRP_CORP_NO
    ,ECIF_CUST_NO_2 = 'CIFCUV#'||SUBSTR(TRIM(A.ACCOUNT_NUM),1,9)||TRIM(B.GRP_CORP_NO)
WHERE A.ACCOUNT_NUM = B.ACCOUNT_NUM 
  AND A.ACCOUNT_MODIFIER_NUM = B.ACCOUNT_MODIFIER_NUM
  AND B.AS_OF_DT = CAST(SUBSTR('${TXNDATE}',1,8)||'01' AS DATE FORMAT'YYYY-MM-DD') - 1  --取上月的维表
;
--.IF ERRORCODE <> 0 THEN .GOTO ERRORQUIT;

CREATE VOLATILE TABLE VT_NEW_DATA_4,NO LOG 
AS
(
  SELECT  A.Mgmt_Org_Id                  AS Mgmt_Org_Id                    --经营管理机构                  
         ,A.Acct_Org_Id                  AS Acct_Org_Id                    --核算机构                      
         ,A.Account_Num                  AS Account_Num                    --协议                          
         ,A.Account_Modifier_Num         AS Account_Modifier_Num           --协议修饰符                    
         ,'NA'                           AS Cust_Acct_No_Psbk              --客户账号（折号）              
         ,'NA'                           AS Cust_Acct_No_Cr                --客户账号（卡号）              
         ,A.Ecif_Cust_No_2               AS Ecif_Cust_No                   --客户编码(ECIF)                
         ,A.Agt_Name                     AS Cust_Nm                        --客户名称                      
         ,A.Currency_Cd                  AS Curr_Cd                        --币种                          
         ,Coalesce(B.Agt_Status_Cd,'101') AS Acct_Status_Cd                 --账户状态                      
       --,'16'                           AS Cust_Lvl_Cd                    --客户等级 
         ,'99'                           AS Cust_Lvl_Cd                    --客户等级     --modified by libing 20110728 客户等级默认值更改
         ,'NA'                           AS Acct_Attr_Cd                   --账户性质                      
         ,A.Biz_Type_Cd                  AS Biz_Type_Cd                    --业务种类(产品代码)            
         ,A.Gl_Acct_Cd                   AS Gl_Acct_Cd                     --核算码（科目号）              
         ,'NA'                           AS Overdue_Gl_Acct_Cd             --逾期核算码                    
         ,'NA'                           AS Non_Accrual_Gl_Acct_Cd         --非应计核算码                  
         ,Date '${MAXDATE}'              AS Due_Dt                         --到期日                        
         ,'NA'                           AS Debt_No                        --债项编号                      
         ,'NA'                           AS Risk_Lvl_Cd                    --十二级分类                    
         ,'1'                            AS Instrument_T_Type_Cd           --分类标志(包括国际卡)          
         ,0                              AS Deposit_Period                 --存期                          
         ,Date '${MAXDATE}'              AS Origin_Dt                      --起息日                        
         ,A.Signe_Dt                     AS Open_Acct_Dt                   --开户日期                      
         ,'01'                           AS Terms_Cycle_Type_Cd            --期限周期类型代码              
         ,'SA'                           AS Acct_Cat_Cd                    --账号种类                      
         ,'0'                            AS Eff_Flg                        --有效标志                      
         ,'30'                           AS Provision_Acct_Type_Cd         --备抵户类型代码                
    FROM VT_NEW_DATA_2 A
    LEFT JOIN ${DWBVIEW}.T03_AGT_STATUS_H B                 --协议状态历史
      ON A.Account_Num = B.Account_Num 
     AND A.Account_Modifier_Num = B.Account_Modifier_Num
     AND B.Agt_Status_Type_Cd = '01'                        --协议状态类型代码 01-基本状态
     AND B.Start_Date <= DATE '${TXNDATE}' 
     AND B.End_Date > DATE '${TXNDATE}'
) WITH DATA
PRIMARY INDEX(ACCOUNT_NUM,ACCOUNT_MODIFIER_NUM)
ON COMMIT PRESERVE ROWS 
;
--.IF ERRORCODE <> 0 THEN .GOTO ERRORQUIT;

--将临时表VT_CHG_DATA中的更新数据及新增数据插入目标表
INSERT INTO ${DWMASMART}.PRVT_ACC_DLY_ST
(
  MULTI_TENANCY_ID            --#多实体标识
 ,Acg_Dt                       --账务日期   
 ,AccNo                        --账号
 ,Mgmt_Org_Id                  --经营管理机构        
 ,Acct_Org_Id                  --核算机构            
 ,Ecif_Cust_No                 --客户编码(ECIF)      
 ,Cust_Nm                      --客户名称
 ,ASPD_ID                      --可售产品编号  需在临时表中添加该字段
 ,Ddln                         --期限  需在临时表中添加该字段
 ,Trm_UnCd	                   --期限单位代码
 ,Dep_Trm_Cd	                 --存款期限代码
 ,Dep_Fnds_Use_Cd	             --存款资金用途代码
 ,Loan_Use_Cd	                 --贷款用途代码
 ,Cst_AccNo	                   --客户账号
 ,Rsk_CL_Rslt_Cd	             --风险分类结果代码
 ,Bsn_Ctlg_ID	                 --业务种类编号
 ,Ldgr_Tp_ID	                 --核算类型编号
 ,Crpnd_DtTbl_Nm	             --#对应数据表名
 ,OpnAcc_Dt	                   --开户日期
 ,Acc_St	                     --账户状态
 ,DpBkInNo	                   --开户机构编号
 ,Cst_Grd_TpCd	               --客户等级类型代码
 ,CcyCd	                       --币种代码
 ,Rcrd_Crt_Dt_Tm	             --#记录创建日期时间
 ,Rcrd_Udt_Dt_Tm	             --#记录更新日期时间
)
SELECT
  MULTI_TENANCY_ID             --#多实体标识
 ,CAST('${MAXDATE}' AS DATE FORMAT 'YYYY-MM-DD')      --账务日期   
 ,Account_Num                  --账号
 ,Mgmt_Org_Id                  --经营管理机构        
 ,Acct_Org_Id                  --核算机构            
 ,Ecif_Cust_No                 --客户编码(ECIF)      
 ,Cust_Nm                      --客户名称
 ,ASPD_ID                      --可售产品编号  需在临时表中添加该字段  ？？？
 ,Deposit_Period               --期限  需在临时表中添加该字段  ？？？
 ,Terms_Cycle_Type_Cd	         --期限单位代码
 ,Dep_Trm_Cd	                 --存款期限代码       ？？？
 ,''             	             --存款资金用途代码   ？？？
 ,''	                         --贷款用途代码       ？？？
 ,Cust_Acct_No_Cr	             --客户账号
 ,Risk_Lvl_Cd	                 --风险分类结果代码
 ,Biz_Type_Cd	                 --业务种类编号
 ,Gl_Acct_Cd	                 --核算类型编号
 ,''            	             --#对应数据表名
 ,OpnAcc_Dt	                   --开户日期
 ,Acct_Status_Cd	             --账户状态
 ,''      	                   --开户机构编号     ？？？无此字段
 ,Cust_Lvl_Cd	                 --客户等级类型代码
 ,Curr_Cd	                     --币种代码
 ,CAST('${TXNDATE}' AS DATE FORMAT 'YYYY-MM-DD')                                
 ,CAST('${MAXDATE}' AS DATE FORMAT 'YYYY-MM-DD')
 FROM VT_NEW_DATA_4
 WHERE  Provision_Acct_Type_Cd = '30'
;
--.IF ERRORCODE <> 0 THEN .GOTO ERRORQUIT;



--备抵户客户编号插入集团和客户关系表
DELETE FROM ${DWMASMART}.MASH_GRP_CUST_REL 
 WHERE AS_OF_DT = DATE '${TXNDATE}' 
   AND GRP_CORP_TYPE_CD = '01' 
   AND ECIF_CUST_NO LIKE 'V#%'
;
--.IF ERRORCODE <> 0 THEN .GOTO ERRORQUIT;

INSERT INTO ${DWMASMART}.MASH_GRP_CUST_REL
SELECT DATE'${TXNDATE}'
      ,SUBSTR(TRIM(A.ECIF_CUST_NO_2),6)
      ,'01' AS GRP_CORP_TYPE_CD
      ,TRIM(A.GRP_CORP_NO) AS GRP_CORP_NO 
      ,SUBSTR(TRIM(A.ECIF_CUST_NO_2),8,9)  
      ,'NA'
      ,'NA'
      ,'NA'
      ,'NA'
      ,'NA'
      ,'NA'
      ,'NA'
 FROM VT_NEW_DATA_2 A
WHERE TRIM(GRP_CORP_NO) <> 'NA'
GROUP BY 1,2,3,4,5
;