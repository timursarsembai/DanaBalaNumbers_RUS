# Progress and Plan

Date: 2025-08-29

## Progress
- Localized NumberIntroductionActivity:
  - Moved lesson texts (0–9) to string resources (ru/en/kk), applied locale via attachBaseContext.
  - Improved TTS locale selection; disabled TTS for Kazakh (button hidden).
  - Replaced hardcoded UI strings in activity_number_introduction.xml with resources.
  - Implemented locale-aware object naming (emoji → noun forms) using new resources:
    - values/strings_number_intro.xml (ru), values-en/strings_number_intro.xml, values-kk/strings_number_intro.xml.
  - Refactored generation logic to use resource arrays and correct grammar per locale (ru: cases and gender; en: singular/plural; kk: nominative after numerals).
- Home screen (MathExercises) games section: replaced hardcoded texts with strings (done earlier in this session).

## Plan
- Split training-specific strings into dedicated files for each training module (continue for remaining trainings).
- Optional: move emojis to string resources to resolve lint warnings.
- Extend the same locale-aware object naming approach to other trainings where needed.
- QA: verify language switching across devices; validate en/kk translations with native speakers.

