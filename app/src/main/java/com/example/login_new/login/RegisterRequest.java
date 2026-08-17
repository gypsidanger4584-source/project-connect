//package com.example.login_new;
//
//import com.android.volley.AuthFailureError;
//import com.android.volley.Response;
//import com.android.volley.toolbox.StringRequest;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class RegisterRequest extends StringRequest {
//
//    // 실제 사용 시 아래 URL에 웹서버 주소 입력
//    final static private String URL = "http://yourserver.com/Register.php";
//
//    private Map<String, String> parameters;
//
//    public RegisterRequest(String userID, String userPassword, String userGender,
//                           String userMajor, String userEmail, Response.Listener<String> listener) {
//        super(Method.POST, URL, listener, null);
//        parameters = new HashMap<>();
//        parameters.put("userID", userID);
//        parameters.put("userPassword", userPassword);
//        parameters.put("userGender", userGender);
//        parameters.put("userMajor", userMajor);
//        parameters.put("userEmail", userEmail);
//    }
//
//    @Override
//    protected Map<String, String> getParams() throws AuthFailureError {
//        return parameters;
//    }
//}
