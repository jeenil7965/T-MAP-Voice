package com.sm3.audiosuite;

public class MokpoSpeech {
    public static String convert(String raw) {
        String s = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
        if (s.length() == 0) return "";
        if (s.contains("좌회전")) return "아따아, 쪼까 있다가, 좌회전 이랑께요오.";
        if (s.contains("우회전")) return "오메에, 오른쪽으로 살짝 붙으랑께요오.";
        if (s.contains("유턴")) return "아따, 앞에서 유턴허면 되것소잉.";
        if (s.contains("회전교차로")) return "회전 교차로여라. 천천히 들어가쇼잉.";
        if (s.contains("고속도로") && (s.contains("진입") || s.contains("입구"))) return "이제 고속도로 들어간당께요. 안전거리 챙기쇼잉.";
        if (s.contains("나들목") || s.contains("IC") || s.contains("진출")) return "아따, 나갈 준비 허쇼잉. 오른쪽 잘 보셔야 쓰겄소.";
        if (s.contains("단속") || s.contains("카메라") || s.contains("과속")) return "오메, 앞에 단속 있당께요. 속도 쪼까 줄이쇼잉.";
        if (s.contains("어린이") || s.contains("보호구역")) return "어린이 보호구역이여라. 천천히 조심허쇼잉.";
        if (s.contains("목적지") || s.contains("도착")) return "고생 많았소잉. 목적지에 다 와부렀당께요.";
        if (s.contains("직진")) return "그대로 쭉 가면 되것소잉.";
        if (s.contains("차로")) return "차로 쪼까 맞춰가쇼잉. 급하게 허지 말고요.";
        return "";
    }
}
