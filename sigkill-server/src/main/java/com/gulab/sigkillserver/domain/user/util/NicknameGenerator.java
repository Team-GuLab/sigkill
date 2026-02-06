package com.gulab.sigkillserver.domain.user.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NicknameGenerator {
    private static final List<String> ADJECTIVES = List.of(
            "따스한", "포근한", "은은한", "꿈꾸는", "불꽃같은", "잔잔한", "두근두근", "웃음 가득한", "이야기하는", "감성적인",
            "추억 속의", "바람 속의", "별빛 아래", "달빛에 물든", "기쁨의", "가슴 뛰는", "캠프파이어", "모닥불 옆", "설레는", "아련한",
            "빛나는", "행복한", "밝은", "따뜻한", "신비로운", "진지한", "외로운", "진중한", "무덤덤한", "졸린", "차분한",
            "사색하는", "고요한", "사랑스러운", "온화한", "마음의", "정다운", "부드러운", "편안한", "아늑한", "믿음직한",
            "미소짓는", "소중한", "전설의", "야릇한", "어두침침한", "음흉한", "흉악한", "괴랄한", "고통받는", "음침한", "기진맥진한", "밤샌",
            "뽀짝한", "통통 튀는", "네모네모", "둥근", "뾰족한", "둥글둥글", "뽀족한", "알싸한", "깐깐한", "민초맛"
    );
    private static final List<String> CHARACTERS = List.of(
            "꿈돌이", "마시멜로", "불씨", "밤송이", "모닥이", "희망이", "추억이", "잔불이", "담쟁이", "별이", "도란이", "푸른밤",
            "캠프리", "도깨비", "호롱이", "파이어링", "휘파람", "바람돌이", "햇살이", "달맞이", "소원돌이", "불꽃요정", "설렘이", "여우비",
            "달빛이", "별빛이", "행복이", "마법사", "꿈꾸는이", "모험가", "기쁨이", "상상가", "여행자", "정원사", "몽상가", "행운이",
            "아이스크림", "레전드", "쿼크", "구렁이", "감자", "토끼", "탕후루", "마라탕", "떡볶이"
    );

    private NicknameGenerator() {
    }

    /**
     * 랜덤 닉네임 생성 메서드
     *
     * @return 생성된 닉네임
     */
    public static String generateRandomNickname() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String character = CHARACTERS.get(random.nextInt(CHARACTERS.size()));
        return adjective + " " + character;
    }
}