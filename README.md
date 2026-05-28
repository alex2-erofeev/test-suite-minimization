# Test Suite Minimization — Руководство по запуску экспериментов

Данное руководство описывает полный цикл эксперимента: запуск PIT до минимизации,
сбор покрытия по тестам, минимизация набора тестов, и запуск PIT после минимизации
для сравнения mutation score.

---

## Требования

- Java 17+
- Maven 3.9+
- Проект `fault-localization` собран и установлен в локальный Maven-репозиторий
- Исследуемый проект (jsoup, jheats и др.) с добавленными зависимостями (см. ниже)

---

## Подготовка исследуемого проекта

В `pom.xml` исследуемого проекта (например, `jsoup-master/pom.xml`) добавьте
в секцию `<dependencies>`:

```xml
<!-- Ваш инструмент минимизации -->
<dependency>
  <groupId>ru.erofeev</groupId>
  <artifactId>fault-localization</artifactId>
  <version>1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>

<!-- JaCoCo-агент для сбора покрытия -->
<dependency>
  <groupId>org.jacoco</groupId>
  <artifactId>org.jacoco.agent</artifactId>
  <version>0.8.11</version>
  <classifier>runtime</classifier>
  <scope>test</scope>
</dependency>
```

В секцию `<build><plugins>` добавьте плагин PIT:

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>1.17.3</version>
  <dependencies>
    <dependency>
      <groupId>org.pitest</groupId>
      <artifactId>pitest-junit5-plugin</artifactId>
      <version>1.2.3</version>
    </dependency>
  </dependencies>
  <configuration>
    <failWhenNoMutations>false</failWhenNoMutations>
    <parseSurefireConfig>false</parseSurefireConfig>
    <timestampedReports>false</timestampedReports>
  </configuration>
</plugin>
```

Проверить версию JUnit: `Select-String -Path pom.xml -Pattern "junit"`
Если проект использует **JUnit 4** (`junit:junit:4.x`), дополнительно добавьте:

```xml
<dependency>
  <groupId>org.junit.vintage</groupId>
  <artifactId>junit-vintage-engine</artifactId>
  <version>5.11.4</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.junit.platform</groupId>
  <artifactId>junit-platform-launcher</artifactId>
  <version>1.11.4</version>
  <scope>test</scope>
</dependency>
```
---

## Сборка fault-localization

Выполните **один раз** перед началом экспериментов.

> Папка: `fault-localization/`

```powershell
mvn clean install -DskipTests
```

---

## Параметры эксперимента

Перед запуском задайте параметры в PowerShell:

```powershell
# Классы production-кода для мутирования
$targetClasses = "org.jheaps.monotone.*"

# Маска тест-классов (glob)
$beforeTests = "org.jheaps.monotone.*Test"

# Алгоритм минимизации: GREEDY_ESSENTIAL | NAIVE | GENETIC | GENETIC_JENETICS
$algorithm = "GREEDY_ESSENTIAL"


# Количество потоков PIT (по числу ядер CPU)
$threads = 16

# Таймауты PIT
$timeoutFactor  = 1.5
$timeoutConstant = 3000

# --- Служебные переменные (не менять) ---
$algorithmDir = switch ($algorithm.ToUpper()) {
  "NAIVE"             { "naive" }
  "GREEDY_ESSENTIAL"  { "greedy-essential" }
  "GENETIC"           { "genetic" }
  "GENETIC_JENETICS"  { "genetic-jenetics" }
  default             { throw "Unsupported algorithm: $algorithm" }
}


$includeClassRegex = '^(' + (($beforeTests -split ',' | ForEach-Object { $_.Trim() -replace '\.', '[.]' -replace '\*', '.*'}) -join '|') + ')$'

# Полный путь к локальному Maven-репозиторию (обычно менять не нужно)
$m2 = "$env:USERPROFILE\.m2\repository"

#Используйте **Вариант Б** для JUnit 4

# --- Вариант А: проект использует JUnit 5 (jupiter) ---
$cp = "target/test-classes;target/classes;" +
  "$m2\ru\erofeev\fault-localization\1.0-SNAPSHOT\fault-localization-1.0-SNAPSHOT.jar;" +
  "$m2\org\jacoco\org.jacoco.agent\0.8.11\org.jacoco.agent-0.8.11-runtime.jar;" +
  "$m2\org\jacoco\org.jacoco.core\0.8.11\org.jacoco.core-0.8.11.jar;" +
  "$m2\org\junit\platform\junit-platform-launcher\1.11.4\junit-platform-launcher-1.11.4.jar;" +
  "$m2\org\junit\platform\junit-platform-engine\1.14.3\junit-platform-engine-1.14.3.jar;" +
  "$m2\org\junit\platform\junit-platform-commons\1.14.3\junit-platform-commons-1.14.3.jar;" +
  "$m2\org\junit\jupiter\junit-jupiter\5.14.3\junit-jupiter-5.14.3.jar;" +
  "$m2\org\junit\jupiter\junit-jupiter-api\5.14.3\junit-jupiter-api-5.14.3.jar;" +
  "$m2\org\junit\jupiter\junit-jupiter-engine\5.14.3\junit-jupiter-engine-5.14.3.jar;" +
  "$m2\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar"

# --- Вариант Б: проект использует JUnit 4 ---
$cp = "target/test-classes;target/classes;" +
  "$m2\ru\erofeev\fault-localization\1.0-SNAPSHOT\fault-localization-1.0-SNAPSHOT.jar;" +
  "$m2\org\jacoco\org.jacoco.agent\0.8.11\org.jacoco.agent-0.8.11-runtime.jar;" +
  "$m2\org\jacoco\org.jacoco.core\0.8.11\org.jacoco.core-0.8.11.jar;" +
  "$m2\org\junit\platform\junit-platform-launcher\1.11.4\junit-platform-launcher-1.11.4.jar;" +
  "$m2\org\junit\platform\junit-platform-engine\1.11.4\junit-platform-engine-1.11.4.jar;" +
  "$m2\org\junit\platform\junit-platform-commons\1.11.4\junit-platform-commons-1.11.4.jar;" +
  "$m2\org\junit\vintage\junit-vintage-engine\5.11.4\junit-vintage-engine-5.11.4.jar;" +
  "$m2\junit\junit\4.13.1\junit-4.13.1.jar;" +
  "$m2\org\hamcrest\hamcrest-core\1.3\hamcrest-core-1.3.jar;" +
  "$m2\org\junit\jupiter\junit-jupiter\5.11.4\junit-jupiter-5.11.4.jar;" +
  "$m2\org\junit\jupiter\junit-jupiter-api\5.11.4\junit-jupiter-api-5.11.4.jar;" +
  "$m2\org\junit\jupiter\junit-jupiter-engine\5.11.4\junit-jupiter-engine-5.11.4.jar;" +
  "$m2\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar"

$jacocoAgent = "$m2\org\jacoco\org.jacoco.agent\0.8.11\org.jacoco.agent-0.8.11-runtime.jar"
```

---

## Шаг 0. Очистка старых результатов

> Папка: `jsoup-master/` (или папка исследуемого проекта)

```powershell
Remove-Item -Recurse -Force target\minimizer, target\pit-reports -ErrorAction SilentlyContinue
```

---

## Шаг 1. Компиляция

> Папка: `jsoup-master/`

```powershell
mvn -DskipTests test-compile "-Danimal.sniffer.skip=true" "-Djapicmp.skip=true" "-Dmaven.javadoc.skip=true"
```

> Флаги `-Danimal.sniffer.skip=true` и `-Djapicmp.skip=true` нужны, если при компиляции возникает ошибка.
> Для других проектов достаточно `mvn -DskipTests test-compile`.

---

## Шаг 2. PIT BEFORE (mutation testing до минимизации)

> Папка: `jsoup-master/`

```powershell
mvn org.pitest:pitest-maven:mutationCoverage "-DtargetClasses=$targetClasses" "-DtargetTests=$beforeTests" "-DreportsDirectory=target/pit-reports/before" -DtimestampedReports=false "-Dthreads=$threads" "-DtimeoutFactor=$timeoutFactor" "-DtimeoutConstant=$timeoutConstant"
```

Результат: `target/pit-reports/before/index.html`

---

## Шаг 3. Сбор покрытия по каждому тесту (collect-full)

> Папка: `jsoup-master/`

> Этот шаг запускается **напрямую через java**, а не через Maven, потому что требует дочернюю JVM с JaCoCo-агентом и полным classpath.

```powershell
java "-javaagent:$jacocoAgent=output=none,dumponexit=false,includes=$($targetClasses -replace '\.\*','.*')" `
  "-Dminimizer.classPattern=$includeClassRegex" `
  "-Dminimizer.classesDir=target/classes" `
  "-DMINIMIZER_JACOCO_RELAUNCHED=1" `
  -cp $cp `
  ru.erofeev.fl.pipeline.MinimizerPipelineMain collect-full
```

Результат: `target/minimizer/full-run/tests.csv`

> **Для доверительных интервалов**: шаги 0–3 делаются **один раз**.
> Затем **20 раз повторяется только шаг 4**.

---

## Шаг 4. Минимизация

> Папка: `jsoup-master/`

```powershell
mvn org.codehaus.mojo:exec-maven-plugin:3.6.1:java "-Dexec.classpathScope=test" "-Dexec.mainClass=ru.erofeev.fl.pipeline.MinimizerPipelineMain" "-Dexec.args=minimize" "-Dminimizer.algorithm=$algorithm" "-Dminimizer.metric=METHOD" "-Dminimizer.classesDir=target/classes"
```

Результаты в `target/minimizer/<algorithmDir>/heuristic-summary.txt`:

```
# Для доверительных интервалов фиксируются:
# t   = Heuristic solve ms       (время работы алгоритма)
# CPU = Peak CPU %               (пиковая нагрузка процессора)
# RAM = Peak RAM MB (heap used)  (пиковое использование кучи)
```

---

## Шаг 5. Генерация Min-обёрток для PIT AFTER

> Папка: `jsoup-master/`

```powershell
mvn org.codehaus.mojo:exec-maven-plugin:3.6.1:java "-Dexec.classpathScope=test" "-Dexec.mainClass=ru.erofeev.fl.pitest.PitestMinimizedWrappersMain" "-Dminimizer.algorithm=$algorithm" "-Dminimizer.pit.includeClassRegex=$includeClassRegex"
```

---

## Шаг 6. Чтение списка wrapper-тестов

> Папка: `jsoup-master/`

```powershell
$afterTests      = (Get-Content "target/minimizer/pit/$algorithmDir-target-tests-minwrappers.txt" -Raw).Trim()
$selectedIdsFile = "target/minimizer/$algorithmDir/selected-test-ids.txt"
```

---

## Шаг 7. PIT AFTER (mutation testing после минимизации)

> Папка: `jsoup-master/`

```powershell
mvn org.pitest:pitest-maven:mutationCoverage "-DtargetClasses=$targetClasses" "-DtargetTests=$afterTests" "-DjvmArgs=-Dminimizer.pit.selectedIdsFile=$selectedIdsFile,-Dminimizer.algorithm=$algorithm" "-DreportsDirectory=target/pit-reports/after" -DtimestampedReports=false "-Dthreads=$threads" "-DtimeoutFactor=$timeoutFactor" "-DtimeoutConstant=$timeoutConstant"
```

Результат: `target/pit-reports/after/index.html`

---

## Итоговые результаты

| Метрика | Файл |
|---|---|
| Mutation score ДО | `target/pit-reports/before/index.html` |
| Mutation score ПОСЛЕ | `target/pit-reports/after/index.html` |
| Кол-во тестов ДО | `target/minimizer/full-run/tests.csv` |
| Кол-во тестов ПОСЛЕ | `target/minimizer/<algorithmDir>/heuristic-summary.txt` → `selected_tests=` |
| Покрытие % | `heuristic-summary.txt` → `reported_coverage_percent=` |
| Время минимизации | `heuristic-summary.txt` → `solve_ms=` |
| Пиковый RAM | `heuristic-summary.txt` → `peak_heap_mb=` |
| Пиковый CPU | `heuristic-summary.txt` → `peak_cpu_percent=` |

---

## Доверительные интервалы

Для получения статистически значимых результатов шаг 4 (минимизация) повторяется **20 раз**.
Шаги 0–3 при этом **не повторяются** — матрица покрытия строится один раз.

---

## Пример вывода шага 4 (minimize)

```
Minimize input tests: total=441, success=441, failed=0, aborted=0, skipped=0
Minimizer algorithm: GREEDY_ESSENTIAL
Matrix size: tests=441, elements=1526, metric=METHOD
Heuristic algorithm: Greedy Essential (GREEDY_ESSENTIAL)
Heuristic selected tests: 126
Heuristic reported coverage %: 100.00
Matrix build ms: 9300
Heuristic solve ms: 124
Peak RAM MB (heap used): 366.42
Peak RAM MB (process committed virtual): 781.84
Peak CPU % (process, sampled): 36.92
Resource sample interval ms: 200
```

# Авторы проекта
Выполнили Ерофеев А.А., Кузьмин В.А.
Научный руководитель: Пархоменко В.А.
Выполнено в рамках учебного курса "Методы тестирования программного обеспечения"
Преподаватель: Пархоменко В.А.
Санкт-Петербургский Политехнический университет Петра Великого, 2026
