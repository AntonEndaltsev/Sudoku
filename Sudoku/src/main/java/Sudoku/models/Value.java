package Sudoku.models;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "sudokumain")
public class Value{
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "value")
    private int value;

    @Column(name = "variants")
    private String variants;

    public Value() {
    }

    public Value(int value, String variants) {
        this.value = value;
        this.variants = variants;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getValue() {
        return value;
    }

    public String getVariants() {
        return variants;
    }

    public void setVariants(String variants) {
        this.variants = variants;
    }

    public void setValue(int value) {
        this.value = value;
    }


}

