package com.aes.exam.ai.service;

import com.aes.exam.paper.service.DocxExtractionService.ExtractedDocument;
import org.springframework.stereotype.Component;

@Component
public class AiPromptBuilder {

    public String build(String title, ExtractedDocument document, String chunk, int index, int total) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("试卷标题：").append(title == null ? "" : title).append("\n\n");
        prompt.append("图片清单：\n");
        if (document.images().isEmpty()) {
            prompt.append("- 无图片\n");
        } else {
            document.images().forEach(image -> prompt
                .append("- ")
                .append("[IMG:")
                .append(image.id())
                .append("]")
                .append(": ")
                .append(image.fileName())
                .append(", ")
                .append(image.contentType())
                .append(", ")
                .append(image.size())
                .append(" bytes\n"));
        }
        prompt.append("\n以下是题目原文第 ")
            .append(index)
            .append("/")
            .append(total)
            .append(" 段。请只抽取本段中完整出现的题目，输出 JSON 对象。\n\n");
        prompt.append(chunk);
        return prompt.toString();
    }

    public String systemPrompt() {
        return """
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
            - 原文包含代码时，必须保留代码的换行、缩进、关键符号和语义，不要把代码压成一行。
            - 原文排版混乱时，只修复可确定的断行、空格和选项前缀，不要推断缺失内容。
            - “题目1、题目2、题目3”等分析题大题可以作为 essay；其下“【问题1】、【问题2】”如果内容完整，应分别拆成独立 essay 或 blank 题。
            - 如果一段只有题目图片，例如只包含 [IMG:image_3] 且没有可读选项，不要凭图片内容编造题目；保留为需要教师审核的 essay 题或返回空 questions。
            """;
    }
}
