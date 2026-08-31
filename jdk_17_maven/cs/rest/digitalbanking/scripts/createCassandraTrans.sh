export CQLSH_HOST=4.236.143.55
cqlsh -e "create keyspace if not exists banking with replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 } ;"
cqlsh < trans.cql
