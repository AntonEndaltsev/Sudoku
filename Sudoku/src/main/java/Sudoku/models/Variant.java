//package Sudoku.models;
//
//import jakarta.persistence.*;
//
//import java.util.List;
//
//Entity
//@Table(name = "sudokuvariants")
//public class Variant {
//    @Id
//    @Column(name = "id")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int id;
//
//    @Column(name = "variant")
//    private int variant;
//
//    @ManyToOne
//    @JoinColumn(name = "value_id", referencedColumnName = "id")
//    private Value value;
//
//    public Variant() {
//    }
//
//    public int getId() {
//        return id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }
//
//    public int getVariant() {
//        return variant;
//    }
//
//    public void setVariant(int variant) {
//        this.variant = variant;
//    }
//
//    public Value getValue() {
//        return value;
//    }
//
//    public void setValue(Value value) {
//        this.value = value;
//    }
//}