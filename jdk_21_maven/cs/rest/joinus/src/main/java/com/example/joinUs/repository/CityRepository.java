package com.example.joinUs.repository;

import com.example.joinUs.model.mongodb.City;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface CityRepository extends MongoRepository<City, String> {

    @Query(value = "{_id: ?0}")
    City findByCityId(String id);

    City findByName(String name);

}
