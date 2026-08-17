//package com.example.login_new.alogic;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import com.android.volley.Response;
//import com.android.volley.toolbox.StringRequest;
//import com.android.volley.AuthFailureError;
//
////여긴 회원가입 여부를 체크하는 클래스임
//public class ValidateRequest extends StringRequest {
//    //위의 StringRequest는 데이터 베이스를 gradle.module에 연동을 시키고 해야하는데 없으니까 오류가 뜰거임
//    final static private String URL = "내 웹서버 입력할것,userValidate가 될곳임";
//    private Map<String, String> parameters;
//
//    //유저아이디를 사이트로 보냄으로서 회원가입이 가능한 아이디인지 요청이 가능한 클래스로 만들거임
//    public ValidateRequest(String userID, Response.Listener<String> listener){
//        super(Method.POST,URL,listener,null);
//        parameters=new HashMap<>();
//        parameters.put("userID",userID);
//    }
//    @Override
//    public Map<String, String> getParams(){
//        return parameters;
//    }
//}
