
CREATE MULTISET	VOLATILE TABLE VT_ACCT_SHARE, NO	LOG
(
		 Asset_Id					  VARCHAR(150)
    ,Asset_Modifier_Num	  CHAR(3)
    ,Currency_Cd  CHAR(3)
		,TODAYSHARE	  DECIMAL(18,4)
    ,M_Pre_Share DECIMAL(18,4)																																			/* modify by yyang 20101120 替换yestodayshare */
)
PRIMARY	INDEX (Asset_Id,Asset_Modifier_Num)
ON COMMIT PRESERVE ROWS
;
--.IF	ERRORCODE <> 0 THEN	.GOTO ERRORQUIT;


INSERT INTO VT_ACCT_SHARE
SELECT
		 COALESCE(P2.Asset_Id,P3.Asset_Id)
		,COALESCE(P2.Asset_Modifier_Num,P3.Asset_Modifier_Num)
		,COALESCE(P2.Currency_Cd,P3.Currency_Cd)
		,COALESCE(P2.D_Share,0) AS TODAYSHARE
		,COALESCE(P3.D_Share,0) as M_Pre_Share																													/* modify by yyang 20101120 替换yestodayshare */
FROM
 (
 SEL Asset_Id
 ,Asset_Modifier_Num
 ,Currency_Cd
 ,D_Share
 FROM
  ${DWZVIEW}.T80_INVEST_ACCT_BAL_DD
  WHERE  DATA_DT=DATE'$TXNDATE' AND Asset_Modifier_Num NOT IN ('451')																/* modify by yyang 20110820 替换45 */
  ) AS  P2

FULL JOIN
(
 SEL Asset_Id
 ,Asset_Modifier_Num
 ,Currency_Cd
 ,D_Share
 FROM
  ${DWZVIEW}.T80_INVEST_ACCT_BAL_DD
  WHERE  DATA_DT= CAST(SUBSTR('${TXNDATE}',1,8)||'01' AS DATE FORMAT 'YYYY-MM-DD')-1								/* modify by yyang 20101120 替换DATE'$TXNDATE'-1 */
  AND Asset_Modifier_Num NOT IN ('451')																															/* modify by yyang 20110820 替换45 */
  )  P3
ON   P2.Asset_Id=P3.Asset_Id
AND P2.Asset_Modifier_Num=P3.Asset_Modifier_Num
AND  P2.Currency_Cd=P3.Currency_Cd
WHERE TODAYSHARE-M_Pre_Share<>0																																			/* modify by yyang 20101120 替换yestodayshare */
;