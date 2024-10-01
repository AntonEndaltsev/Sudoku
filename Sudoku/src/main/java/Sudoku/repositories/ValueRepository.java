package Sudoku.repositories;


import Sudoku.models.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ValueRepository extends JpaRepository<Value, Integer> {
    Optional<Value> findByValue(int value);
}

