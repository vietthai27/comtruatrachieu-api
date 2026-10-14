package vn.comtruatrachieu.be.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import vn.comtruatrachieu.be.member.Member;

/** Tra tài khoản / thành viên của người đang gọi API từ JWT. */
@Component
public class CurrentUser {

    private final UserAccountRepository accounts;

    public CurrentUser(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    public UserAccount account(Jwt jwt) {
        return accounts.findByUsername(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không còn tồn tại"));
    }

    /** Thành viên gắn với tài khoản; tài khoản admin không có thành viên nên không dùng được các chức năng chia tiền. */
    public Member member(Jwt jwt) {
        Member member = account(jwt).getMember();
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản quản trị không gắn với thành viên nào");
        }
        return member;
    }
}
