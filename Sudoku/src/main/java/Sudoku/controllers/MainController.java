package Sudoku.controllers;

import Sudoku.models.Value;
import Sudoku.services.ValueService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@RestController
public class MainController {

    private final ValueService valueService;

    public MainController(ValueService valueService) {
        this.valueService = valueService;
    }


    @PostMapping("/uploadfile")
    public String handleFileUpload(@RequestParam("file") MultipartFile file){
        if (!file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                BufferedOutputStream stream =
                        new BufferedOutputStream(new FileOutputStream(new File("sudoku-uploaded.txt")));
                stream.write(bytes);
                stream.close();
                return valueService.WorkWithFile(); // запускаем все вычисления в valuesrvice

            } catch (Exception e) {
                return "Вам не удалось загрузить файл => " + e.getMessage();
            }
        } else {
            return "Вам не удалось загрузить файл, потому что файл пустой.";
        }

    }



}
