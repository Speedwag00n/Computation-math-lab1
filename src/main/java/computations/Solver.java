package computations;

public class Solver {

    private Data data;
    private double[][] solvableMatrix;

    public Solver(Data data) {
        this.data = data;
    }

    public void solve() {
        ConvergenceChecker checker = new ConvergenceChecker();

        //Проверка, получена ли ранее матрица, удовлетворяющая достаточному условию сходимости метода
        if (checker.check()) {
            double[] currentIterationValues = getInitialApproximations();
            double[] previousIterationValues = null;
            int iterationsCount = 0;
            boolean isSolved = false;
            //Производим итерации, пока не достигнем необходимой точности вычислений
            while (!isSolved) {
                previousIterationValues = currentIterationValues;
                currentIterationValues = iterate(currentIterationValues, solvableMatrix);
                isSolved = isSolved(currentIterationValues, previousIterationValues);
                iterationsCount++;
            }

            printResults(currentIterationValues, getErrors(currentIterationValues, previousIterationValues), iterationsCount);
        }
    }

    private double[] getInitialApproximations() {
        int n = solvableMatrix.length;
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            values[i] = solvableMatrix[i][n];
        }
        return values;
    }

    private double[] iterate(double[] previousIterationValues, double[][] matrix) {
        int n = previousIterationValues.length;
        double[] newValues = previousIterationValues.clone();

        for (int i = 0; i < newValues.length; i++) {
            double currentIterationSum = 0;
            double previousIterationSum = 0;
            for (int j = 0; j < newValues.length; j++) {
                if (j < i) {
                    //Учет вклада промежуточных значений, полученных на текущей итерации
                    currentIterationSum += matrix[i][j] * newValues[j];
                } else {
                    //Учет вклада промежуточных значений, полученных на предыдушей итерации
                    previousIterationSum += matrix[i][j] * newValues[j];
                }
            }
            //Получение нового промежуточного значения
            double newValue = (matrix[i][n] + currentIterationSum + previousIterationSum);
            newValues[i] = newValue;
        }

        return newValues;
    }

    private boolean isSolved(double[] currentIterationValues, double[] previousIterationValues) {
        int n = currentIterationValues.length;
        double maxError = 0;

        for (int i = 0; i < n; i++) {
            double currentError = Math.abs(currentIterationValues[i] - previousIterationValues[i]);
            //Ищем максимальную погрешность среди всех искомых переменных
            if (currentError > maxError) {
                maxError = currentError;
            }
        }
        //Проверяем, достигнута ли заданная точность вычислений
        if (maxError < data.getAccuracy()) {
            return true;
        } else {
            return false;
        }

    }

    private double[] getErrors(double[] currentIterationValues, double[] previousIterationValues) {
        int n = currentIterationValues.length;
        double[] errors = new double[n];

        for (int i = 0; i < n; i++) {
            errors[i] = Math.abs(currentIterationValues[i] - previousIterationValues[i]);
        }

        return errors;
    }

    private void printResults(double[] values, double[] errors, int iterationsCount) {
        System.out.printf("Получен ответ (количество итераций - %d):\n", iterationsCount);
        System.out.printf("%-32s|%-32s\n", "Решения", "Погрешности");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("%+-32.16f|%+-32.16f\n", values[i], errors[i]);
        }
    }

    private class ConvergenceChecker {

        private double[][] reducedMatrix;
        private double[][] transformedMatrix;
        private double[][] reducedTransformedMatrix;

        public boolean check() {
            double[][] matrix = data.getMatrix();
            try {
                reducedMatrix = makeReducedMatrix(matrix);
            } catch (MatrixTransformException e) {
                //Не удалось получить сокращенную матрицу. Далее будет произведена попытка получить сокращенную матрицу после достижения диагонального преобладания
                System.out.println(e.getMessage());
            }
            //Проверка достаточного условия сходимости
            if (reducedMatrix != null && isConverging(reducedMatrix)) {
                solvableMatrix = reducedMatrix;
                System.out.println("Изначальная матрица удовлетворяет условию сходимости.");
                return true;
            } else {
                System.out.println("Изначальная матрица не удовлетворяет условию сходимости. Поиск подходящих преобразований для достижения диагонального преобладания.");
                try {
                    //Производим поиск преобразований для достижения диагонального преобладания и находим сокращенную матрицу
                    transformedMatrix = transformMatrix();
                    reducedTransformedMatrix = makeReducedMatrix(transformedMatrix);
                    //Проверка достаточного условия сходимости
                    if (isConverging(reducedTransformedMatrix)) {
                        solvableMatrix = reducedTransformedMatrix;
                        System.out.println("После преобразований матрица удовлетворяет условию сходимости.");
                        return true;
                    } else {
                        System.out.println("После преобразований матрица так и не удовлетворяет условия сходимости. К сожалению, решение найти невозможно.");
                        return false;
                    }
                } catch (MatrixTransformException e) {
                    //Невозможно достичь диагонального преобладания
                    System.out.print(e.getMessage());
                    System.out.println(" К сожалению, решение найти невозможно.");
                    return false;
                }
            }
        }

        private boolean isConverging(double[][] reducedMatrix) {
            int n = data.getN();
            double maxSum = 0;
            for (int i = 0; i < n; i++) {
                double currentSum = 0;
                for (int j = 0; j < n; j++) {
                    currentSum += Math.abs(reducedMatrix[i][j]);
                }
                if (currentSum > maxSum) {
                    maxSum = currentSum;
                }
                if (!(maxSum < 1)) {
                    return false;
                }
            }
            return true;
        }

        private double[][] makeReducedMatrix(double[][] matrix) throws MatrixTransformException {
            int n = matrix.length;
            for (int i = 0; i < n; i++) {
                if (matrix[i][i] == 0) {
                    //Элемент на главной диагонали равен 0, дальнейшие вычисления сокращенной матрицы невозможны из-за деления на 0
                    throw new MatrixTransformException("Элемент на главной диагонали на строке #" + (i + 1) + " равен 0. Составить сокращенную матрицу невозможно.");
                }
            }
            double[][] reducedMatrix = new double[n][n+1];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n + 1; j++) {
                    if (i != j) {
                        if (j != n) {
                            reducedMatrix[i][j] = -1 * matrix[i][j] / matrix[i][i];
                        } else {
                            reducedMatrix[i][j] = matrix[i][j] / matrix[i][i];
                        }
                    } else {
                        reducedMatrix[i][j] = 0;
                    }
                }
            }
            return reducedMatrix;
        }

        private double[][] transformMatrix() throws MatrixTransformException {
            double[][] matrix = data.getMatrix();
            int n = matrix.length;
            int[] maxCoefficientIndexes = new int[n];

            //Ищем максимальные коэффициенты в каждой строке и запоминаем их
            for (int i = 0; i < n; i++) {
                double maxCoefficientValue = Math.abs(matrix[i][0]);
                int maxCoefficientIndex = 0;
                for (int j = 1; j < n; j++) {
                    if (Math.abs(matrix[i][j]) > maxCoefficientValue){
                        maxCoefficientValue = Math.abs(matrix[i][j]);
                        maxCoefficientIndex = j;
                    }
                }
                maxCoefficientIndexes[i] = maxCoefficientIndex;
            }

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != j){
                        //Проверяем, нет ли двух строк, где максимален коэффициент при одном и том же неизвестном
                        if (maxCoefficientIndexes[i] == maxCoefficientIndexes[j]){
                            //Есть есть две строки, где коэффициенты при одном и том же неизвестном максимальны, достичь диагонального преобладания перестановкой матрицу не представляется возможным
                            throw new MatrixTransformException("Достичь диагонального преобладания невозможно.");
                        }
                    }
                }
            }

            //Делаем перестановку строк, чтобы на главной диагонали находились наибольшие коэффициенты в каждой строке
            double[][] transformedMatrix = new double[n][n+1];
            for (int i = 0; i < n; i++) {
                int currentLineIndex = maxCoefficientIndexes[i];
                transformedMatrix[currentLineIndex] = matrix[i];
            }
            return transformedMatrix;
        }

    }

}
