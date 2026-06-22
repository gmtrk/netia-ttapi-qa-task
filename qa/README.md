# Testy TTAPI - uruchomienie

Katalog `qa/` zawiera dwie warstwy testów oraz dokument strategii testów - [`strategia-testow.md`](strategia-testow.md).

## Struktura

| Ścieżka | Zawartość |
|---|---|
| `api-tests/` | Testy integracyjne API (Java 21 · JUnit 5 · REST Assured · AssertJ) - obszary `API`, `FLOW`, `SEC`, `DATA`, `CROSS` |
| `ui-tests/` | Testy E2E interfejsu (TypeScript · Playwright) - obszar `UI` |
| `allure/` | Konfiguracja wspólnego raportu Allure i skrypt `generate-report.sh` |
| `strategia-testow.md` | Strategia testów |
| `run-tests.sh` | Uruchamia obie warstwy i buduje wspólny raport |
| `credentials.env` | Hasło użytkowników testowych, wczytywane przez obie warstwy |

## Wymagania

- **Docker Engine 24+** i **Docker Compose v2** - do uruchomienia testowanego środowiska.
- **Node.js 20+** - do testów UI (Playwright) oraz do generowania raportu Allure.
- **Google Chrome** - testy UI korzystają z kanału `chrome` (instalacja: `npx playwright install chrome`).
- **Java (JRE 8+)** - do zbudowania raportu Allure.
- **JDK 21 + Gradle** - tylko do lokalnego uruchomienia testów API. Ścieżka domyślna (`run-tests.sh`) uruchamia je w obrazie `gradle:9.4.1-jdk21-ubi`.

## Krok 1 - uruchomienie środowiska

Testy działają na uruchomionym stacku (API, Keycloak, PostgreSQL, frontend). Z katalogu `docker/` w korzeniu repozytorium:

```bash
cd docker/
docker compose up -d --build
```

Adresy domyślne: API `http://localhost:8080`, Keycloak `http://localhost:8180`, frontend `http://localhost:3000`, PostgreSQL `localhost:5432`.

## Krok 2 - credentials

Obie warstwy odczytują hasło użytkowników testowych ze zmiennej `TTAPI_PASSWORD`. Plik `credentials.env` jest już wypełniony wartością ze środowiska Docker, więc poza domyślną konfiguracją nie trzeba nic ustawiać:

```
TTAPI_PASSWORD=Test1234!
```

Użytkownicy `alpha`, `beta`, `gamma` odpowiadają trzem tenantom i mają to samo hasło.

## Krok 3 - uruchomienie testów

### Wariant A - wszystko naraz

```bash
qa/run-tests.sh
```

Skrypt uruchamia testy API (w kontenerze Gradle), następnie testy UI (Playwright) i na końcu buduje wspólny raport Allure. Kontener API używa sieci hosta (`--network host`), aby sięgnąć stacku na `localhost`. Jeśli dany silnik Dockera nie wspiera takiego dostępu do usług hosta, testy API należy uruchomić lokalnie (Wariant B).

### Wariant B - warstwy osobno

**API** (lokalny Gradle, wymaga JDK 21; `credentials.env` jest wczytywany automatycznie):

```bash
cd qa/api-tests
gradle test
```

albo w kontenerze, bez lokalnego JDK:

```bash
docker run --rm --network host --env-file qa/credentials.env \
  -v "$PWD/qa/api-tests":/src:ro -v "$PWD/qa/api-tests/build/allure-results":/out \
  gradle:9.4.1-jdk21-ubi \
  sh -c "cp -r /src /home/gradle/proj && cd /home/gradle/proj && gradle test --no-daemon -Dallure.results.directory=/out"
```

**UI** (Playwright):

```bash
cd qa/ui-tests
npm install
npx playwright install chrome
npx playwright test
```

Pojedyncza test klasa: `npx playwright test login.spec.ts`. 
Tryb z oknem przeglądarki: `npm run test:headed`.

## Raport

Wspólny raport Allure (API + UI) buduje się skryptem:

```bash
qa/allure/generate-report.sh
```

Wynik trafia do `qa/allure/report/index.html`. Otworzyć go można poleceniem wypisanym przez skrypt (`allure open …`). 
Testy UI mają dodatkowo własny raport: `npx playwright show-report` (z katalogu `qa/ui-tests`).

## Interpretacja wyników

Część testów **nie przechodzi** - wynika to z bugów w aplikacji. Nie są obchodzone w kodzie; oznaczono je adnotacją `@Tag("defect")`, a w raporcie Allure grupują się w kategorii **„Known application defects (spec divergence)"**.
Dlatego **niezerowy kod wyjścia warstwy API jest oczekiwany** i nie oznacza błędu uruchomienia.

Kategoria **„Test infrastructure problems"** (status `broken`) sygnalizuje problem, który wymaga naprawy środowiska.

## Konfiguracja (nadpisywanie domyślnych wartości)

Wszystkie adresy mają domyślne wartości wskazujące na `localhost` i zwykle nie wymagają zmian.
W razie potrzeby można je nadpisać zmiennymi środowiskowymi (warstwa API przyjmuje też właściwości systemowe `-Dttapi.*`):

| Zmienna | Domyślnie | Warstwa |
|---|---|---|
| `TTAPI_PASSWORD` | (wymagane, z `credentials.env`) | API + UI |
| `TTAPI_API_URL` | `http://localhost:8080` | API |
| `TTAPI_KEYCLOAK_URL` | `http://localhost:8180` | API |
| `TTAPI_DB_URL` | `jdbc:postgresql://localhost:5432/rest_db` | API |
| `TTAPI_UI_URL` | `http://localhost:3000` | UI |
