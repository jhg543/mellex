
--insert into c1
--select c1.a,b,c,y2 from c1,c2 where x1>y1 and x2>y2;

--update c1 as a1 from c1,c2 set a = a1.x1+c2.y1 where c1.x2=c2.y2;


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
