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

        Date date = new Date();
        long timeOfCalculate = date.getTime(); // зафиксировали время старта алгоритма

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

        // два варианта - или сходимость и надо вывести ответ или некорректные данные в файле - несходимость
        StringBuilder response = new StringBuilder("Файл загружен успешно. Решение найдено:\n");
        List<Value> allValues = this.findAll();
        Collections.sort(allValues);
        int counter1 = 0;
        for (Value v:allValues) {
            response.append(v.getValue());
            if (v.getValue()==0) {return "Файл загружен. Но решение невозможно получить, некорректные данные в файле";}
            counter1++;
            if (counter1==Math.sqrt(allValues.size())){
                counter1=0;
                response.append("\n");
            }
        }

        date = new Date();
        timeOfCalculate = date.getTime() - timeOfCalculate; // вычисление потраченного времени
        //System.out.println(timeOfCalculate);
        response.append("Количество миллисекуд на вычисления: ");
        response.append(timeOfCalculate);
        return response.toString();
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

        int[][] valuesToDelete = calculatePart1(allValues); // создаем двумерный массив значений, которые надо вычистить из подходящих variants

        calculatePart2(valuesToDelete, allValues); // чистим variants по горизонтали, вертикали и в секторе на основе двумерного массива

        isOk = calculatePart3(false, allValues); // если у нулевого (неизвестного) значения есть variants, содержащий только одну цифру, то вставляем в значение.
        // если  у нулевого (неизвестного) значения есть пустой variants - значит получили несходимость

        // нашелся хотя бы один variants содержащий ровно одну цифру, которую подставили в итоговое значение -> пора пересчитывать рекурсивно
        if (isOk) {
            printIterationSudoku(); // печатаем очередной просчитываемый вариант Sudoku
            calculate(); // рекурсия, повторяем процедуру вычислений
        }

        // тут мы окажемся, только если или все решено, или остались variants, содержащие более 1 цифры, или не сходится пазл
        Collections.sort(allValues);
        isOk=true;

        for (Value v: allValues) {
                if (v.getValue()==0 && v.getVariants().length()>1) {isOk=false;} // получили, что есть варианты с variants больше одной цифры
            System.out.println(v.getValue()+" "+v.getVariants());
        }

        // точно есть еще неопределенные элементы, у котороых variants содержат более одной цифры (неоднозначность)
        // начинает рекурсивно перебирать деревья вариантов
        if (!isOk) {
            System.out.println("Пошел перебор деревьев");calculate3(allValues);}

       // в итоге calculate или получит сходимость, или изначальные данные в файле не дают сходимость

    }

    private void printIterationSudoku(){
        List<Value> allValues = this.findAll();
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
    }

    private void calculate3(List<Value> allValues) { // сюда попадаем только если остались неизвестные значения с variants, содержащим более 1 цифры

        //найти variants с минимальным количеством цифр для оптимизации
        int min = (int) Math.sqrt(allValues.size());
        Collections.sort(allValues);
        for (Value v: allValues) {
            if (v.getValue()==0 && v.getVariants().length()<min) min = v.getVariants().length();
        }

        // сохраняем слепок изначальной матрицы Sudoku, т.к. дальше начнем ее изменять перебирая варианты
        String baseValueOfThisVariants =""; // переменная сохранит variants с минимальным кол-вом цифр для перебора
        Value v1 = null; // объект будет хранить указатель на нужный объект, который мы перебираем
        Collections.sort(allValues);
        int counter2 = 0;
        List<Value> saveValues2 = new ArrayList<>();
        for (Value v: allValues) {
            Value v3 = new Value();
            v3.setValue(v.getValue());
            v3.setVariants(v.getVariants());
            saveValues2.add(v3);
            if (v3.getVariants().length()==min && counter2==0){
                baseValueOfThisVariants = v.getVariants();
                //System.out.println("baseValueOfThisVariants= " + baseValueOfThisVariants);
                counter2=1;
                v1=v;
            }
        }

//        //
//        String baseValueOfThisVariants =""; // переменная сохранит variants с минимальным кол-вом цифр для перебора
//        Value v1 = null; // объект будет хранить указатель на нужный объект, который мы перебираем
//        Collections.sort(allValues);
//        for (Value v: allValues) {
//            if (v.getVariants().length()==min){
//                baseValueOfThisVariants = v.getVariants();
//                v1=v;
//               break;
//            }
//        }





        //System.out.println("base= " + baseValueOfThisVariants);


        // пробежаться в цикле - и подставлять одно значение за другим, пока не получим сходимость
        for (char c: baseValueOfThisVariants.toCharArray()) {

            int counter3=0;
            for (Value v : allValues) {
                if (v.getValue() == 0) {
                    counter3 = 1;  // тут могут быть два варианта или все сошлось или могут быть нули с соответствующими variants больше одной цифры
                    break;
                }
            }
            if (counter3==0) break; // мы нашли решение, нам больше не нужно проходить циклы и рекурсии

            //постоянно возвращаем старые данные при каждой итерации
            v1=null;
            counter2 = 0;
            int counter1 = 0;

            for (Value v : allValues) {
                v.setValue(saveValues2.get(counter2).getValue());
                v.setVariants(saveValues2.get(counter2).getVariants());
                counter2++;
                if (v.getVariants().length()==min && counter1==0){
                    baseValueOfThisVariants = v.getVariants();
                    //System.out.println("baseValueOfThisVariants= " + baseValueOfThisVariants);
                    counter1=1;
                    v1=v;
                }
            }

            List<Value> saveValues = calculate2(c, baseValueOfThisVariants, allValues, v1); // вновь вложенная рекурсия просчета варианта (3 исхода: сходимость, неопределенность, несходимость)

            counter2 = 0; // флажок, для определения - если есть сходимость, то выйти из цикла
            allValues = this.findAll();
            Collections.sort(allValues);
            for (Value v : allValues) {
                if (v.getValue() == 0) {
                    counter2 = 1;  // тут могут быть два варианта или все сошлось или могут быть нули с соответствующими variants больше одной цифры
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

                if (counter2 == 0) calculate3(allValues); // вновь рекурсия, требующая сделать слепок и  перебирать деревья

                if (counter2 == 1) { // ветка тупиковая, пора вернуть все исходные значения

//                    counter2 = 0;
//                    for (Value v : allValues) {
//                        v.setValue(saveValues.get(counter2).getValue());
//                        v.setVariants(saveValues.get(counter2).getVariants());
//                        counter2++;
//                    }
//
//                    v1 = null;
//                    for (Value v : allValues) {
//                        if (v.getVariants().length() == min) {
//                            //baseValueOfThisVariants = v.getVariants();
//                            v1 = v; // вновь возвращаем указатель на нужый нам объект для перебора, т.к. он мог измениться
//                            break;
//                        }
//                    }
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

    private List<Value> calculate2(char c, String baseValueOfThisVariants, List<Value>  allValues, Value value){
        //Тут два состояния - или эту функцию вызвали из цикла for главной функции, или ее вызвали рекурсивно
        List<Value> saveValues = new ArrayList<>();

        boolean isOk = false; // понадобится в конце, чтобы определить нужна ли еще рекурсия

        if (c!='r') { // значит функцию вызвали из цикла for
//            //надо сохранить БД
//            for (Value v: allValues) {
//                Value v2 = new Value();
//                v2.setValue(v.getValue());
//                v2.setVariants(v.getVariants());
//                saveValues.add(v2);
//            }
            // единственное отличие откуда вызвана функция. В данном случае надо вставить значение из перебираемого variants
            value.setValue(Integer.parseInt(String.valueOf(c)));
            value.setVariants(value.getVariants().replaceAll(String.valueOf(c), ""));
            this.save(value);





        }

        allValues = this.findAll();
        int[][] valuesToDelete = calculatePart1(allValues);
        calculatePart2(valuesToDelete, allValues);
        isOk = calculatePart3(false, allValues);

        //if (!isOk) for (Value v: allValues) System.out.println(v.getValue()+ " "+v.getVariants());

        // нашелся хотя бы один variants содержащий ровно одну цифру, которую подставили в итоговое значение -> пора пересчитывать рекурсивно
        if (isOk) {
                printIterationSudoku(); // печатаем очередной просчитываемый вариант Sudoku
                List<Value> tempValues = calculate2('r', "", null, null); // рекурсия, повторяем процедуру вычислений
            }

            // тут мы окажемся, только если или все решено, или остались variants, содержащие более 1 цифры, или не сходится пазл
            // в итоге функция calculate2 перестает себя вызывать, когда или получилась сходимость, или получилась неоднозначность, или получилась не сходимость
        return saveValues;

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

                }
                cleanSector(counter2+1, counter1, valuesToDelete.length, v, valuesToDelete); // удаляем пересекающиеся значения внутри сектора

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
