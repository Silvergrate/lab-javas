# Лабораторна робота № 2

Технології розробки на платформі Java  
Гасюк Д.О., група ІО-41

## Варіант завдання

Номер залікової книжки — **4104**. Номер варіанта дорівнює остачі від ділення на 3. `4104 = 3 × 1368 + 0`, тому `4104 mod 3 = 0`.

**Варіант 0 — аналізатор серверних логів.**

## Опис проєкту

Клас [`LogAnalyzer`](src/main/java/ua/kpi/comsys/collections/LogAnalyzer.java) аналізує записи серверного журналу у форматі Common Log Format. Некоректні записи пропускаються.

- `getUniqueIps(logLines)` повертає множину унікальних IP-адрес.
- `getTopIps(logLines, topN)` повертає до `topN` адрес за спаданням частоти. За рівної частоти адреси сортуються лексикографічно за зростанням.

Проєкт використовує Java 21, Gradle Wrapper і JUnit 5. Версії тестових залежностей задані в [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Запуск тестів в IntelliJ IDEA

1. Вибрати **File → Open** і відкрити кореневу папку репозиторію, де розташований `build.gradle`.
2. Дочекатися синхронізації Gradle. За потреби в **Settings → Build, Execution, Deployment → Build Tools → Gradle** вибрати Gradle Wrapper і встановлений JDK 21 або 25 у полі **Gradle JVM**.
3. У **View → Tool Windows → Gradle** розгорнути **Tasks → verification** і двічі клацнути **test**. Якщо дерево задач приховане, натиснути **Execute Gradle Task** і вибрати задачу `test`.
4. Переглянути результати у вкладці **Run**. Повний звіт Gradle після запуску доступний у `build/reports/tests/test/index.html`.

[`LogAnalyzerTest`](src/test/java/ua/kpi/comsys/collections/LogAnalyzerTest.java) перевіряє обидва методи, сортування за частотою, порядок за рівних частот, обмеження `topN`, порожні списки та пропуск некоректних записів. Окремі тести перевіряють `IllegalArgumentException` і точні повідомлення для `null` замість списку та `topN <= 0`. Усі 27 тестових випадків пройшли успішно.

Для демонстраційного запуску відкрити [`Main.java`](src/main/java/ua/kpi/comsys/collections/Main.java) і натиснути зелену стрілку біля `main`.
