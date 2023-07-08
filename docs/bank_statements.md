---
layout: default
---
# Bank statements
Countary imports transactions from bank statements. Your bank needs to provide a bank
statement for each account that you wish to track in some format that Countary can process.
The current formats supported by Countary are:
1. OFX 1.x (Tested with RMB Private Bankstatements)
2. OFX 2.x (Tested with Capitec Bank statements)**
3. CSV (RMB Private Bank/First National Bank)
4. CSV (Bank Zero)*
5. CSV (Capitec Bank)

*To import the Bank Zero statement, first convert the monthly Excel sheet mailed to you 
by the bank to .csv format.
**Unfortunately the *OFX 2.x* statement for Capitec is flawed: the balance is wrong. At least
this was the case at the time of testing. Better use the CSV version.

The banks mentioned are banks in South Africa and the CSV formats will likely not work for
any other bank. However, as long as your bank supports exporting of banks statements in 
either OFX 1.x or OFX 2.x formats, you should be able to use Countary! If you experience
any issues with importing of bank statements, follow the guidance on [troubleshooting](trouble.markdown).


Ensure that exporting is done regularly enough that there is sufficient overlap between
statements so that no transactions are skipped. Countary will detect duplicate transactions
and won't import them, but if a duplicate should slip by, it can be deleted from the 
`Transactions worksheet`. It has been observed with some banks that transactions get
re-arranged during the month and this can sometimes cause the calculation that is used
to detect duplicate transactions to miss some.

To import a bank statement, first get an exported statement from your bank's website. 
Then make sure you have selected the correct account from the list of accounts in the 
top-left area of the `Transactions worksheet` and then click on the **Load Statement...** 
button. Select the appropriate format for the statement and click on **OK**. Use the
`File-Open` dialog that pops up to locate the statement exported from you bank. Countary
will match the account number in the statement with the selected account and will then
load all transactions in the statement except for any duplicates that it detects.

You can now proceed to allocate the transactions to your budgets. To learn more about
budgets, go to [**Budgets**](budgets.markdown).

Countary will remember the format you selected the last time when you are ready to import
your next statement.

Back to [User Guide](user_guide.markdown)
