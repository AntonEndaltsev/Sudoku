package Sudoku.services;


import Sudoku.models.Value;
import Sudoku.repositories.ValueRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Service
@Transactional(readOnly = true)
public class ValueService {

    private final ValueRepository valueRepository;

    @Autowired
    public ValueService(ValueRepository valueRepository) {
        this.valueRepository = valueRepository;
    }

    public List<Value> findAll() {
        return valueRepository.findAll();
    }

    public Value findOne(int id){
        Optional<Value> foundValue = valueRepository.findById(id);
        return foundValue.orElse((null));
    }

    public Value findByValue(int value){
        Optional<Value> foundValue = valueRepository.findByValue(value);
        return foundValue.orElse((null));
    }

    @Transactional
    public void save(Value value){
        valueRepository.save(value);
    }

    @Transactional
    public void update(int id, Value updatedValue){
        updatedValue.setId(id);
        valueRepository.save(updatedValue);
    }

    @Transactional
    public void delete(int id){
        valueRepository.deleteById(id);
    }

    @Transactional
    public void deleteAll(){
        valueRepository.deleteAll();
    }




}
