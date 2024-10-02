package Sudoku.services;


import Sudoku.models.Value;
import Sudoku.repositories.ValueRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
    public void saveAll(List<Value> values){
        valueRepository.saveAll(values);
    }

    @Transactional
    public void delete(int id){
        valueRepository.deleteById(id);
    }

    @Transactional
    public void deleteAll(){
        valueRepository.deleteAll();
    }

    @Transactional
    public String WorkWithFile(){
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

        //основные вычисления через рекурсию
        calculate();

        return "Вы удачно загрузили корректный файл";
    }

    @Transactional
    private void makePostgreSQLtables(List<String> listString){
        // удалить прежние таблицы, создать новые
        //System.out.println("OK");
        this.deleteAll();
        //System.out.println("OK");
        for (String str: listString){

            for (char c: str.toCharArray()){
                System.out.print(c);
                if (c == '.') this.save(new Value(0, "123456789"));
                else this.save(new Value(Character.getNumericValue(c), "123456789"));

                //System.out.println("("+valueService.+")");
            }
            System.out.println();
            //valueService.save(new Value(10, "123456789"));
        }
    }


    @Transactional
    private void calculate(){
        List<Value> allValues = this.findAll();
//        System.out.println("----");
//        Collections.sort(allValues);
//        for (Value v: allValues) {
//            System.out.println(v.getValue() + " " + v.getVariants());
//        }
//        System.out.println("----");
        //проверка по горизонтали, что нет повторений в значениях
        int counter1 = 0;
        int counter2 = 0;
        boolean isOk = false;
        //int counter3 = -1;
        //List<Value> inLineValues = new ArrayList<>();
        int[][] valuesToDelete = new int[(int)Math.sqrt(allValues.size())][(int)Math.sqrt(allValues.size())];

        // пробегаем по всем значениям из базы и собираем двумерный массив значений, которые надо вычистить из поля variants базы
        Collections.sort(allValues);
        for (Value v: allValues){
            counter1++;
            //if (counter1 == 1) System.out.println("---" + v.getValue() + "---");
            //counter3++;
            valuesToDelete[counter2][counter1-1] = 0;
            if (v.getValue()!=0) {
                valuesToDelete[counter2][counter1-1] = v.getValue();

                //System.out.print(v.getValue());
            }
            //System.out.print(valuesToDelete[counter2][counter1-1]);


            if (counter1==Math.sqrt(allValues.size())) {
                //System.out.println();
                counter1 = 0;
                counter2++;
                //counter3 = 0;
            }

        }

        counter1 = 0;
        counter2 = 0;
        // чистим поле variants у каждого значения
        Collections.sort(allValues);
        for (Value v: allValues){
            counter1++;
            if (v.getValue()==0) {
                for (int i = 0; i < valuesToDelete.length; i++) {
                    v.setVariants(v.getVariants().replaceAll(String.valueOf(valuesToDelete[counter2][i]), "")); // удаляем по горизонтали
                    v.setVariants(v.getVariants().replaceAll(String.valueOf(valuesToDelete[i][counter1 - 1]), "")); // удаляем по вертикали
                    cleanSector(counter2+1, counter1, valuesToDelete.length, v, valuesToDelete); // удаляем пересекающиеся значения внутри сектора

                }
            }
            if (counter1==Math.sqrt(allValues.size())) {
                counter2++;
                counter1 = 0;
            }
            this.save(v);

        }

        // Проверяем есть ли variants состоящие из одной цифры, если есть - то вставляем их в нулевые value
        Collections.sort(allValues);
        for (Value v: allValues) {
            if (v.getValue()==0) {
                if (v.getVariants().isEmpty())
                    System.out.println("Ошибка вычисления, некорректные данные в файле");
                if (v.getVariants().length() == 1) {
                    v.setValue(Integer.parseInt(v.getVariants()));
                    v.setVariants("");
                    this.save(v);
                    isOk = true;
                }
                //if (v.getVariants().length() > 1) isOk = true; // остались variants содержащие больше 1ой цифры

            }
        }

        if (isOk) {
            Collections.sort(allValues);
            counter1=0;
            System.out.println("---");
            for (Value v: allValues) {
                counter1++;
                if (v.getValue()>0) System.out.print(v.getValue());
                else System.out.print(".");
                if (counter1==Math.sqrt(allValues.size())){
                    counter1=0;
                    System.out.println();
                }
            }
            Collections.sort(allValues);
            System.out.println("---");
            for (Value v: allValues) {
                System.out.println(v.getValue()+" "+v.getVariants());
            }
            System.out.println("Пошла рекурсия");calculate(); // рекурсия, повторяем процедуру вычислений
        }
        //if (valueService.findByValue(0)!=null) System.out.println("Все сложно, нет однозначности");
    }

    private void cleanSector(int x,int y, int z, Value v, int[][] valuesToDelete){
        int countOfSectorInLine = z / 3;
        int numberOfSector = (x-1)/3 + (y-1)/3*countOfSectorInLine + 1;
        for (int i = 1; i<z+1 ; i++) {
            for (int j = 1; j<z+1; j++) {
                if (numberOfSector==(i-1)/3 + (j-1)/3*countOfSectorInLine + 1) {
                    v.setVariants(v.getVariants().replaceAll(String.valueOf(valuesToDelete[i-1][j-1]), ""));
                    if (x==2 && y==1) {
                        //System.out.println("numberofsector=" + numberOfSector);
                        //System.out.println(valuesToDelete[i-1][j-1]);
                    }
                }
            }
        }
    }


}
