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
                return isUploadedFileCorrect();
            } catch (Exception e) {
                return "Вам не удалось загрузить файл => " + e.getMessage();
            }
        } else {
            return "Вам не удалось загрузить файл, потому что файл пустой.";
        }
    }

    private String isUploadedFileCorrect(){
        String tempString;
        List<String> listString = new ArrayList<>();
        int length=0;

        try(BufferedReader br = new BufferedReader(new FileReader("sudoku-uploaded.txt")))
        {

            while((tempString=br.readLine())!=null){

                if (length==0) {
                    tempString = tempString.replace("\uFEFF", ""); // убираем BOM символ у первой строки, характерный для UTF
                    //System.out.println(tempString);
                    //System.out.println(tempString.length());
                    length = tempString.length();
                    //char [] temp = tempString.toCharArray();
                    //for (char c: temp) System.out.println((int)c);
                }
                // проверка, что все строки имеют одинаковое количество символов
                if (tempString.length()!=length || length % 3 != 0) return "Некорректные данные в файле => " + "проблема с количеством символов в строке №" + (listString.size()+1) + " " + length;
                listString.add(tempString);
            }
        }
        catch(IOException ex){
            System.out.println(ex.getMessage());
        }

        // проверяем что размер матрицы Судоку по вертикали равен размеру по горизонтали
        if (length!= listString.size()) return "Некорректные данные в файле => " + "количество строк:" + listString.size() + " количество символов в строке:" + length;

        // файл корректный, пора создавать структуру БД
        makePostgreSQLtables(listString);
        return "Вы удачно загрузили корректный файл";
    }

    @Transactional
    private void makePostgreSQLtables(List<String> listString){
        // удалить прежние таблицы, создать новые
        //System.out.println("OK");
        valueService.deleteAll();
        //System.out.println("OK");
        for (String str: listString){

            for (char c: str.toCharArray()){
                System.out.print(c);
                if (c!='\n') {
                    if (c == '.') valueService.save(new Value(0, "123456789"));
                    else valueService.save(new Value(Character.getNumericValue(c), "123456789"));
                }
                //System.out.println("("+valueService.+")");
            }
            System.out.println();
            //valueService.save(new Value(10, "123456789"));
        }
        System.out.println(valueService.findAll().size());
//        Iterator<Value> i = valueService.findAll().iterator();
//        int j=0;
//        StringBuilder sb = new StringBuilder();
//        while (i.hasNext()){
//            Value v = i.next();
//            j++;
//            if (Math.sqrt(valueService.findAll().size()) != j) sb.append(v.getValue());
//            if (Math.sqrt(valueService.findAll().size()) == j) {System.out.println(sb);j=0;sb = new StringBuilder();}
//        }
//        for (Value v: valueService.findAll()){
//
//            System.out.print(v.getValue());
//        }

    }


}
