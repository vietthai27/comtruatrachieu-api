package vn.comtruatrachieu.be.auth;

import java.text.Normalizer;
import java.util.Locale;

final class Usernames {

    private Usernames() {
    }

    /** "Việt Thái" -> "vietthai": bỏ dấu, viết thường, chỉ giữ a-z0-9. */
    static String fromFullName(String fullName) {
        String s = Normalizer.normalize(fullName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return s.isEmpty() ? "user" : s;
    }
}
