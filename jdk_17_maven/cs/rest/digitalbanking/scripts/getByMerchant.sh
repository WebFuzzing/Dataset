# find all transactions for an account from one merchant in date range 
# easiest to look up in Redisinsight and plug value
curl -X GET -H "Content-Type: application/json"  'http://localhost:8080/merchantTransactions?merchant=Walmart&account=Acct195J&from=2022-06-27&to=2023-11-23'
