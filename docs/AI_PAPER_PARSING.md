# AI Question Import Pipeline

Version: Phase 2

Status: Active

# 1. Feature Overview

This module imports exam questions from teacher-uploaded source files into the question bank.

Supported input in Phase 2:

- `.docx`
- `.txt`

The backend remains Java/Spring Boot. Python LangChain may be introduced later only as an optional internal service for OCR, visual parsing, or unusually complex layout preprocessing.

# 2. Pipeline

```text
Upload source file
  -> Store original file
  -> Extract text, tables, and image placeholders
  -> Split content into chunks
  -> Build strict DeepSeek prompts
  -> Parse short-field JSON
  -> Map to internal question model
  -> Validate
  -> Teacher review
  -> Import approved questions into official question bank
```

# 3. Extraction Rules

`.docx` extraction uses Apache POI:

- Preserve paragraph order.
- Preserve table rows using `|` cell separators.
- Insert image references as `[IMG:image_n]`.
- Keep code text and line breaks as much as Apache POI can expose.

`.txt` extraction reads UTF-8 text directly.

Images are not interpreted in Phase 2. If a question depends on image content, the placeholder is retained for teacher review. OCR or visual model parsing is a later enhancement.

# 4. DeepSeek Prompt Contract

System prompt:

```text
你是考试题库导入助手。把原文题目解析为 JSON。
只输出 JSON，不要 Markdown，不要解释。格式为 {"questions":[...]}。
questions 数组元素只允许这些短字段：
t 题型: single/multi/judge/blank/essay
c 题干: 不含选择题选项，必须原样保留 [IMG:文件名] 图片占位符
o 选项数组: 选择题去掉 A/B/C/D 前缀；非选择题为空数组
a 答案: single/multi/judge/blank 用数组；essay 用字符串；未知用 [] 或 ""
e 解析: 没有则空字符串
s 分值: 数字；未知按 single=2,multi=4,judge=1,blank=3,essay=10
规则：
- 单选只有一个正确选项，多选可有多个正确选项，答案字母用大写 A-H。
- 判断题答案统一为 ["对"] 或 ["错"]。
- 填空题答案按空顺序输出数组。
- 若原文包含答案区或解析区，要合并到对应题目。
- [IMG:xxx] 代表题目图片，它可能是题干、选项或解析的一部分，不要删除、改名或改写。
- 不要编造原文没有的题目、选项、答案或解析。
```

User prompt per chunk:

```text
以下是题目原文第 {idx}/{len(chunks)} 段。请只抽取本段中完整出现的题目，输出 JSON 对象。

{chunk}
```

# 5. JSON Contract

DeepSeek response:

```json
{
  "questions": [
    {
      "t": "single",
      "c": "题干",
      "o": ["选项一", "选项二"],
      "a": ["A"],
      "e": "",
      "s": 2
    }
  ]
}
```

Internal mapping:

| AI Type | Internal Type |
| --- | --- |
| `single` | `single_choice` |
| `multi` | `multiple_choice` |
| `judge` | `true_false` |
| `blank` | `fill_blank` |
| `essay` | `subjective` |

# 6. Storage

Temporary AI data:

- `papers`
- `ai_parse_jobs`
- `ai_parsed_questions`
- `ai_logs`

Approved question bank data:

- `questions`
- `question_options`
- `question_answers`

AI output never enters the official question bank until a teacher reviews and confirms it.

# 7. Security

- DeepSeek API Key is configured only through ignored local config or environment variables.
- The key is never committed to Git and never bundled into frontend code.
- Browser clients never call DeepSeek directly.
- Students cannot access parsing APIs.

# 8. Definition of Done

Phase 2 is complete when:

- `.docx` upload works.
- `.txt` upload works.
- Original files are retained.
- Apache POI extracts Word text, tables, and image placeholders.
- DeepSeek prompt uses the short-field import contract.
- JSON is mapped and validated.
- Teacher can edit questions, options, answers, analysis, and scores.
- Approved questions are imported into official question bank tables.
- Backend tests pass.
- Frontend build passes.
- API documentation is updated.
- Git commit and tag `v0.3.0-phase2` are ready.
