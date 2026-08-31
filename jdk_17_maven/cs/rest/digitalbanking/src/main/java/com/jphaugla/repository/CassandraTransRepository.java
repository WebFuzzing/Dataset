package com.jphaugla.repository;

import com.jphaugla.domain.CassandraTransaction;
import com.jphaugla.domain.Transaction;
import org.springframework.data.cassandra.repository.CassandraRepository;


import java.util.UUID;

public interface CassandraTransRepository extends CassandraRepository<Transaction, String> {

}