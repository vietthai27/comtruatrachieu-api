package vn.comtruatrachieu.be.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.comtruatrachieu.be.member.Member;
import vn.comtruatrachieu.be.member.MemberRepository;

/**
 * Chạy lúc khởi động:
 * 1. Lần đầu (bảng tài khoản trống): tạo tài khoản cho mọi thành viên có sẵn
 *    (username = tên viết thường bỏ dấu, mật khẩu mặc định).
 * 2. Luôn đảm bảo có tài khoản admin (không gắn thành viên).
 */
@Component
public class AccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountSeeder.class);

    private final UserAccountRepository accounts;
    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final String defaultPassword;
    private final String adminUsername;
    private final String adminPassword;

    public AccountSeeder(UserAccountRepository accounts, MemberRepository members, PasswordEncoder passwordEncoder,
                         JdbcTemplate jdbc,
                         @Value("${app.seed.default-password}") String defaultPassword,
                         @Value("${app.admin.username}") String adminUsername,
                         @Value("${app.admin.password}") String adminPassword) {
        this.accounts = accounts;
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.defaultPassword = defaultPassword;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accounts.count() == 0) {
            seedMemberAccounts();
        }
        if (!accounts.existsByUsername(adminUsername)) {
            // Bảng tạo từ bản cũ có member_id NOT NULL; ddl-auto=update không tự bỏ ràng buộc này
            jdbc.execute("ALTER TABLE app_user ALTER COLUMN member_id DROP NOT NULL");
            accounts.save(UserAccount.admin(adminUsername, passwordEncoder.encode(adminPassword)));
            log.info("Tạo tài khoản quản trị '{}'", adminUsername);
        }
    }

    private void seedMemberAccounts() {
        String hash = passwordEncoder.encode(defaultPassword);
        for (Member member : members.findAll()) {
            String username = uniqueUsername(Usernames.fromFullName(member.getFullName()));
            accounts.save(new UserAccount(username, hash, member));
            log.info("Tạo tài khoản '{}' cho thành viên '{}'", username, member.getFullName());
        }
    }

    private String uniqueUsername(String base) {
        String candidate = base;
        for (int i = 2; accounts.existsByUsername(candidate) || candidate.equals(adminUsername); i++) {
            candidate = base + i;
        }
        return candidate;
    }
}
