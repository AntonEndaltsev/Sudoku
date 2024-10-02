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
import java.util.*;


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

        List<Value> allValues = this.findAll();
        Collections.sort(allValues);
        int counter1 = 0;
        for (Value v:allValues) {
            System.out.print(v.getValue());
            counter1++;
            if (counter1==Math.sqrt(allValues.size())){
                counter1=0;
                System.out.println();
            }
        }

        return "Вы удачно загрузили файл";
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

        boolean isOk = false; // понадобится в конце, чтобы определить нужна ли еще рекурсия

        List<Value> allValues = this.findAll();

        int[][] valuesToDelete = calculatePart1(allValues);

        calculatePart2(valuesToDelete, allValues);

        isOk = calculatePart3(false, allValues);



        // нашелся хотя бы один variants содержащий ровно одну цифру, которую подставили в итоговое значение -> пора пересчитывать рекурсивно
        if (isOk) {
            allValues = this.findAll();
            Collections.sort(allValues);
            int counter1=0;
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
//            Collections.sort(allValues);
//            System.out.println("---");
//            for (Value v: allValues) {
//                System.out.println(v.getValue()+" "+v.getVariants());
//            }
            //System.out.println("Пошла рекурсия");
            calculate(); // рекурсия, повторяем процедуру вычислений
        }

        // тут мы окажемся, только если или все решено, или остались variants, содержащие более 1 цифры, или не сходится пазл
        Collections.sort(allValues);
        isOk=true;
        int counter1=0;
        for (Value v: allValues) {
                if (v.getValue()==0 && v.getVariants().isEmpty()) {isOk=true;counter1=1;break;} // получили не сходимость
                if (v.getValue()==0 && v.getVariants().length()>1) {isOk=false;} // получили, что есть варианты с variants больше одной цифры
        }

        // точно есть еще неопределенные элементы, у котороых variants содержат более одной цифры (неоднозначность)
       // начинает рекурсивно перебирать деревья вариантов
        if (!isOk) {calculate3(allValues);}


        if (isOk && counter1 ==1) System.out.println("Ошибка в данных файла, невозможно получить ответ");

    }

    private void calculate3(List<Value> allValues) {
        //System.out.println("Все сложно, нет однозначности");
        //найти variants с минимальным количеством цифры для оптимизации
        int min = (int) Math.sqrt(allValues.size());
        Collections.sort(allValues);
        for (Value v: allValues) {
            if (v.getValue()==0 && v.getVariants().length()<min) min = v.getVariants().length();
        }

        String baseValueOfThisVariants ="";
        Value v1 = null;
        Collections.sort(allValues);
        int counter2 = 0;
        List<Value> saveValues = new ArrayList<>();
        for (Value v: allValues) {
            Value v2 = new Value();
            v2.setValue(v.getValue());
            v2.setVariants(v.getVariants());
            saveValues.add(v2);
            if (v2.getVariants().length()==min && counter2==0){
                baseValueOfThisVariants = v2.getVariants();
                //System.out.println("baseValueOfThisVariants= " + baseValueOfThisVariants);
                counter2=1;
                v1=v;
            }
        }
        //System.out.println("saveValues=" + saveValues.get(0).getValue());

        // пробежаться в цикле - и подставлять одно значение за другим, пока не получим сходимость
        for (char c: baseValueOfThisVariants.toCharArray()) {
            calculate2(c, baseValueOfThisVariants, allValues, v1); // результат или все круто, или не получилось
            counter2 = 0; // флажок, для определения - если есть сходимость, то выйти из цикла
            allValues = this.findAll();
            Collections.sort(allValues);

            // тут могут быть два варианта или все сошлось или могут быть нули с соответствующими variants больше одной цифры
            for (Value v : allValues) {
                if (v.getValue() == 0) {
                    counter2 = 1;
                    break;
                }

            }
            if (counter2 == 0) break; // все круто, нашлось решение
            if (counter2 == 1) {
                //тут мы на развилке - или ветка тупиковая или в ветке получись только нули с соответствующими variants больше одной цифры
                counter2 = 0;
                for (Value v : allValues) {
                    if (v.getValue() == 0 && v.getVariants().isEmpty()) {
                        counter2 = 1;
                        break;
                    }

                }
                if (counter2 == 0) calculate3(allValues); // вновь рекурсия, перебираем деревья
                if (counter2 == 1) { // ветка тупиковая
                    counter2 = 0;
                    for (Value v : allValues) {
                        v.setValue(saveValues.get(counter2).getValue());
                        v.setVariants(saveValues.get(counter2).getVariants());
                        counter2++;
                    }
                    //System.out.println("неудачная ветка2");

                    //saveAll2(saveValues); // надо восстановить исходное состояние БД после всех изменений в calculate2 в другой транзакции
                    //System.out.println("неудачная ветка3");
                    //System.out.println("saveValues2=" + saveValues.get(0).getValue());
                    allValues = this.findAll();
                    Collections.sort(allValues);
                    int counter1 = 0;
                    //System.out.println("+++");
//                    for (Value v : allValues) {
//                        counter1++;
//                        if (v.getValue() > 0) System.out.print(v.getValue());
//                        else System.out.print(".");
//                        if (counter1 == Math.sqrt(allValues.size())) {
//                            counter1 = 0;
//                            System.out.println();
//                        }
//                    }
                    //baseValueOfThisVariants ="";
                    v1 = null;

                    for (Value v : allValues) {
                        if (v.getVariants().length() == min) {
                            //baseValueOfThisVariants = v.getVariants();
                            v1 = v;
                            break;
                        }
                    }
                }
            }
        }
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

    private void calculate2(char c, String baseValueOfThisVariants, List<Value>  allValues, Value value){
        //Тут два состояния - или эту функцию вызвали из цикла for главной функции, или ее вызвали рекурсивно

        if (c!='r') { // значит функцию вызвали из цикла for


            boolean isOk = false; // понадобится в конце, чтобы определить нужна ли еще рекурсия

            value.setValue(Integer.parseInt(String.valueOf(c)));
            value.setVariants(value.getVariants().replaceAll(String.valueOf(c), ""));
            this.save(value);
            allValues = this.findAll();

            int[][] valuesToDelete = calculatePart1(allValues);

            calculatePart2(valuesToDelete, allValues);

            isOk = calculatePart3(false, allValues);



            // нашелся хотя бы один variants содержащий ровно одну цифру, которую подставили в итоговое значение -> пора пересчитывать рекурсивно
            if (isOk) {
                allValues = this.findAll();
                Collections.sort(allValues);
                int counter1=0;
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
//            Collections.sort(allValues);
//            System.out.println("---");
//            for (Value v: allValues) {
//                System.out.println(v.getValue()+" "+v.getVariants());
//            }
                //System.out.println("Пошла рекурсия2");
                calculate2('r', "", null, null); // рекурсия, повторяем процедуру вычислений
            }

            // тут мы окажемся, только если или все решено, или остались variants, содержащие более 1 цифры, или не сходится пазл
            allValues = this.findAll();
            Collections.sort(allValues);
            isOk=true;
            int counter1=0;
            for (Value v: allValues) {
                if (v.getValue()==0 && v.getVariants().isEmpty()) {isOk=true;counter1=1;break;} // получили не сходимость
                if (v.getValue()==0 && v.getVariants().length()>1) {isOk=false;} // получили, что есть варианты с variants больше одной цифры
            }

            // точно есть еще неопределенные элементы, у котороых variants содержат более одной цифры (неоднозначность), возвращаем БД в исходное состояние
            if (!isOk) {
               //this.saveAll(tempAllValues);
                //System.out.println("получили, что есть толко варианты с variants больше одной цифры");
            }

            // неудачный вариант, возвращаем БД в исходное состояние
            if (isOk && counter1 ==1) {
                //System.out.println("Ошибка в данных файла, невозможно получить ответ2 (функцию вызвали из цикла for)");
                //this.saveAll(tempAllValues);

            }
        }

        if (c=='r'){ // значит это рекурсивный вызов

            boolean isOk = false; // понадобится в конце, чтобы определить нужна ли еще рекурсия

            //value.setValue(Integer.parseInt(String.valueOf(c)));
            //value.setVariants(value.getVariants().replaceAll(String.valueOf(c), ""));
            //this.save(value);
            allValues = this.findAll();


            Collections.sort(allValues);
            //System.out.println("===");
            //for (Value v: allValues) System.out.println(v.getValue()+" "+v.getVariants());

            int[][] valuesToDelete = calculatePart1(allValues);

            calculatePart2(valuesToDelete, allValues);

            isOk = calculatePart3(false, allValues);



            // нашелся хотя бы один variants содержащий ровно одну цифру, которую подставили в итоговое значение -> пора пересчитывать рекурсивно
            if (isOk) {
                allValues = this.findAll();
                Collections.sort(allValues);
                int counter1=0;
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
//            Collections.sort(allValues);
//            System.out.println("---");
//            for (Value v: allValues) {
//                System.out.println(v.getValue()+" "+v.getVariants());
//            }
                //System.out.println("Пошла рекурсия2");
                calculate2('r', "", null, null); // рекурсия, повторяем процедуру вычислений
            }

            // тут мы окажемся, только если или все решено, или остались variants, содержащие более 1 цифры, или не сходится пазл
            Collections.sort(allValues);
            isOk=true;
            int counter1=0;
            for (Value v: allValues) {
                if (v.getValue()==0 && v.getVariants().isEmpty()) {isOk=true;counter1=1;break;} // получили не сходимость
                if (v.getValue()==0 && v.getVariants().length()>1) {isOk=false;} // получили, что есть варианты с variants больше одной цифры
            }

            // точно есть еще неопределенные элементы, у котороых variants содержат более одной цифры (неоднозначность), возвращаем БД в исходное состояние
            if (!isOk) {
               // this.saveAll(tempAllValues);
                //System.out.println("получили, что есть толко варианты с variants больше одной цифры");
            }

            // неудачный вариант, возвращаем БД в исходное состояние
            if (isOk && counter1 ==1) {
                //System.out.println("Ошибка в данных файла, невозможно получить ответ2");
               // this.saveAll(tempAllValues);
            }
        }

    }

    private int[][] calculatePart1(List<Value> allValues){



        int counter1 = 0;
        int counter2 = 0;


        // двумерный массив значений, которые надо вычищать из подходящих variants
        int[][] valuesToDelete = new int[(int)Math.sqrt(allValues.size())][(int)Math.sqrt(allValues.size())];

        // пробегаем по всем значениям из базы и собираем двумерный массив значений, которые надо вычистить из поля variants базы
        Collections.sort(allValues);
        //System.out.println();
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
        return valuesToDelete;
    }

    private void calculatePart2(int[][] valuesToDelete, List<Value> allValues) {
        int counter1 = 0;
        int counter2 = 0;
        // чистим поле variants у каждого значения равного нулю (т.е. незивестное)
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
        //System.out.println("чистка variants прошла успешно");
    }

    private boolean calculatePart3(boolean isOk, List<Value> allValues){
        // Проверяем есть ли variants состоящие из одной цифры, если есть - то вставляем их в нулевые value
        Collections.sort(allValues);
        for (Value v: allValues) {
            if (v.getValue()==0) {
                if (v.getVariants().isEmpty()) {
                    //System.out.println("Ошибка вычисления, некорректные данные в файле");
                    isOk = false;
                    break;
                }
                if (v.getVariants().length() == 1) {
                    v.setValue(Integer.parseInt(v.getVariants()));
                    v.setVariants("");
                    this.save(v);
                    isOk = true;
                }
                //if (v.getVariants().length() > 1) isOk = true; // остались variants содержащие больше 1ой цифры

            }
        }
        return isOk;
    }

}
