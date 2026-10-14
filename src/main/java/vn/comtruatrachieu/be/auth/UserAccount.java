package vn.comtruatrachieu.be.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import vn.comtruatrachieu.be.member.Member;

/** Tài khoản đăng nhập — mỗi tài khoản gắn với đúng 1 thành viên. Bảng tên app_user vì "user" là từ khoá Postgres. */
@Entity
@Table(name = "app_user")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    /** null với tài khoản admin (admin không phải thành viên chia tiền). */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id", unique = true)
    private Member member;

    /** "ADMIN" hoặc null (= người dùng thường). Để nullable để thêm cột vào bảng có sẵn không lỗi. */
    private String role;

    protected UserAccount() {
    }

    public UserAccount(String username, String passwordHash, Member member) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.member = member;
    }

    public static UserAccount admin(String username, String passwordHash) {
        UserAccount account = new UserAccount(username, passwordHash, null);
        account.role = ADMIN;
        return account;
    }

    public static final String ADMIN = "ADMIN";

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Member getMember() { return member; }
    public String getRole() { return role == null ? "USER" : role; }
    public boolean isAdmin() { return ADMIN.equals(role); }
}
