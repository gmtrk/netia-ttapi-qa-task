# Strategia testów - Trouble Ticket API (TTAPI)

| | |
|---|---|
| **Autor** | Jakub Gmiterek |
| **Data** | 19.06.2026 |
| **Aplikacja** | Trouble Ticket API - system zarządzania zgłoszeniami serwisowymi |
| **Architektura** | REST API (Spring Boot 4, Java 21) · SPA (React 19, TypeScript, MUI) · Keycloak (OAuth2/OIDC) · PostgreSQL 18 |
| **Zakres dokumentu** | Strategia testów: obszary, priorytety, podejście, ryzyka, katalog scenariuszy |
| **Wersja API** | v1 (`/api/v1`) |

## Wprowadzenie

Trouble Ticket API to wielodostępowy (multi-tenant) system obsługi zgłoszeń. Klient (konto API lub użytkownik) uwierzytelnia się tokenem JWT z Keycloak, a zakres widocznych danych wyznacza wyłącznie claim `tenant_id` w tokenie. API udostępnia pięć operacji: utworzenie zgłoszenia, listowanie, pobranie pojedynczego zgłoszenia, zamknięcie i dodanie notatki. Frontend (SPA) konsumuje to API i odwzorowuje jego reguły w warstwie UI.

Poniżej opisano podejście do testowania tego systemu: które obszary są brane pod uwagę, jak ustala się priorytety względem ryzyka, jakich technik się używa oraz jakie scenariusze (pozytywne i negatywne) są proponowane.

### Podejście oparte na specyfikacji (źródło prawdy)

Testy projektowane są metodą black-box. Źródłem oczekiwanego zachowania jest specyfikacja (OpenAPI `trouble-ticket-api.yaml` oraz `TASK.md`), a sama aplikacja to system testowany (SUT). Jeśli implementacja rozjeżdża się ze specyfikacją, jest to traktowane jako defekt: test ma prawo (i powinien) nie przejść. Nie jest to zachowanie, które należałoby odtworzyć w teście.

Gdy źródła są ze sobą sprzeczne, stosuje się pierwszeństwo. `TASK.md` rozstrzyga semantykę biznesową (reguły, dozwolone dane, katalog kodów błędów), a OpenAPI - kontrakt techniczny (struktury żądań i odpowiedzi, kody HTTP, formaty). Sam taki konflikt jest dodatkowo zgłaszany jako defekt dokumentacji.

---

## 1. Identyfikacja obszarów do testowania

Aplikację podzielono na obszary ryzyka. Dla każdego opisano, co podlega sprawdzeniu i dlaczego ten obszar jest istotny.

**`API` - kontrakt warstwy REST.** Zgodność z OpenAPI 3.1: struktury żądań i odpowiedzi, kody HTTP (200/201/400/401/403/404), nagłówki (`Location` przy 201, `Content-Type`), walidacja wejścia (pola wymagane, `minLength`, `serviceId ≥ 1`, enum statusów, `additionalProperties: false`) oraz jednolity format błędu (`code`, `message`, `requestId`). To publiczny kontrakt integracyjny, więc każda rozbieżność z dokumentacją uderza wprost w klientów, którzy z niego korzystają.

**`FLOW` - reguły biznesowe i cykl życia zgłoszenia.** Przejścia statusów (zamknięcie wyłącznie z `acknowledged`/`inProgress` -> `closed`, pozostałe -> `STATUS_TRANSITION_ERROR`), reguły notatek (blokada dla `resolved`/`closed`/`rejected` -> `NOTE_ADDITION_NOT_ALLOWED`), idempotencja tworzenia (`(tenantId, externalId)` -> 200 zamiast 201), ustalanie statusu początkowego oraz automatyczna notatka przy zamknięciu. To główna logika domeny; błąd w tym miejscu prowadzi do niespójnego stanu zgłoszeń.

**`SEC` - multi-tenancy i bezpieczeństwo.** Izolacja danych między tenantami (dostęp do cudzego zasobu -> 404, nie 403), lista ograniczona do własnego tenanta, uwierzytelnianie (brak, niepoprawny lub wygasły token -> 401), autoryzacja (token bez claimu `tenant_id` -> 403) oraz konfiguracja (stateless, wyłączony CSRF, CORS, ochrona endpointów przy publicznym Swaggerze). Najważniejszy z obszarów: wyciek danych między operatorami to ryzyko jednocześnie biznesowe i prawne.

**`UI` - frontend (SPA React).** Logowanie przez Keycloak (przekierowanie, odświeżanie tokenu), lista zgłoszeń (render, oznaczenia statusów), formularz tworzenia (walidacja kliencka, obsługa 200/201/400), widok szczegółów (historia notatek, akcje „zamknij" i „dodaj notatkę" oraz ich dostępność zależna od statusu), obsługa stanów ładowania i błędów, routing. To warstwa, z którą styka się użytkownik, więc musi wiernie odwzorować reguły backendu.

**`DATA` - warstwa danych i dane testowe.** Ograniczenia integralności wynikające z modelu (unikalność `(tenant_id, external_id)` jako podstawa idempotencji, dozwolony zbiór statusów, wartości niepuste, `serviceId` z dozwolonego zakresu), spójność danych zwracanych przez API z modelem zgłoszenia oraz niezależność i powtarzalność danych testowych (zgodnie z TASK.md dopuszcza się własne fixtures i nie zakłada konkretnej liczby ani treści danych przykładowych). Integralność danych pilnuje spójności na samym końcu, a zakładanie z góry konkretnych danych przykładowych prowadzi do niestabilnych testów.

**`CROSS` - zagadnienia przekrojowe.** Jednolity format i katalog kodów błędów, korelacja żądań przez pole `requestId`, wersjonowanie API (`/api/v1`), dostępność i poprawność dokumentacji (Swagger UI bez uwierzytelnienia, serwowany plik OpenAPI) oraz zgodność specyfikacji z implementacją. Decyduje o spójności operacyjnej i łatwości diagnozy; rozjazd między specyfikacją a implementacją generuje dług techniczny i myli integratorów.

Poza zakresem v1 pozostają testy performance/load, pełny pentest oraz testy samej infrastruktury (Docker/Keycloak) poza tym, co konieczne do uruchomienia środowiska. Reguły uwierzytelniania samego Keycloak (np. polityka haseł) traktujemy jak tooling - sprawdzamy wyłącznie reakcję aplikacji na nieudane logowanie (brak dostępu do UI). Uzasadnienie podano przy priorytetyzacji.

---

## 2. Priorytetyzacja

Priorytety ustalane są podejściem risk-based, w przybliżeniu Risk ≈ Impact × Exposure, gdzie Impact to dotkliwość skutku, a Exposure - prawdopodobieństwo i częstość użycia danej ścieżki. Uwzględniany jest też koszt późnego wykrycia błędu. Punktem odniesienia pozostaje specyfikacja (OpenAPI + TASK.md). Skala priorytetów: P0 - krytyczny, P1 - wysoki, P2 - średni, P3 - niski.

| Priorytet | Wymaganie (wg specyfikacji) | Uzasadnienie |
|---|---|---|
| **P0** | `SEC` - Izolacja tenantów: zasób innego tenanta -> 404; lista ograniczona do własnego tenanta | Wyciek danych między operatorami ma bardzo poważne skutki, a ścieżka jest aktywna przy każdym żądaniu. |
| **P0** | `SEC` - Uwierzytelnianie/autoryzacja: brak/niepoprawny token -> 401; token bez claimu `tenant_id` -> 403 | Granica bezpieczeństwa; obejście oznacza nieuprawniony dostęp. |
| **P0** | `FLOW` - Zamknięcie wyłącznie z `acknowledged`/`inProgress` -> `closed`; z innych statusów -> `STATUS_TRANSITION_ERROR` (400) | Główna logika domeny; błędne przejście psuje stan i historię zgłoszenia. |
| **P0** | `UI` - Krytyczna ścieżka UI (smoke): logowanie przez Keycloak + wyświetlenie listy zgłoszeń | Bez tego użytkownik nie wejdzie do aplikacji; awaria oznacza brak dostępu do całego UI. |
| **P1** | `FLOW` - Notatki niedozwolone w `resolved`/`closed`/`rejected` -> `NOTE_ADDITION_NOT_ALLOWED` (400) | Integralność cyklu życia; operacja częsta. |
| **P1** | `FLOW` - Idempotencja `(tenantId, externalId)`: ponowne utworzenie -> 200 z istniejącym zasobem (nie 201) | Reguła integracyjna; błąd -> duplikaty lub błędne kody u klienta. |
| **P1** | `FLOW` - Tworzenie: dozwolony wyłącznie status `new`; po utworzeniu status `acknowledged` | Poprawność ścieżki tworzenia; wysoka ekspozycja. |
| **P1** | `API` - Walidacja wejścia i ścieżki 4xx: pola wymagane, `minLength`, `serviceId` z dozwolonego zbioru (-> `SERVICE_NOT_FOUND`), enumy statusów, `additionalProperties: false`, `VALIDATION_ERROR` | Publiczny kontrakt; negatywne ścieżki są obowiązkowym elementem oceny. |
| **P1** | `UI` - Przepływy funkcjonalne UI (tworzenie, zamknięcie, dodanie notatki) + obsługa błędów i stanów | Defekt blokuje pojedynczą funkcję; reguły poprawnościowe egzekwowane i testowane na poziomie API. |
| **P2** | `DATA` - Spójność danych i ograniczenia (unikalność, dozwolone statusy, wartości niepuste) | Obrona w głąb; częściowo pokryte testami `API`/`FLOW`. |
| **P2** | `CROSS` - Zgodność implementacji ze specyfikacją: format błędu (`code`/`message`/`requestId`), korelacja diagnostyczna, kody HTTP | Diagnostyka i dług techniczny; niżej niż poprawność funkcjonalna. |
| **P3** | `CROSS` - Dostępność Swagger/OpenAPI, powierzchnia wersjonowania (`/api/v1`) | Funkcja wspierająca, nie ścieżka krytyczna. |
| **P3** | `UI` - Detale kosmetyczne UI | Niski wpływ; **exploratory testing**. |

Wyłączenia z zakresu: testy performance/load (brak zdefiniowanego SLA), pełny pentest (osobna, specjalistyczna ścieżka) oraz testy infrastruktury (środowisko jest dostarczone i sprawdzane przy uruchomieniu). Przedmiotem testów pozostaje aplikacja zestawiona ze specyfikacją.

---

## 3. Podejście do testowania

### Poziomy testów (dostosowana test pyramid)

Testowane jest uruchomione środowisko metodą black-box wobec specyfikacji, więc klasyczna test pyramid trochę się przesuwa. Szeroką podstawą są tu integration testy (API), w które wplecione są przekrojowe sprawdzenia kontraktu (zgodność odpowiedzi i katalogu błędów ze schematem), a na wierzchołku zostaje cienka warstwa E2E UI. Unit testy pozostają po stronie deweloperów, bo kod wewnętrzny jest poza zakresem testów black-box.

| Poziom | Cel | Obszary | Narzędzia |
|---|---|---|---|
| **Integration / API** (podstawa) | Reguły biznesowe, walidacja, kody i format błędów, izolacja tenantów - wobec uruchomionego API | `API`, `FLOW`, `SEC`, `CROSS` | Java 21 · JUnit 5 · REST Assured · AssertJ |
| **Contract** (przekrojowo, w ramach integration) | Zgodność odpowiedzi, formatu błędu i kodów HTTP z OpenAPI 3.1 - sprawdzana asercjami w suicie `CROSS` | `API`, `CROSS` | REST Assured · AssertJ (asercje ręczne) |
| **E2E UI** (wierzchołek) | Krytyczne ścieżki użytkownika i odwzorowanie reguł backendu w UI | `UI` | TypeScript · Playwright |

Uwaga: automatyczny walidator kontraktu (swagger-request-validator) był rozważany, ale przy obecnym stanie aplikacji odrzucony - na OpenAPI 3.1 swagger-parser potrafi po cichu pominąć `additionalProperties: false`, więc ukryłby defekt TC-API-09. Zgodność ze schematem weryfikowana jest wprost w suicie `CROSS`.

### Techniki projektowania przypadków (test design techniques)

- Equivalence partitioning i boundary value analysis - np. `serviceId` na granicach dozwolonego zbioru, `minLength` pól tekstowych, wartości puste i białe znaki.
- Decision table - macierz status × akcja (zamknięcie, dodanie notatki), żeby systematycznie pokryć dozwolone i zabronione kombinacje.
- State transition testing - model cyklu życia zgłoszenia, z weryfikacją przejść dozwolonych i niedozwolonych (zwłaszcza `-> closed`).
- Negative testing - dla każdego wymagania co najmniej jedna ścieżka błędu (4xx) z asercją kodu z katalogu błędów.

### Zasady realizacji

- Black-box ze specyfikacją jako źródłem prawdy; rozbieżność implementacji to defekt.
- Niezależność i powtarzalność: każdy test tworzy własne dane (fixtures), jest idempotentny i sprząta po sobie (teardown). Brak zależności między testami i od kolejności wykonania.
- Pokrycie pozytywne i negatywne: sprawdzana jest ścieżka poprawna oraz błędne, przy czym te negatywne są obowiązkowe.
- Asercje wielowarstwowe: kod HTTP, body (schema i wartości) oraz nagłówki (`Location`, `Content-Type`).
- Tożsamość i tenant: tokeny pobierane są programowo z Keycloak (ROPC grant) przez wspólny helper; testy izolacji używają tokenu jednego tenanta wobec zasobu drugiego.
- Data-driven: macierze statusów i walidacji jako parameterized tests.
- Środowisko: testy wobec `docker-compose` (API `:8080`, Keycloak `:8180`, UI `:3000`).

---

## 4. Ryzyka i wyzwania

Poniżej ryzyka dla samego procesu testowego wraz ze sposobami ich ograniczenia.

- **R1 · Niejednoznaczności i konflikty specyfikacji** (TASK.md vs OpenAPI, np. zakres `serviceId`, warunkowe „może" przy statusie początkowym)
    - *Wpływ:* niejasna specyfikacja prowadzi do spornych wyników testów.
    - *Mitygacja:* reguła pierwszeństwa (TASK.md dla semantyki, OpenAPI dla kontraktu), rejestr niejednoznaczności z jawnymi założeniami, eskalacja do PM.
- **R2 · Ograniczona controllability stanów** - statusy `inProgress`/`resolved`/`rejected` są nieosiągalne przez publiczne API (wg specyfikacji to przejścia systemowe, poza zakresem create/close)
    - *Wpływ:* część przejść i blokad trudno wymusić samym API.
    - *Mitygacja:* seeding przez DB w test setup (świadomie grey-box, wyłącznie do przygotowania danych); ten sam seed służy potem jako read-only fixtures.
- **R3 · Niedeterminizm i narastanie danych** - brak operacji DELETE w API, zmienne dane seed
    - *Wpływ:* flaky tests i rosnący stan bazy.
    - *Mitygacja:* własne fixtures z unikalnym `externalId` (prefiks plus znacznik czasu/UUID), brak asercji na danych seed, reset bazy (`docker compose down -v`) między przebiegami.
- **R4 · Zależność od Keycloak** - dostępność, czas startu (~30-60 s, zależnie od sprzętu), wygasanie tokenów (`accessTokenLifespan` = 1 h, więc token nie wygaśnie samoistnie w trakcie typowej serii), wymagany ROPC grant
    - *Wpływ:* flakiness i fałszywe 401 - głównie przy niegotowym Keycloak, nie przez samoistne wygaśnięcie tokenu.
    - *Mitygacja:* wait-for-ready lub health check przed serią, helper pobierający token (odświeżanie zwykle zbędne przy 1-godzinnym tokenie), dedykowani użytkownicy testowi (`alpha`/`beta`/`gamma`).
- **R5 · Kruchość E2E UI** - logowanie przez Keycloak (redirect, formularz), odświeżanie tokenu w SPA
    - *Wpływ:* niestabilne testy UI.
    - *Mitygacja:* programmatic login lub wstrzyknięty storageState, stabilne selektory (`data-testid`), trace i screenshot przy błędzie, dane przygotowane przez API.
- **R6 · Współdzielony stan bazy i kolejność testów**
    - *Wpływ:* interferencje między testami.
    - *Mitygacja:* pełna izolacja danych per test, brak zależności od kolejności, równoległość tylko przy rozłącznych danych.

### Rejestr zidentyfikowanych niejednoznaczności specyfikacji

- **Zakres `serviceId`:** OpenAPI dopuszcza `≥ 1` (bez górnej granicy), TASK.md ogranicza do `100001-100030` z błędem `SERVICE_NOT_FOUND`. -> *Przyjęto: rozstrzyga TASK.md; `serviceId` spoza zbioru -> 404.*
- **Status początkowy:** dokumenty mówią, że po utworzeniu system „może" ustawić `acknowledged` (sformułowanie warunkowe). -> *Przyjęto: oczekiwany status po utworzeniu = `acknowledged`.*
- **Status `new`:** zdefiniowany w enumie i dozwolony na wejściu `create`, lecz brak ścieżki, w której zostaje utrwalony w odpowiedzi. -> *Do potwierdzenia z PM.*
- **`additionalProperties: false` (pola nadmiarowe):** kontrakt dopuszcza wyłącznie pola ze schematu - żądanie z dodatkowym/nieznanym polem musi zostać odrzucone (`VALIDATION_ERROR`). -> *Oczekiwanie wynika z kontraktu i pozostaje wiążące; jeśli aplikacja ignoruje nadmiarowe pola, jest to defekt (nieobchodzony w teście).*

---

## 5. Propozycja scenariuszy testowych

Legenda: `[priorytet, +/−]` - priorytet z sekcji 2; `+` ścieżka pozytywna, `−` negatywna, `edge` przypadek brzegowy. `(seed)` = stan wymuszony przez DB w *test setup*. Oczekiwane rezultaty wynikają ze specyfikacji.

### `FLOW` - Reguły biznesowe i cykl życia zgłoszenia

*Tworzenie:*
- **TC-FLOW-01** [P1, +] - create z poprawnymi danymi (`status: new`) -> `201`, `Location`, status `acknowledged`.
- **TC-FLOW-02** [P1, +] - create z polem `note` -> notatka jest pierwszym elementem `notes`.
- **TC-FLOW-03** [P1, +] - idempotencja: ponowny create `(tenantId, externalId)` -> `200` z istniejącym zasobem (nie `201`), brak duplikatu.
- **TC-FLOW-04** [P2, − edge] - ponowny create z istniejącym `externalId`, lecz innym `description`/`serviceId` -> `200` ze **starym, niezmienionym** zasobem.
- **TC-FLOW-05** [P2, + edge] - ponowny create (ścieżka `200`) z polem `note` -> notatka **nie** zostaje dodana (idempotencja nie mutuje).

*Zamknięcie (decision table + state transition):*
- **TC-FLOW-06** [P0, +] - close z `acknowledged` -> `200`, status `closed`.
- **TC-FLOW-07** [P0, +] - close z `inProgress` -> `200`, status `closed`. *(seed)*
- **TC-FLOW-08** [P0, +] - po close automatyczna notatka o przejściu statusu obecna w `notes`.
- **TC-FLOW-09** [P0, −] - close z `new` -> `400 STATUS_TRANSITION_ERROR`. *(seed)*
- **TC-FLOW-10** [P0, −] - close z `resolved` -> `400 STATUS_TRANSITION_ERROR`. *(seed)*
- **TC-FLOW-11** [P0, −] - ponowny close z `closed` -> `400 STATUS_TRANSITION_ERROR`.
- **TC-FLOW-12** [P0, −] - close z `rejected` -> `400 STATUS_TRANSITION_ERROR`. *(seed)*

*Notatki (decision table):*
- **TC-FLOW-13** [P1, +] - notatka do `acknowledged` -> `201`.
- **TC-FLOW-14** [P1, +] - notatka do `inProgress` -> `201`. *(seed)*
- **TC-FLOW-15** [P1, +] - notatka do `new` -> `201`. *(seed)*
- **TC-FLOW-16** [P1, −] - notatka do `resolved` -> `400 NOTE_ADDITION_NOT_ALLOWED`. *(seed)*
- **TC-FLOW-17** [P1, −] - notatka do `closed` -> `400 NOTE_ADDITION_NOT_ALLOWED`.
- **TC-FLOW-18** [P1, −] - notatka do `rejected` -> `400 NOTE_ADDITION_NOT_ALLOWED`. *(seed)*
- **TC-FLOW-19** [P2, − edge] - po zamknięciu (auto-notatka dodana) ręczna notatka -> `400 NOTE_ADDITION_NOT_ALLOWED`.

### `SEC` - Multi-tenancy i bezpieczeństwo

- **TC-SEC-01** [P0, +] - lista zwraca wyłącznie zgłoszenia własnego tenanta.
- **TC-SEC-02** [P0, −] - GET cudzego zgłoszenia -> `404 TROUBLE_TICKET_NOT_FOUND` (nie `403`).
- **TC-SEC-03** [P0, −] - close cudzego zgłoszenia -> `404 TROUBLE_TICKET_NOT_FOUND`.
- **TC-SEC-04** [P0, −] - notatka do cudzego zgłoszenia -> `404 TROUBLE_TICKET_NOT_FOUND`.
- **TC-SEC-05** [P0, −] - żądanie bez tokenu -> `401`.
- **TC-SEC-06** [P0, −] - niepoprawny / wygasły token (token wygasły = z ręcznie ustawionym minionym `exp`) -> `401`.
- **TC-SEC-07** [P0, −] - token poprawny, lecz bez claimu `tenant_id` -> `403 FORBIDDEN`.
- **TC-SEC-08** [P1, + edge] - ten sam `externalId` u `alpha` i `beta` to dwa różne zasoby (idempotencja per tenant).
- **TC-SEC-09** [P0, − edge] - cudzy *istniejący* zasób zwraca identyczne `404` jak nieistniejący -> brak ujawnienia istnienia (ochrona przed enumeracją).
- **TC-SEC-10** [P1, − edge] - token z poprawnym podpisem, lecz błędnym `iss` -> `401` (walidacja `issuer-uri`).
- **TC-SEC-11** [P2, − edge] - token zmanipulowany / `alg: none` -> `401`.

### `API` - Kontrakt i walidacja wejścia

*`serviceId` (boundary value analysis):*
- **TC-API-01** [P1, +] - `serviceId` `100001` (dolna granica) -> akceptowany.
- **TC-API-02** [P1, +] - `serviceId` `100030` (górna granica) -> akceptowany.
- **TC-API-03** [P1, − edge] - `serviceId` `100000` (tuż poniżej) -> `404 SERVICE_NOT_FOUND`.
- **TC-API-04** [P1, − edge] - `serviceId` `100031` (tuż powyżej) -> `404 SERVICE_NOT_FOUND`.
- **TC-API-05** [P1, −] - `serviceId` `0` / ujemny -> `400 VALIDATION_ERROR` (`minimum: 1`).
- **TC-API-06** [P2, − edge] - `serviceId` `1` (poprawny wg OpenAPI, spoza zbioru wg TASK.md) -> `404 SERVICE_NOT_FOUND` (reguła pierwszeństwa).

*Wymagane pola, format, puste żądania:*
- **TC-API-07** [P1, −] - brak wymaganego pola (np. `description`) -> `400 VALIDATION_ERROR`.
- **TC-API-08** [P1, −] - create ze statusem ≠ `new` (np. `acknowledged`) -> `400`.
- **TC-API-09** [P1, − edge] - pole nadmiarowe (`additionalProperties: false`) -> `400 VALIDATION_ERROR`.
- **TC-API-10** [P2, − edge] - literówka w nazwie pola (`externalld`) -> `400` (brak wymaganego pola).
- **TC-API-11** [P1, −] - `description`/`externalId` puste -> `400 VALIDATION_ERROR` (narusza `minLength: 1`). Wartość z samych białych znaków to przypadek brzegowy: `minLength` liczy znaki, więc kontrakt formalnie jej nie zabrania - rezultat zależy od ewentualnego trim po stronie aplikacji (do weryfikacji).
- **TC-API-12** [P2, −] - malformed JSON -> `400 VALIDATION_ERROR`.
- **TC-API-13** [P2, − edge] - `serviceId` jako string `"100002"` lub ułamkowy -> `400` (niezgodność typu).
- **TC-API-14** [P1, −] - create z pustym body `{}` -> `400 VALIDATION_ERROR` (wszystkie wymagane pola brakujące; komunikat wskazuje braki).
- **TC-API-15** [P2, − edge] - create bez body (pusty ładunek) -> `400 VALIDATION_ERROR` (`requestBody` wymagane).

*Kontrakt close / note / pobranie:*
- **TC-API-16** [P1, −] - close z body `status` ≠ `closed` (np. `resolved`) -> `400` (naruszenie enum).
- **TC-API-17** [P1, −] - close z pustym body `{}` / brak `status` -> `400`.
- **TC-API-18** [P1, −] - notatka z pustym `text` -> `400 VALIDATION_ERROR`.
- **TC-API-19** [P1, −] - notatka z pustym body `{}` (brak `text`) -> `400 VALIDATION_ERROR`.
- **TC-API-20** [P1, −] - GET nieistniejącego `externalId` -> `404 TROUBLE_TICKET_NOT_FOUND`.
- **TC-API-21** [P3, − edge] - nieznana ścieżka -> `404`; niedozwolona metoda (np. DELETE) -> `405`.

### `UI` - Frontend (SPA)

- **TC-UI-01** [P0, +] - logowanie przez Keycloak -> po zalogowaniu widoczna lista (smoke).
- **TC-UI-02** [P0, +] - lista renderuje zgłoszenia własnego tenanta z oznaczeniem statusów.
- **TC-UI-03** [P1, −] - nieudane logowanie (reprezentatywne błędne dane: złe hasło, nieznany użytkownik, puste pola) -> brak wejścia do aplikacji, użytkownik zostaje na formularzu. Sprawdzana jest granica dostępu po stronie aplikacji (odpowiednik SEC-05/06 na poziomie UI), nie reguły uwierzytelniania Keycloak; dla niepustych danych dodatkowo widoczny komunikat błędu.
- **TC-UI-04** [P1, +] - formularz tworzenia: poprawne dane -> sukces, zgłoszenie pojawia się na liście.
- **TC-UI-05** [P1, −] - formularz tworzenia: dane niepoprawne (puste pole) -> walidacja kliencka / komunikat.
- **TC-UI-06** [P1, +] - widok szczegółów: historia notatek widoczna.
- **TC-UI-07** [P1, + edge] - dostępność akcji zależna od statusu: „Zamknij"/„Dodaj notatkę" niedostępne dla `resolved`/`closed`/`rejected`.
- **TC-UI-08** [P1, − edge] - akcja zakończona błędem API (np. `STATUS_TRANSITION_ERROR`) -> czytelny komunikat, spójny stan UI.
- **TC-UI-09** [P2, + edge] - utworzenie z istniejącym `externalId` (ścieżka idempotencji `200`) -> UI pokazuje istniejące zgłoszenie, brak duplikatu.
- **TC-UI-10** [P2, − edge] - wygaśnięcie tokenu w trakcie sesji -> transparentne odświeżenie lub ponowne logowanie (brak cichego błędu).

### `DATA` - Warstwa danych i dane testowe

- **TC-DATA-01** [P2, +] - unikalność `(tenant, externalId)` egzekwowana (manifestacja: idempotencja API).
- **TC-DATA-02** [P2, +] - wyłącznie dozwolone statusy są utrwalane (weryfikacja przez API).
- **TC-DATA-03** [P2, + edge] - notatki zwracane w porządku chronologicznym (wg daty utworzenia).
- **TC-DATA-04** [P2, +] - niezależność testów: dane z unikalnym `externalId`; brak kolizji przy powtórnym przebiegu.

### `CROSS` - Zagadnienia przekrojowe

- **TC-CROSS-01** [P2, +] - jednolity format błędu: `code` + `message` obecne, `requestId` obecny.
- **TC-CROSS-02** [P2, +] - pole `requestId` obecne i unikalne w odpowiedziach błędów (korelacja diagnostyczna).
- **TC-CROSS-03** [P2, +] - odpowiedzi mają `Content-Type: application/json`.
- **TC-CROSS-04** [P3, +] - Swagger UI dostępne bez uwierzytelnienia; plik OpenAPI serwowany.
- **TC-CROSS-05** [P3, − edge] - nieobsługiwana wersja API (`/api/v2/...`) -> `404`.
- **TC-CROSS-06** [P2, + edge] - odpowiedzi `401`/`403` również zgodne ze schematem `Error` (spójność formatu).

### Mapa pokrycia (traceability)

Zestawienie pokazuje, jak scenariusze pokrywają obszary, i łączy identyfikację (sekcja 1), priorytety (sekcja 2) i katalog.

| Obszar | Priorytety | Scenariusze | Pozytywne | Negatywne | Edge |
|---|---|---|---|---|---|
| `FLOW`  | P0-P2 | 19 | 10 | 9  | 3  |
| `SEC`   | P0-P2 | 11 | 2  | 9  | 4  |
| `API`   | P1-P3 | 21 | 2  | 19 | 8  |
| `UI`    | P0-P2 | 10 | 6  | 4  | 4  |
| `DATA`  | P2    | 4  | 4  | 0  | 1  |
| `CROSS` | P2-P3 | 6  | 5  | 1  | 2  |
| **Razem** | **P0-P3** | **71** | **29** | **42** | **22** |

Każdy obszar z sekcji 1 ma przypisane scenariusze. Ścieżki negatywne to około 59% katalogu, co wynika z nacisku, jaki TASK.md kładzie na obsługę błędów i przypadków brzegowych. Najgłębiej pokryte negatywnie są obszary o najwyższym priorytecie (`FLOW`, `SEC`). `DATA` sprawdzane jest pośrednio na poziomie API, dlatego nie ma tam osobnych ścieżek negatywnych.
