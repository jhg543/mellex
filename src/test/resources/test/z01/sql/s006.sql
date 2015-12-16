
UPDATE 
			${DWMDATA}.T80_LIAB_CR_T_ACT_INTCS_CND_H
SET
			 End_Date = CAST('$TXNDATE' AS DATE FORMAT 'YYYY-MM-DD')
WHERE
			 End_Date = CAST('$MAXDATE' AS DATE FORMAT 'YYYY-MM-DD')
AND( 
				Start_Date			    					 		--开始日期
			,	Account_Num									--合约账户标识
			,	Account_Modifier_Num				--合约账户修饰符
)IN(
		SELECT
				Start_Date			    					 		--开始日期
			,	Account_Num									--合约账户标识
			,	Account_Modifier_Num				--合约账户修饰符
			FROM VT_CHG_DATA
);