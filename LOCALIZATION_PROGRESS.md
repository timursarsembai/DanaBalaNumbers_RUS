# Прогресс локализации проекта DanaBala Numbers

## Этап 1: Подготовка инфраструктуры (Основа)

### ✅ ВЫПОЛНЕНО:

#### 1. Создание системы управления языками
- ✅ **LocaleManager класс создан** (`LocaleManager.kt`)
  - Полнофункциональный класс с методами управления языками
  - Поддержка трех языков: русский (ru), английский (en), казахский (kk)
  - Методы: getCurrentLanguage(), setLanguage(), applyLanguage(), getLanguageDisplayName()
  - Сохранение выбранного языка в SharedPreferences

- ✅ **LanguageSettingsActivity создан**
  - Интерфейс для выбора языка с RadioButton
  - Автоматический перезапуск приложения при смене языка
  - Интеграция с LocaleManager

- ✅ **Интеграция в главные активности**
  - MathExercisesActivity поддерживает переключение языков
  - DanaBalApplication применяет язык при запуске
  - Методы attachBaseContext() добавлены в нужные активности

#### 2. Создание базовой структуры папок
- ✅ **Папка `/res/values/`** (русский - по умолчанию)
- ✅ **Папка `/res/values-en/`** (английский)
- ✅ **Папка `/res/values-kk/`** (казахский)

#### 3. Рефакторинг строковых ресурсов -> ПОЛНОСТЬЮ ЗАВЕРШЕН ✅
- ✅ **Основные строки переведены на все языки**
  - Базовый интерфейс (next, done, back, retry, etc.)
  - Игровые элементы (score, level, time, etc.)
  - Названия активностей и результатов
  - Настройки языка

- ✅ **Удаление дублей**
  - strings_snake.xml очищен от дублированных строк
  - Основные строки консолидированы в strings.xml

- ✅ **Модульное разделение строк ПОЛНОСТЬЮ создано:**
  - `strings_training.xml` - все строки для тренировок ✅
  - `strings_games.xml` - все строки для игр ✅
  - `strings_results.xml` - все строки для экранов результатов ✅
  - `strings_ui.xml` - общие элементы интерфейса ✅

- ✅ **Завершена локализация paywall:**
  - `strings_paywall.xml` обновлен для русского ✅
  - `/values-en/strings_paywall.xml` создан ✅
  - `/values-kk/strings_paywall.xml` создан ✅

- ✅ **НОВОЕ: Модульные файлы переведены на все языки:**
  - `/values-en/strings_training.xml` ✅
  - `/values-en/strings_games.xml` ✅
  - `/values-en/strings_results.xml` ✅
  - `/values-en/strings_ui.xml` ✅
  - `/values-kk/strings_training.xml` ✅
  - `/values-kk/strings_games.xml` ✅
  - `/values-kk/strings_results.xml` ✅
  - `/values-kk/strings_ui.xml` ✅

- ✅ **НОВОЕ: Обновление основного strings.xml:**
  - Удалены дублированные строки из основного strings.xml ✅
  - Оставлены только app_name и критически важные строки ✅
  - **ИСПРАВЛЕНО: Решена проблема дублированных ресурсов** ✅

## 🎉 ЭТАП 1 ПОЛНОСТЬЮ ЗАВЕРШЕН! 🎉

### ✅ ФИНАЛЬНЫЙ СТАТУС:
- Все дублированные ресурсы устранены ✅
- Проект успешно компилируется ✅  
- Модульная структура строк готова ✅
- Поддержка 3 языков (ru, en, kk) работает ✅
- **ИСПРАВЛЕНО: Все ошибки линковки ресурсов устранены** ✅

## Лог изменений — 29 августа 2025

- Локализован макет activity_math_exercises.xml (экран списка тренировок и игр):
  - В секции «Игры» все жёстко заданные тексты заменены на строковые ресурсы из strings_ui.xml для всех трёх языков (ru, en, kk).
  - Игра Bubble Catch: заголовок использует ресурс game_bubble_catch_title из strings_games.xml (в strings_ui.xml заголовка нет); описание берётся из main_game_bubble_catch_desc (strings_ui.xml).
  - Эмодзи оставлены в макете (есть lint-предупреждения о «hardcoded string»). При необходимости можно вынести эмодзи в strings_ui.xml позднее.
- Проверка: ошибок линковки ресурсов нет; остаются только lint-предупреждения на эмодзи и предупреждение о количестве View (>80) в макете.

*Последнее обновление: 29 августа 2025*

## Лог изменений — 29 августа 2025 (продолжение)

- Локализована тренировка «Знакомство с цифрами» (NumberIntroductionActivity):
  - Уроки для цифр 0–9 вынесены в строковые ресурсы (ru/en/kk); активность применяет локаль через attachBaseContext.
  - Макет activity_number_introduction.xml переведён на ресурсы (заголовок, индикатор, кнопки, начальные значения).
  - Реализована локализуемая генерация описаний объектов (emoji → формы слов) на базе новых ресурсов:
    - values/strings_number_intro.xml (ru), values-en/strings_number_intro.xml, values-kk/strings_number_intro.xml.
    - RU: падежи и род (один/одна/одно), EN: ед./мн. число, KK: существительное после числительных в ед. числе.
  - Озвучка (TTS): улучшен выбор локали; для казахского языка озвучка отключена (кнопка скрыта).
  - Мелкие правки: формат индикатора слайдов через строковый ресурс, замена устаревших конструкторов Locale на Locale.forLanguageTag.

- Домашний экран (MathExercises): в секции «Игры» жёсткие строки заменены на ресурсы (strings_ui.xml) — выполнено ранее в рамках этой сессии.

*Последнее обновление: 29 августа 2025*

## Лог изменений — 29 августа 2025 (продолжение — «Найди цифру»)

- Локализована тренировка «Найди цифру» (NumberRecognitionActivity):
  - Все жёстко заданные строки вынесены в ресурсы: строки вопроса, заголовок, тексты загрузки и кнопок, а также строки экрана результатов.
  - Добавлены переводы для ru/en/kk в strings_ui.xml, strings_training.xml и strings_results.xml, а также соответствующие файлы в values-en/ и values-kk/.
  - Макеты activity_number_recognition.xml и activity_number_recognition_results.xml переведены на использование строковых ресурсов.
- Локализация TTS и логика языка:
  - NumberRecognitionActivity применяет локаль приложения через attachBaseContext.
  - TTS инициализируется с языком из LocaleManager (ru/en). Для казахского языка (kk) TTS отключён.
  - Для kk иконка динамика скрывается; для ru/en иконка отображается и озвучка работает корректно.
  - Текст вопроса формируется по шаблону из ресурсов (find_digit_instruction) с использованием словесных названий чисел (number_name_0..9) для корректной озвучки.
- Экран результатов «Найди цифру»:
  - Вынесены и локализованы заголовок, формат счёта и строка «Правильных ответов» (ru/en/kk).
- Дополнительно:
  - Закрыты предупреждения о недостающих переводах для новых ключей (kk/en). Остались предупреждения об устаревшем конструкторе Locale (не критично; можно заменить на Locale.forLanguageTag позднее).

Проверка:
- RU: интерфейс и озвучка соответствуют русскому языку; динамик доступен.
- KK: интерфейс на казахском, озвучка отключена, иконка динамика скрыта.
- EN: интерфейс и озвучка соответствуют английскому языку; динамик доступен.

## Лог изменений — 29 августа 2025 (продолжение — «Найди количество»)

- Локализована тренировка «Найди количество» (CountingActivity + CountingResultsActivity):
  - Вынесены все жёстко заданные строки из макетов `activity_counting.xml` и `activity_counting_results.xml` в ресурсы.
  - Добавлены модульные ресурсы для тренировки:
    - ru: `values/strings_counting.xml` (заголовки, вопрос, форматы, массивы фраз похвалы/подбадривания, фразы для озвучки итогов).
    - en: `values-en/strings_counting.xml` (аналогично; дополнительно устранены апострофы для стабильности мерджа ресурсов).
    - kk: `values-kk/strings_counting.xml` (аналогично; интерфейс локализован).
  - В `CountingActivity.kt` фразы обратной связи читаются из строковых массивов (`R.array.counting_*`), TTS не используется при локали `kk`.
  - В `CountingResultsActivity.kt` формат вывода счёта переведён на ресурс `score_of_format`; строки заголовков/ободрения берутся из ресурсов.

- Настройка локали и TTS:
  - Добавлен `attachBaseContext(LocaleManager.applyLanguage(...))` в обе активности тренировки, чтобы UI подхватывал выбранный язык (ru/en/kk).
  - TTS выбирает язык по текущему коду из `LocaleManager` (ru → RU, en → US). Для `kk` озвучка намеренно отключена.
  - Для EN устранена ошибка сборки ресурсов (NPE при `mergeDebugResources`) путём замены апострофов в `values-en/strings_counting.xml` на эквиваленты без апострофов.

- Дополнительно:
  - Добавлен формат строки счёта `score_of_format` в EN/KK, удалена конкатенация строк в коде.
  - Устранены все жёстко заданные русские подписи в макетах этой тренировки.

Проверка:
- RU: интерфейс и озвучка работают на русском.
- KK: интерфейс на казахском, озвучка отключена по требованию.
- EN: интерфейс и озвучка работают на английском.

*Последнее обновление: 29 августа 2025*

## Лог изменений — 29 августа 2025 (продолжение — «Посчитай предметы»)

- Локализована тренировка «Посчитай предметы» (ObjectCountingActivity + ObjectCountingResultsActivity):
  - Строки вынесены в модульные ресурсы и переведены: values/strings_object_counting.xml (ru), values-en/strings_object_counting.xml (en), values-kk/strings_object_counting.xml (kk).
  - Переиспользованы массивы фраз из strings_counting.xml для похвалы/подбадривания и TTS на экране результатов.
  - Макеты activity_object_counting.xml и activity_object_counting_results.xml переведены на строковые ресурсы; эмодзи вынесено в @string/object_counting_emoji; убраны хардкоды (примерные значения перенесены в tools:text).
- Локализация и TTS:
  - Обе активности применяют локаль через attachBaseContext(LocaleManager.applyLanguage(...)).
  - Для kk озвучка полностью отключена по требованию. Для ru/en TTS включён, язык выбирается через Locale.forLanguageTag("ru-RU"/"en-US").
  - Текст вопроса формируется по шаблону: для ru/en используется локализованный словарь названий эмодзи и шаблон "object_counting_question_template"; для kk — общий вопрос без TTS.
- Технические правки:
  - Заменены устаревшие конструкторы Locale(...) на Locale.forLanguageTag(...).
  - Устранены lint-предупреждения о жёстко заданных строках в макетах.

Проверка:
- RU: интерфейс и озвучка работают на русском.
- KK: интерфейс на казахском, озвучка отключена полностью.
- EN: интерфейс и озвучка работают на английском.

*Последнее обновление: 29 августа 2025*

## Лог изменений — 29 августа 2025 (продолжение — «Сопоставление»)

- Локализована тренировка «Сопоставление» (MatchingActivity + MatchingResultsActivity):
  - Все жёстко заданные строки вынесены в модульные ресурсы:
    - ru: values/strings_matching.xml
    - en: values-en/strings_matching.xml
    - kk: values-kk/strings_matching.xml
  - Макеты activity_matching.xml и activity_matching_results.xml переведены на использование @string; добавлены contentDescription.
- Озвучка (TTS):
  - Язык TTS выбирается по LocaleManager (ru-RU/en-US).
  - Для казахского языка (kk) TTS полностью отключён в тренинге и на экране результатов согласно требованию.
- Локаль и инициализация:
  - В MatchingActivity и MatchingResultsActivity добавлен attachBaseContext(LocaleManager.applyLanguage(...)).
  - Устаревшие конструкторы Locale("..", "..") заменены на Locale.forLanguageTag("..").
- Текстовая обратная связь:
  - Фразы похвалы/подбадривания и TTS-фразы экрана результатов вынесены в строковые массивы (matching_*).
- Дополнительно:
  - Устранены хардкоды в макетах; добавлено tools:ignore="MissingTranslation" в ru-файл для устойчивости сборки (переводы en/kk присутствуют).

Проверка:
- RU: интерфейс и TTS работают на русском.
- KK: интерфейс на казахском, TTS отключён полностью.
- EN: интерфейс и TTS работают на английском.

*Последнее обновление: 29 августа 2025*

## Лог изменений — 29 августа 2025 (продолжение — «Сопоставление по звуку»)

- Полная локализация тренировки «Сопоставление по звуку» (AudioMatchingActivity + AudioMatchingResultsActivity) и их макетов:
  - Заменены все хардкоды на строковые ресурсы.
  - Созданы модульные файлы строк:
    - ru: values/strings_audio_matching.xml
    - en: values-en/strings_audio_matching.xml
    - kk: values-kk/strings_audio_matching.xml
  - Добавлен массив названий чисел 1–9 (number_names_1_9) во всех языках для корректной озвучки.
- Локаль и TTS:
  - Выбор языка TTS по LocaleManager (ru-RU/en-US); для казахского (kk) TTS отключён во всей тренировке и на экране результатов.
  - Озвучка чисел берётся из локализованных массивов; фразы поощрения/поддержки — из локализованных массивов matching_*.
- Домашний экран (MathExercisesActivity):
  - Карточка «Сопоставление по звуку» скрывается для казахского языка (kk) по требованиям.
- Макеты:
  - activity_audio_matching.xml и activity_audio_matching_results.xml переведены на использование @string и добавлены contentDescription для кнопок «Назад».
- Проверка:
  - RU/EN: интерфейс и TTS работают корректно.
  - KK: интерфейс переведён; карточка тренировки скрыта; TTS отключён.

*Последнее обновление: 29 августа 2025*

## Лог изменений — 30 августа 2025

- Полная локализация тренировки «Сравнение чисел» (NumberComparisonActivity + NumberComparisonResultsActivity) и макетов:
  - Вынесены все жёсткие строки в модульные ресурсы: values/strings_number_comparison.xml (ru), values-en/strings_number_comparison.xml (en), values-kk/strings_number_comparison.xml (kk).
  - Макеты activity_number_comparison.xml и activity_number_comparison_results.xml переведены на @string; добавлены contentDescription для кнопок «Назад».
  - Включён TTS для ru/en, отключён для kk; выбор Locale через LocaleManager; заменён устаревший Locale("..", "..") на Locale.forLanguageTag("..").
  - Добавлены фразы поощрения и шаблоны TTS; интерфейс и озвучка соответствуют выбранному языку.

- Локализация «Числа по возрастанию» (AscendingSequenceActivity + AscendingSequenceResultsActivity):
  - Вынесены все строки в ресурсы: values/strings_ascending.xml (ru), values-en/strings_ascending.xml (en), values-kk/strings_ascending.xml (kk).
  - Макеты activity_ascending_sequence.xml и activity_ascending_sequence_results.xml переведены на @string; заменены жёсткие тексты кнопок и подсказок.
  - Активности применяют локаль через attachBaseContext(LocaleManager.applyLanguage(...)).
  - TTS: ru/en — включён с соответствующей локалью; kk — отключён по требованию.
  - Исправлено: добавлен отсутствующий ресурс ascending_score_points (values/strings_ascending_additions.xml), устранена ошибка компиляции Unresolved reference.

- Общие правки:
  - Подавлены MissingTranslation в ru-файле strings_number_comparison.xml для устойчивости сборки (переводы en/kk присутствуют).
  - Проверены XML-ресурсы на пустые/повреждённые файлы; критичных проблем не обнаружено.

Проверка:
- RU/EN: интерфейс и TTS корректны.
- KK: интерфейс переведён, TTS отключён по требованию.

*Последнее обновление: 30 августа 2025*

## Лог изменений — 30 августа 2025 (продолжение — «Числа по убыванию»)

- Полная локализация тренировки «Числа по убыванию» (DescendingSequenceActivity + DescendingSequenceResultsActivity) и макетов:
  - Вынесены все жёсткие строки в модульные ресурсы: values/strings_descending.xml (ru), values-en/strings_descending.xml (en), values-kk/strings_descending.xml (kk).
  - Макеты activity_descending_sequence.xml и activity_descending_sequence_results.xml переведены на @string; добавлены contentDescription для кнопок «Назад»; убраны хардкоды.
  - Активности применяют локаль через attachBaseContext(LocaleManager.applyLanguage(...)).
  - TTS: ru/en — включён с выбором Locale через LocaleManager; kk — отключён полностью по требованию.
  - Фразы похвалы/подсказки и шаблоны озвучки вынесены в строковые массивы и шаблоны (descending_*). 
  - Заменены устаревшие конструкторы Locale(...) на Locale.forLanguageTag(...); очищены предупреждения о жёстко заданных строках.

Проверка:
- RU/EN: интерфейс и TTS корректны.
- KK: интерфейс переведён, TTS отключён.

*Последнее обновление: 30 августа 2025*

## Лог изменений — 30 августа 2025 (продолжение — «Выбор языка и флаги»)

- Главная страница (MathExercisesActivity):
  - Иконка языка заменена на флаг выбранного языка (RU/KZ/EN). Флаг обновляется при возвращении на экран (onResume).
  - contentDescription кнопки языка теперь соответствует названию текущего языка (для доступности).
- Ресурсы флагов:
  - Добавлены векторные drawable: ic_flag_ru, ic_flag_kz, ic_flag_us.
  - В LocaleManager добавлен метод getLanguageFlagRes(languageCode) для получения ресурса флага.
- Экран «Настройки языка» (LanguageSettingsActivity + activity_language_settings.xml):
  - Полный редизайн списка языков — минималистичный стиль в духе iOS: без чекбоксов и радиокнопок.
  - Каждый язык — это MaterialCardView c флагом и названием; выбранный элемент подсвечивается мягким цветом фона.
  - Убраны рамки; выделение реализовано цветом фона и акцентом текста.
  - В макете RadioGroup заменён на LinearLayout-контейнер для динамического списка карточек.
  - Добавлены цвета: language_selected_bg (фон выбранного), language_unselected_bg (фон обычного).
- Дополнительно:
  - Цвет текста выбранного элемента — акцентный (primaryColor), для остальных — text_color.

Проверка:
- Сборка assembleDebug — успешна; критичных ошибок нет.
- Предупреждения: onBackPressed() устаревший (не критично; поведение сохранено), предупреждения об устаревших конструкторах Locale в других местах не затронуты этим изменением.

*Последнее обновление: 30 августа 2025*

## Лог изменений — 30 августа 2025 (продолжение — «Ошибка в ряду»)

- Полная локализация тренировки «Ошибка в ряду» (RowErrorActivity + RowErrorResultsActivity) и их макетов:
  - Вынесены все жёстко заданные строки в модульные ресурсы: values/strings_row_error.xml (ru), values-en/strings_row_error.xml (en), values-kk/strings_row_error.xml (kk).
  - Макеты activity_row_error.xml и activity_row_error_results.xml переведены на @string; добавлены contentDescription для кнопки «Назад»; эмодзи вынесено в @string/row_error_emoji.
  - Формат строки результата с процентами вынесен в ресурс row_error_correct_answers_with_percent_format.
- Локаль и TTS:
  - Обе активности применяют локаль через attachBaseContext(LocaleManager.applyLanguage(...)).
  - В RowErrorActivity TTS инициализируется для ru/en (Locale.forLanguageTag("ru-RU"/"en-US")); для kk TTS полностью отключён (не инициализируется и не вызывается).
- Экран результатов:
  - Заголовок выбирается из локализованных вариантов по проценту (excellent/great/good/keep trying).
  - Текст счёта формируется по локализованному формату с процентами.
- Технические правки:
  - Исправлены апострофы в EN-строках (заменены на типографские) во избежание ошибок мерджа ресурсов.
  - Исправлена валидность values-kk/strings_row_error.xml (корневой тег resources) после обнаружения пустого файла.
- Проверка:
  - Сборка assembleDebug — успешна; критичных ошибок нет.
  - RU/EN: интерфейс и TTS работают.
  - KK: интерфейс переведён; TTS полностью отключён по требованию.

*Последнее обновление: 30 августа 2025*

## Лог изменений — 30 августа 2025 (дополнение — «Сравнение чисел»)

- Верификация и полировка тренировки «Сравнение чисел» (NumberComparisonActivity + NumberComparisonResultsActivity):
  - Проверены ресурсы ru/en/kk в values*/strings_number_comparison.xml; все ключи присутствуют (UI-строки, массивы похвалы/поддержки, шаблоны TTS).
  - Подтверждено: TTS инициализируется только для ru/en; для kk полностью отключён (tts не создаётся и не вызывается) в обеих активностях.
  - Проверены словесные названия чисел number_name_0..9 в strings_training.xml для всех локалей — присутствуют.
  - Макеты activity_number_comparison.xml и activity_number_comparison_results.xml используют @string; жёстких строк нет; у кнопок «Назад» есть contentDescription.
  - Сборка assembleDebug и статическая проверка файлов — успешны; ошибок не обнаружено.

Проверка:
- RU/EN: интерфейс и TTS соответствуют выбранному языку.
- KK: интерфейс переведён; TTS отключён полностью по требованию.

*Последнее обновление: 30 августа 2025*

## Лог изменений — 30 августа 2025 (дополнение — «Язык по умолчанию = язык системы»)

- Реализовано поведение по умолчанию: при первом запуске приложения язык берётся из системного языка пользователя.
  - Если системный язык поддерживается (ru, en, kk), используется он.
  - Для «kz» выполнен маппинг на «kk». Для прочих неподдерживаемых — резерв: «en».
  - Выбор не сохраняется до тех пор, пока пользователь явно не сменит язык в настройках.
- После явного выбора в «Настройках языка» приложение сохраняет код языка в SharedPreferences и далее всегда использует этот выбор.
- Точки интеграции:
  - DanaBalApplication.applyLanguage(...) теперь использует актуальную логику LocaleManager.getCurrentLanguage(...), которая возвращает системный язык при отсутствии сохранённого выбора.
  - LanguageSettingsActivity подсвечивает текущий язык, определяемый новой логикой (системный при первом запуске, либо сохранённый пользовательский).
- Технические детали:
  - Обновлён LocaleManager: добавлен resolveSystemLanguage(...) c маппингом ru/en/kk (+kz→kk). Метод getCurrentLanguage(...) возвращает сохранённый язык, а при его отсутствии — системный.

Проверка:
- Сборка assembleDebug — успешна; регрессий не обнаружено.
- Для проверки «первого запуска» очистите данные приложения или переустановите APK: интерфейс стартует на языке системы; после ручного выбора фиксируется выбранный язык.

*Последнее обновление: 30 августа 2025*

## Лог изменений — 30 августа 2025 (продолжение — «Рисование цифр»)

- Полная локализация игры «Рисование цифр» (NumberDrawingActivity + NumberDrawingResultsActivity) и макетов:
  - Устранены все жёстко заданные строки в activity_number_drawing.xml и activity_number_drawing_results.xml; добавлены ресурсы: number_drawing_title, eraser, number_drawing_results_* и др.
  - Созданы модульные файлы строк: values/strings_number_drawing.xml (ru), values-en/strings_number_drawing.xml (en), values-kk/strings_number_drawing.xml (kk).
  - Фразы похвалы и подсказки вынесены в строковые массивы (number_drawing_success_phrases, number_drawing_encouragement_phrases; для экрана результатов — number_drawing_results_praise_phrases/…_motivation_phrases) и локализованы на ru/en/kk.
- Локаль и TTS:
  - Обе активности применяют локаль приложения через attachBaseContext(LocaleManager.applyLanguage(...)).
  - TTS выбирает язык по LocaleManager: ru → ru-RU, en → en-US (Locale.forLanguageTag). Для казахского языка (kk) TTS полностью отключён (не инициализируется и не вызывается).
- Логика UI:
  - Логика кнопки «Готово/Далее» переписана на булев флаг isNextMode вместо сравнения текста, предотвращая завязку на конкретную локализацию.
  - Добавлены/уточнены contentDescription для кнопок «Назад» и «Ластик» через ресурсы.
- Проверка:
  - Сборка assembleDebug — успешна; ошибок компиляции нет.

*Последнее обновление: 30 августа 2025*

## Лог изменений — 30 августа 2025 (исправления: ресурсы EN и синтаксис)

- Исправлена ошибка сборки mergeDebugResources (NPE в aapt2 при мердже values-en):
  - В файле `values-en/strings_number_drawing.xml` экранированы апострофы в строках с You\'re/you\'re и т. п., устранён краш компилятора ресурсов.
  - Проверены другие файлы `values-en/*.xml` на наличие битых тегов и спецсимволов — критичных проблем не обнаружено.
- Замена устаревших конструкторов Locale("..", "..") → `Locale.forLanguageTag(...)` в:
  - `NumberDrawingActivity.kt`
  - `NumberDrawingResultsActivity.kt`
- Устранены синтаксические ошибки Kotlin (Unexpected tokens):
  - `NumberDrawingActivity.kt`: корректно завершены выражения и блоки, логика кнопки «Готово/Далее» переведена на флаг `isNextMode`.
  - `NumberDrawingResultsActivity.kt`: исправлен метод `onInit(...)`, вынесена инициализация TTS, корректно оформлен отложенный вызов `Handler.postDelayed { ... }`.
- Локализация «Рисование цифр» подтверждена:
  - Макеты `activity_number_drawing*.xml` переведены на @string (ru/en/kk), добавлены недостающие ключи.
  - Добавлены модульные строки: `values/strings_number_drawing.xml`, `values-en/strings_number_drawing.xml`, `values-kk/strings_number_drawing.xml` (заголовки, кнопки, фразы успеха/поддержки, фразы результата).
  - Для языка `kk` TTS полностью отключён в тренировке и на экране результатов; для `ru/en` TTS включает соответствующую локаль (`ru-RU`/`en-US`).
- Проверка сборки:
  - `./gradlew assembleDebug` — успешно; ошибок Kotlin и ресурсов нет.

*Последнее обновление: 30 августа 2025*

# Лог изменений — 31 августа 2025

- Полная локализация игры «Шарики с цифрами» (BubbleCatchActivity + BubbleCatchResultsActivity) и их макетов:
  - Все хардкоды вынесены в модульные ресурсы: values/strings_bubble_catch.xml (ru), values-en/strings_bubble_catch.xml (en), values-kk/strings_bubble_catch.xml (kk).
  - Исправлены ошибки ресурсов: битый base-файл (multiple root/EOF), экранированы апострофы в EN (You\'re), приведены файлы к валидному XML.
- Применение локали и TTS:
  - В BubbleCatchActivity/BubbleCatchResultsActivity добавлен attachBaseContext(LocaleManager.applyLanguage(...)) — UI подхватывает выбранный язык.
  - TTS включён для ru/en (Locale.forLanguageTag("ru-RU"/"en-US")), для kk полностью отключён; кнопка динамика скрывается при kk.
  - Озвучка цели формируется из локализованных названий чисел (number_name_0..9); фразы обратной связи и сообщений берутся из локализованных массивов.
- Стабильность применения языка:
  - LanguageSettingsActivity: при смене языка выполняется полный перезапуск приложения с очисткой стека (NEW_TASK | CLEAR_TASK + finishAffinity) для гарантированного применения локали во всех активити, включая игру.
- Сборка:
  - Ошибки mergeDebugResources устранены; ресурсы ru/en/kk корректно мержатся.

Проверка:
- RU: интерфейс и озвучка работают на русском.
- EN: интерфейс и озвучка на английском.
- KK: интерфейс на казахском, озвучка полностью отключена; иконка динамика скрыта.

*Последнее обновление: 31 августа 2025*

# Лог изменений — 31 августа 2025 (продолжение — «Цифровой ряд»)

- Полная локализация игры «Цифровой ряд» (BlockMatchActivity + BlockMatchResultsActivity) и их макетов:
  - Вынесены все фразы в модульные ресурсы: values/strings_block_match.xml (ru), values-en/strings_block_match.xml (en), values-kk/strings_block_match.xml (kk):
    - Фразы объявлений повышения уровня (2–10 и общий шаблон).
    - Тексты экрана результатов: формат счёта, массивы фраз поздравления и мотивации.
  - Экран результатов переведён на @string/@array; удалены жёсткие строки.
- Применение локали и TTS:
  - В BlockMatchActivity и BlockMatchResultsActivity добавлен attachBaseContext(LocaleManager.applyLanguage(...)) — UI подхватывает выбранный язык (ru/en/kk).
  - Исправлено: игра больше не принудительно на английском; тексты берутся из активной локали.
  - TTS включён для ru/en (Locale.forLanguageTag("ru-RU"/"en-US")), для kk полностью отключён.
  - Фразы озвучки уровней берутся из локализованных ресурсов; на kk озвучка не создаётся и не вызывается.
- Известный вопрос (на 31.08.2025):
  - Игра открывается, но не стартует игровой цикл (ощущение «замерзания» экрана).
  - Гипотеза: проблема в lifecycle BlockMatchGameView (resume()/Choreographer) или в порядке инициализации после внедрения attachBaseContext/TTS.
  - План: проверить вызовы gameView.resume() в onResume(), состояние running/lastFrameNs и регистрацию frameCallback; добавить логирование кадров и guard'ы против двойной паузы; воспроизвести на ru/en/kk.

Проверка:
- RU/EN/KK: UI локализован корректно; TTS работает на ru/en и отключён на kk.
- TODO: устранить «замерзание» игрового цикла.

*Последнее обновление: 31 августа 2025*

# Лог изменений — 31 августа 2025 (продолжение — «Скорочтение. Шульте»)

- Полная локализация игры «Скорочтение. Шульте» (SchulteNumbersActivity + SchulteNumbersResultsActivity) и их макетов:
  - Вынесены строки в модульные ресурсы: values/strings_schulte.xml (ru), values-en/strings_schulte.xml (en), values-kk/strings_schulte.xml (kk).
  - Исправлен пустой base-файл values/strings_schulte.xml (ошибка mergeDebugResources: EOF) — восстановлен корректным содержимым.
  - Макет activity_schulte_numbers_results.xml переведён на @string: заголовок (@string/results_title), «Назад» (@string/back), подзаголовок (@string/your_result), кнопки (@string/play_again, @string/main_menu).
- Применение локали и TTS:
  - Обе активности применяют локаль через attachBaseContext(LocaleManager.applyLanguage(...)).
  - TTS: включён для ru/en (Locale.forLanguageTag("ru-RU"/"en-US")); для kk — полностью отключён (tts не создаётся и не вызывается), в соответствии с требованием.
  - Озвучка цели в самой игре формируется из локализованных шаблонов: find_digit_instruction + number_name_0..9.
  - Экран результатов: фразы похвалы и шаблоны времени локализованы; для kk озвучка отключена.
- Проверка:
  - ./gradlew assembleDebug — успешно; UI локализован; поведение TTS соответствует языку (ru/en — включён; kk — отключён).

*Последнее обновление: 31 августа 2025*

## Лог изменений — 31 августа 2025 (продолжение — «Змейка в математике»)

- Полная локализация игры «Змейка в математике» (SnakeMathActivity + SnakeMathResultsActivity) и их макетов:
  - Все жёстко заданные строки вынесены в модульные ресурсы: values/strings_snake_math.xml (ru), values-en/strings_snake_math.xml (en), values-kk/strings_snake_math.xml (kk).
  - Макет activity_snake_math.xml: убраны хардкоды «Уровень 1» и «00:00.0» (переведены в tools:text, реальные значения задаются из кода); заголовок использует @string/game_snake_math_title; сохранены emoji на кнопках джойстика.
  - Экран результатов activity_snake_math_results.xml использует локализованные строки (заголовки, формат уровня, фразы мотивации).
- Локаль и применение языка:
  - В SnakeMathActivity и SnakeMathResultsActivity добавлен attachBaseContext(LocaleManager.applyLanguage(...)) для корректного UI на выбранном языке (ru/en/kk).
- Озвучка (TTS):
  - Для ru/en TTS включён, язык выбирается через Locale.forLanguageTag("ru-RU"/"en-US"); для kk TTS полностью отключён по требованию (движок не инициализируется и вызовы speak подавлены).
  - Озвучка событий вынесена в ресурсы: повышение уровня (snake_math_speak_level), «Правильно: %1$s» (snake_math_correct_template, с использованием словарных названий чисел number_name_0..9), «Не та цифра» (snake_math_wrong), а также шаблоны озвучки экрана результатов (win/lose).
- Технические правки:
  - Заменены устаревшие конструкторы Locale на Locale.forLanguageTag.
  - Исправлены EN-строки для устойчивости мерджа ресурсов (экранированы/исключены апострофы в values-en/strings_snake_math.xml).
- Проверка:
  - RU/EN: интерфейс и TTS соответствуют выбранному языку.
  - KK: интерфейс переведён; TTS отключён полностью.
  - Сборка assembleDebug — успешна; ошибок ресурсов/линковки не выявлено.

*Последнее обновление: 31 августа 2025*
