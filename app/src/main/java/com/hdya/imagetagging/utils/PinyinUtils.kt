package com.hdya.imagetagging.utils

object PinyinUtils {
    
    /**
     * Convert Chinese characters to Pinyin for search
     * This is an enhanced implementation that covers more Chinese characters
     */
    private val chineseToPinyin = mapOf(
        // Common characters
        '的' to "de", '是' to "shi", '我' to "wo", '你' to "ni", '他' to "ta", '她' to "ta", '它' to "ta",
        '了' to "le", '在' to "zai", '有' to "you", '和' to "he", '人' to "ren", '这' to "zhe", '中' to "zhong",
        '大' to "da", '为' to "wei", '上' to "shang", '个' to "ge", '国' to "guo", '时' to "shi", '要' to "yao",
        '就' to "jiu", '出' to "chu", '会' to "hui", '可' to "ke", '也' to "ye", '对' to "dui", '生' to "sheng",
        '能' to "neng", '而' to "er", '子' to "zi", '那' to "na", '得' to "de", '于' to "yu", '着' to "zhe",
        '下' to "xia", '自' to "zi", '之' to "zhi", '年' to "nian", '过' to "guo", '发' to "fa", '后' to "hou",
        '作' to "zuo", '里' to "li", '用' to "yong", '道' to "dao", '行' to "xing", '所' to "suo", '然' to "ran",
        '家' to "jia", '种' to "zhong", '事' to "shi", '方' to "fang", '多' to "duo", '经' to "jing", '么' to "me",
        '去' to "qu", '法' to "fa", '学' to "xue", '如' to "ru", '水' to "shui", '前' to "qian", '面' to "mian",
        
        // More characters including pet-related ones
        '宠' to "chong", '物' to "wu", '小' to "xiao", '胖' to "pang", '猫' to "mao", '狗' to "gou",
        '鸟' to "niao", '鱼' to "yu", '虫' to "chong", '熊' to "xiong", '老' to "lao", '虎' to "hu",
        '狮' to "shi", '象' to "xiang", '龙' to "long", '凤' to "feng", '兔' to "tu", '鸡' to "ji",
        '鸭' to "ya", '鹅' to "e", '猪' to "zhu", '牛' to "niu", '马' to "ma", '羊' to "yang",
        
        // Numbers
        '一' to "yi", '二' to "er", '三' to "san", '四' to "si", '五' to "wu", '六' to "liu",
        '七' to "qi", '八' to "ba", '九' to "jiu", '十' to "shi", '百' to "bai", '千' to "qian",
        '万' to "wan", '亿' to "yi",
        
        // Directions
        '东' to "dong", '西' to "xi", '南' to "nan", '北' to "bei", '左' to "zuo", '右' to "you",
        
        // Nature
        '山' to "shan", '河' to "he", '天' to "tian", '地' to "di", '花' to "hua", '树' to "shu",
        '草' to "cao", '水' to "shui", '火' to "huo", '土' to "tu", '风' to "feng", '雨' to "yu",
        '雪' to "xue", '雷' to "lei", '电' to "dian", '光' to "guang", '月' to "yue", '星' to "xing",
        '空' to "kong", '云' to "yun", '冰' to "bing",
        
        // Colors
        '红' to "hong", '绿' to "lv", '蓝' to "lan", '白' to "bai", '黑' to "hei", '黄' to "huang",
        '金' to "jin", '银' to "yin", '紫' to "zi", '粉' to "fen", '灰' to "hui",
        
        // Actions
        '吃' to "chi", '喝' to "he", '睡' to "shui", '走' to "zou", '跑' to "pao", '跳' to "tiao",
        '笑' to "xiao", '哭' to "ku", '唱' to "chang", '舞' to "wu", '看' to "kan", '听' to "ting",
        '说' to "shuo", '读' to "du", '写' to "xie", '画' to "hua", '玩' to "wan", '游' to "you",
        
        // Body parts
        '头' to "tou", '脸' to "lian", '眼' to "yan", '鼻' to "bi", '嘴' to "zui", '耳' to "er",
        '手' to "shou", '脚' to "jiao", '心' to "xin", '肚' to "du", '背' to "bei",
        
        // Common adjectives
        '好' to "hao", '坏' to "huai", '新' to "xin", '旧' to "jiu", '高' to "gao", '低' to "di",
        '长' to "chang", '短' to "duan", '快' to "kuai", '慢' to "man", '热' to "re", '冷' to "leng",
        '甜' to "tian", '酸' to "suan", '苦' to "ku", '辣' to "la", '咸' to "xian", '香' to "xiang",
        '臭' to "chou", '美' to "mei", '丑' to "chou", '胖' to "pang", '瘦' to "shou",
        
        // Technology
        '电' to "dian", '脑' to "nao", '手' to "shou", '机' to "ji", '电' to "dian", '话' to "hua",
        '网' to "wang", '络' to "luo", '软' to "ruan", '件' to "jian", '游' to "you", '戏' to "xi",
        '照' to "zhao", '片' to "pian", '相' to "xiang", '机' to "ji", '视' to "shi", '频' to "pin",
        
        // Food
        '饭' to "fan", '菜' to "cai", '肉' to "rou", '蛋' to "dan", '奶' to "nai", '糖' to "tang",
        '盐' to "yan", '油' to "you", '醋' to "cu", '茶' to "cha", '咖' to "ka", '啡' to "fei",
        '面' to "mian", '包' to "bao", '蛋' to "dan", '糕' to "gao",
        
        // Places
        '家' to "jia", '店' to "dian", '公' to "gong", '园' to "yuan", '学' to "xue", '校' to "xiao",
        '医' to "yi", '院' to "yuan", '银' to "yin", '行' to "hang", '车' to "che", '站' to "zhan",
        '机' to "ji", '场' to "chang", '城' to "cheng", '市' to "shi", '村' to "cun", '镇' to "zhen",
        
        // Time
        '早' to "zao", '晚' to "wan", '今' to "jin", '明' to "ming", '昨' to "zuo", '春' to "chun",
        '夏' to "xia", '秋' to "qiu", '冬' to "dong", '年' to "nian", '月' to "yue", '日' to "ri",
        '周' to "zhou", '时' to "shi", '分' to "fen", '秒' to "miao"
    )
    
    /**
     * Convert a Chinese text to possible Pinyin representations
     */
    fun toPinyin(text: String): List<String> {
        val result = mutableListOf<String>()
        val fullPinyin = StringBuilder()
        val initialPinyin = StringBuilder()
        
        for (char in text) {
            val pinyin = chineseToPinyin[char]
            if (pinyin != null) {
                fullPinyin.append(pinyin)
                if (pinyin.isNotEmpty()) {
                    initialPinyin.append(pinyin[0])
                }
            } else {
                // For non-Chinese characters, keep original
                fullPinyin.append(char.lowercase())
                if (char.isLetter()) {
                    initialPinyin.append(char.lowercase())
                }
            }
        }
        
        // Add original text
        result.add(text.lowercase())
        // Add full pinyin
        if (fullPinyin.toString() != text.lowercase()) {
            result.add(fullPinyin.toString())
        }
        // Add initial pinyin
        val initialStr = initialPinyin.toString()
        if (initialStr != text.lowercase() && initialStr != fullPinyin.toString()) {
            result.add(initialStr)
        }
        
        return result.distinct()
    }
    
    /**
     * Check if a label matches the search query considering Pinyin
     */
    fun matchesSearch(labelName: String, searchQuery: String): Boolean {
        if (searchQuery.isBlank()) return true
        
        val query = searchQuery.lowercase().trim()
        val pinyinVariants = toPinyin(labelName)
        
        return pinyinVariants.any { variant ->
            variant.contains(query)
        }
    }
}