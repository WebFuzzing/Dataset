package com.example.joinUs.service;

import com.example.joinUs.Utils;
import com.example.joinUs.mapping.CityMapper;
import com.example.joinUs.model.embedded.CityEmbedded;
import com.example.joinUs.model.mongodb.City;
import com.example.joinUs.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;


@Service
public class CityService {

    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private CityMapper cityMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    public void parseCity(CityEmbedded cityOld) {
        if (!Utils.isNullOrEmpty(cityOld)) {
            if (!Utils.isNullOrEmpty(cityOld.getCityId())) {
                String cityId = cityOld.getCityId();
                City city = cityRepository.findByCityId(cityId);
                if (city != null) cityOld = cityMapper.toEmbedded(city);
            } else if (!Utils.isNullOrEmpty(cityOld.getName())) {
                String cityName = cityOld.getName();
                City city = cityRepository.findByName(cityName);
                if (city != null) cityOld = cityMapper.toEmbedded(city);
            }
        }
    }

}

