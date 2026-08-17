package com.example.login_new.login;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class SQLiteHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mydb.sqlite";
    private static final int DATABASE_VERSION = 4;

    public SQLiteHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE building_info (name TEXT PRIMARY KEY);");

        sqLiteDatabase.execSQL("INSERT INTO building_info (name) VALUES\n" +
                "('학생복지센터'),\n" +
                "('문화 1관'),\n" +
                "('문화 2관'),\n" +
                "('공학 1관'),\n" +
                "('공학 2관'),\n" +
                "('학술정보관'),\n" +
                "('도의관'),\n" +
                "('식품과학관'),\n" +
                "('연성대학교'),\n" +
                "('창의교육센터'),\n" +
                "('창조관'),\n" +
                "('연곡문화센터'),\n" +
                "('자연과학관');");
        sqLiteDatabase.execSQL("CREATE TABLE building_departments (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    building_name TEXT NOT NULL, \n" +
                "    department_name TEXT NOT NULL,\n" +
                "    FOREIGN KEY (building_name) REFERENCES building_info(name)\n" +
                ");\n");
        sqLiteDatabase.execSQL("INSERT INTO building_departments (id, building_name, department_name) VALUES\n" +
                "(1, '학생복지센터', '학생취업처'),\n" +
                "(2, '학생복지센터', '학생상담센터'),\n" +
                "(3, '학생복지센터', '장애학생지원센터'),\n" +
                "(4, '학생복지센터', '원스톱서비스센터'),\n" +
                "(5, '학생복지센터', '예비군대대'),\n" +
                "(6, '문화 1관', '패션디자인비즈니스과'),\n" +
                "(7, '문화 1관', '뷰티스타일리스트과 헤어디자인전공'),\n" +
                "(8, '문화 1관', '뷰티스타일리스트과 메이크업전공'),\n" +
                "(9, '문화 1관', '뷰티스타일리스트과 스킨케어전공'),\n" +
                "(10, '문화 1관', 'K-POP과'),\n" +
                "(11, '문화 2관', '시각디자인과'),\n" +
                "(12, '문화 2관', '영상콘텐츠과'),\n" +
                "(13, '문화 2관', 'K-POP과'),\n" +
                "(14, '공학 1관', '전자공학과'),\n" +
                "(15, '공학 1관', '정보통신과'),\n" +
                "(16, '공학 1관', '전기과'),\n" +
                "(17, '공학 1관', '컴퓨터소프트웨어과'),\n" +
                "(18, '공학 2관', '스포츠재활과'),\n" +
                "(19, '도의관', '응급구조과'),\n" +
                "(20, '도의관', '경찰경호보안과'),\n" +
                "(21, '도의관', '군사학과'),\n" +
                "(22, '식품과학관', '식품영양학과'),\n" +
                "(23, '식품과학관', '카페베이커리과'),\n" +
                "(24, '식품과학관', '호텔외식조리과 호텔조리전공'),\n" +
                "(25, '식품과학관', '호텔외식조리과 호텔외식경영전공'),\n" +
                "(26, '연성대학교', '유통물류과'),\n" +
                "(27, '연성대학교', '경영학과'),\n" +
                "(28, '연성대학교', '세무회계과'),\n" +
                "(29, '창의교육센터', '작업치료과'),\n" +
                "(30, '창의교육센터', '자유전공학과'),\n" +
                "(31, '창조관', '치위생과'),\n" +
                "(32, '창조관', '치기공과'),\n" +
                "(33, '창조관', '건축과'),\n" +
                "(34, '창조관', '실내건축과'),\n" +
                "(35, '창조관', '항공서비스과'),\n" +
                "(36, '창조관', '호텔관광과'),\n" +
                "(37, '창조관', '관광영어과'),\n" +
                "(38, '창조관', '보건의료행정과'),\n" +
                "(39, '연곡문화센터', '유아교육과'),\n" +
                "(40, '연곡문화센터', '유아특수재활과'),\n" +
                "(41, '연곡문화센터', '사회복지과 사회복지전공'),\n" +
                "(42, '연곡문화센터', '사회복지과 아동심리보육전공'),\n" +
                "(43, '연곡문화센터', '게임콘텐츠과'),\n" +
                "(44, '연곡문화센터', '웹툰만화콘텐츠과'),\n" +
                "(45, '연곡문화센터', '사회복지경영과'),\n" +
                "(46, '자연과학관', '반려동물보건과'),\n" +
                "(47, '자연과학관', '반려동물산업과');");

        sqLiteDatabase.execSQL("CREATE TABLE building_spaces (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    building_name TEXT NOT NULL,\n" +
                "    space_type TEXT NOT NULL,\n" +
                "    FOREIGN KEY (building_name) REFERENCES building_info(name)\n" +
                ");");
        sqLiteDatabase.execSQL("INSERT INTO building_spaces (id, building_name, space_type) VALUES\n" +
                "(1, '학생복지센터', '커리어라운지'),\n" +
                "(2, '학생복지센터', '잼카페'),\n" +
                "(3, '학생복지센터', '잼비즈마켓'),\n" +
                "(4, '학생복지센터', '모의면접실'),\n" +
                "(5, '학생복지센터', '보건실'),\n" +
                "(6, '학생복지센터', 'AI실습실'),\n" +
                "(7, '학생복지센터', '레몬트리'),\n" +
                "(8, '학생복지센터', 'e-mart24'),\n" +
                "(9, '학생복지센터', '학생식당'),\n" +
                "(10, '학생복지센터', '교직원식당'),\n" +
                "(11, '학생복지센터', '서점'),\n" +
                "(12, '문화 1관', '아트홀'),\n" +
                "(13, '문화 2관', '뷰티하이플렉스실'),\n" +
                "(14, '문화 2관', '커뮤니케이션 실습실'),\n" +
                "(15, '공학 2관', '공학계열 강의실/실습실'),\n" +
                "(16, '공학 2관', '실내체육관'),\n" +
                "(17, '학술정보관', '도서관'),\n" +
                "(18, '학술정보관', '전자계산소'),\n" +
                "(19, '학술정보관', '인터넷플라자'),\n" +
                "(20, '학술정보관', '영화관'),\n" +
                "(21, '도의관', '총학생회'),\n" +
                "(22, '도의관', '대의원회'),\n" +
                "(23, '도의관', '신문방송국'),\n" +
                "(24, '도의관', '동아리실'),\n" +
                "(25, '연성대학교', '이사장실'),\n" +
                "(26, '연성대학교', '총장실'),\n" +
                "(27, '연성대학교', '부총장실'),\n" +
                "(28, '연성대학교', '혁신지원사업단'),\n" +
                "(29, '연성대학교', '기획처'),\n" +
                "(30, '연성대학교', 'IR센터'),\n" +
                "(31, '연성대학교', '산학협력처'),\n" +
                "(32, '연성대학교', '산학협력단'),\n" +
                "(33, '연성대학교', '현장실습지원센터'),\n" +
                "(34, '연성대학교', '창업교육지원센터'),\n" +
                "(35, '연성대학교', '교무처'),\n" +
                "(36, '연성대학교', '교육혁신본부'),\n" +
                "(37, '연성대학교', '교양교육혁신센터'),\n" +
                "(38, '연성대학교', '학사학위지원센터'),\n" +
                "(39, '연성대학교', '입학홍보처'),\n" +
                "(40, '연성대학교', '행정지원처'),\n" +
                "(41, '연성대학교', '법인사무처'),\n" +
                "(42, '연성대학교', '역사홍보관'),\n" +
                "(43, '창의교육센터', '인스퍼레이션홀'),\n" +
                "(44, '창의교육센터', '교수학습지원센터'),\n" +
                "(45, '창의교육센터', '서비스러닝센터'),\n" +
                "(46, '창의교육센터', '창의실습실'),\n" +
                "(47, '창의교육센터', '온라인콘텐츠제작실'),\n" +
                "(48, '창의교육센터', '파티쉐&바리스타 실습실'),\n" +
                "(49, '창의교육센터', '식음료실습실'),\n" +
                "(50, '창의교육센터', '카페 플래닛 37'),\n" +
                "(51, '창의교육센터', '연성별마당'),\n" +
                "(52, '창의교육센터', '통통계단'),\n" +
                "(53, '연곡문화센터', '생활관'),\n" +
                "(54, '연곡문화센터', '게스트하우스'),\n" +
                "(55, '연곡문화센터', '컨벤션홀'),\n" +
                "(56, '연곡문화센터', '잉글리쉬 카페'),\n" +
                "(57, '연곡문화센터', '연곡갤러리'),\n" +
                "(58, '연곡문화센터', '평생교육원'),\n" +
                "(59, '연곡문화센터', '국제교류원');\n");
        sqLiteDatabase.execSQL("CREATE TABLE departments (\n" +
                "    department_name VARCHAR(100) NOT NULL,\n" +
                "    building_name VARCHAR(100),\n" +
                "    room_number VARCHAR(20),\n" +
                "    FOREIGN KEY (building_name) REFERENCES building_info(name)\n" +
                ");");
        sqLiteDatabase.execSQL("INSERT INTO departments (department_name, building_name, room_number) VALUES\n" +
                "('전자공학과', '공학 1관', '410호'),\n" +
                "('정보통신과', '공학 1관', '210호'),\n" +
                "('전기과', '공학 1관', '308호'),\n" +
                "('컴퓨터소프트웨어과', '공학 1관', '113호'),\n" +
                "('건축과', '창조관', '110호'),\n" +
                "('실내건축과', '창조관', '110호'),\n" +
                "('패션디자인비즈니스과', '문화 1관', '514호'),\n" +
                "('뷰티스타일리스트과 헤어디자인전공', '문화 1관', '207호'),\n" +
                "('뷰티스타일리스트과 메이크업전공', '문화 1관', '207호'),\n" +
                "('뷰티스타일리스트과 스킨케어전공', '문화 1관', '207호'),\n" +
                "('게임콘텐츠과', '연곡문화센터', '711호'),\n" +
                "('웹툰만화콘텐츠과', '연곡문화센터', '711호'),\n" +
                "('영상콘텐츠과 영상콘텐츠젠작전공', '문화 2관', '514호'),\n" +
                "('영상콘텐츠과 뉴미디어콘텐츠전공', '문화 2관', '514호'),\n" +
                "('시간디자인과', '문화 2관', '514호'),\n" +
                "('유통물류과', '연성대학교', '410호'),\n" +
                "('경영학과', '연성대학교', '410호'),\n" +
                "('세무회계과', '연성대학교', '410호'),\n" +
                "('국방군사학과', '도의관', '1층'),\n" +
                "('경찰경호보안과', '도의관', '105호'),\n" +
                "('사회복지과 사회복지전공', '연곡문화센터', '102호'),\n" +
                "('유아교육과', '연곡문화센터', '101호'),\n" +
                "('유아특수재활과', '연곡문화센터', '102호'),\n" +
                "('사회복지과 아동심리보육전공', '연곡문화센터', '101호'),\n" +
                "('치위생과', '창조관', 'B1'),\n" +
                "('치기공과', '창조관', 'B1'),\n" +
                "('응급구조과', '도의관', '3층'),\n" +
                "('보건의료행정과', '창조관', '309호'),\n" +
                "('스포츠재활과', '공학 2관', '106호'),\n" +
                "('식품영양학과', '식품과학관', '210호'),\n" +
                "('반려동물보건과', '자연과학관', '102호'),\n" +
                "('반려동물산업과', '자연과학관', '1층'),\n" +
                "('항공서비스과', '창조관', '510호'),\n" +
                "('관광영어과', '창조관', '431호'),\n" +
                "('호텔관광과', '창조관', '431호'),\n" +
                "('호텔외식조리과', '식품과학관', '403호'),\n" +
                "('카페베이커리과', '식품과학관', '209호'),\n" +
                "('호텔외식조리과 호텔외식경영전공', '식품과학관', '403호');");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS building_info");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS building_departments");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS building_spaces");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS departments");
        onCreate(sqLiteDatabase);
    }
    // 1. building_departments 테이블에서 학과 이름 조회
    public ArrayList<String> getDepartmentsByBuilding(String buildingName) {
        ArrayList<String> departments = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT department_name FROM building_departments WHERE building_name = ?", new String[]{buildingName});

        if (cursor.moveToFirst()) {
            do {
                String department = cursor.getString(0);
                departments.add(department);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return departments;
    }

    public ArrayList<String> getSpacesByBuilding(String buildingName) {
        ArrayList<String> spaces = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT space_type FROM building_spaces WHERE building_name = ?", new String[]{buildingName});
        while (cursor.moveToNext()) {
            spaces.add(cursor.getString(0));
        }
        cursor.close();
        return spaces;
    }
    // 2. departments 테이블에서 사무실 위치 조회
    public ArrayList<String> getDepartmentOfficesByBuilding(String buildingName) {
        ArrayList<String> offices = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT department_name || ' - ' || room_number FROM departments WHERE building_name = ?", new String[]{buildingName});

        if (cursor.moveToFirst()) {
            do {
                offices.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return offices;
    }

    public ArrayList<String> searchAllTables(String keyword) {
        ArrayList<String> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // building_info 검색
        Cursor cursor1 = db.rawQuery("SELECT name FROM building_info WHERE name LIKE ?", new String[]{"%" + keyword + "%"});
        while (cursor1.moveToNext()) {
            results.add("건물: " + cursor1.getString(0));
        }
        cursor1.close();

        // building_departments 검색
        Cursor cursor2 = db.rawQuery("SELECT building_name, department_name FROM building_departments WHERE department_name LIKE ?", new String[]{"%" + keyword + "%"});
        while (cursor2.moveToNext()) {
            results.add("학과: " + cursor2.getString(1) + " (건물: " + cursor2.getString(0) + ")");
        }
        cursor2.close();

        // building_spaces 검색
        Cursor cursor3 = db.rawQuery("SELECT building_name, space_type FROM building_spaces WHERE space_type LIKE ?", new String[]{"%" + keyword + "%"});
        while (cursor3.moveToNext()) {
            results.add("공간: " + cursor3.getString(1) + " (건물: " + cursor3.getString(0) + ")");
        }
        cursor3.close();

        // departments 테이블 검색 (학과 + 건물 + 호실)
        Cursor cursor4 = db.rawQuery(
                "SELECT department_name, building_name FROM departments WHERE department_name LIKE ?",
                new String[]{"%" + keyword + "%"}
        );
        while (cursor4.moveToNext()) {
            results.add("학과: " + cursor4.getString(0) + " (건물: " + cursor4.getString(1) + ")");
        }
        cursor4.close();

        db.close();

        return results;
    }

}
