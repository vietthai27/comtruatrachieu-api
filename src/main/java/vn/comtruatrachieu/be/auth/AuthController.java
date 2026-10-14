package vn.comtruatrachieu.be.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import vn.comtruatrachieu.be.member.Member;
import vn.comtruatrachieu.be.member.MemberController.MemberRequest;
import vn.comtruatrachieu.be.member.MemberRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank(message = "Nhập tên đăng nhập") String username,
                               @NotBlank(message = "Nhập mật khẩu") String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "Nhập tên đăng nhập")
            @Pattern(regexp = "[a-z0-9._]{3,30}", message = "Tên đăng nhập 3–30 ký tự: chữ thường không dấu, số, dấu . hoặc _")
            String username,
            @NotBlank(message = "Nhập mật khẩu") @Size(min = 6, max = 100, message = "Mật khẩu tối thiểu 6 ký tự")
            String password,
            @NotBlank(message = "Họ tên không được để trống") String fullName,
            @NotBlank(message = "Chưa chọn ngân hàng") String bankBin,
            @NotBlank(message = "Số tài khoản không được để trống")
            @Pattern(regexp = "\\d{4,30}", message = "Số tài khoản chỉ gồm chữ số") String accountNo) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Nhập mật khẩu hiện tại") String currentPassword,
            @NotBlank(message = "Nhập mật khẩu mới") @Size(min = 6, max = 100, message = "Mật khẩu mới tối thiểu 6 ký tự")
            String newPassword) {
    }

    /** member = null với tài khoản admin. */
    public record UserInfo(String username, String role, Member member) {
        static UserInfo of(UserAccount account) {
            return new UserInfo(account.getUsername(), account.getRole(), account.getMember());
        }
    }

    public record AuthResponse(String token, UserInfo user) {
    }

    private final UserAccountRepository accounts;
    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration ttl;

    public AuthController(UserAccountRepository accounts, MemberRepository members, PasswordEncoder passwordEncoder,
                          JwtEncoder jwtEncoder, @Value("${app.jwt.ttl-days}") long ttlDays) {
        this.accounts = accounts;
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.ttl = Duration.ofDays(ttlDays);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount account = accounts.findByUsername(request.username().trim().toLowerCase())
                .filter(a -> passwordEncoder.matches(request.password(), a.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu"));
        return new AuthResponse(issueToken(account), UserInfo.of(account));
    }

    /** Tự đăng ký: tạo thành viên mới rồi tạo tài khoản gắn với thành viên đó. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        if (accounts.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên đăng nhập đã có người dùng");
        }
        Member member = new Member();
        member.setFullName(request.fullName().trim());
        member.setBankBin(request.bankBin().trim());
        member.setAccountNo(request.accountNo().trim());
        member = members.save(member);

        UserAccount account = accounts.save(
                new UserAccount(request.username(), passwordEncoder.encode(request.password()), member));
        return new AuthResponse(issueToken(account), UserInfo.of(account));
    }

    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal Jwt jwt) {
        return UserInfo.of(current(jwt));
    }

    /** Tự sửa thông tin thành viên của chính mình (họ tên, ngân hàng, số tài khoản). */
    @PutMapping("/me")
    public UserInfo updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberRequest request) {
        UserAccount account = current(jwt);
        Member member = account.getMember();
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản quản trị không gắn với thành viên nào");
        }
        member.setFullName(request.fullName().trim());
        member.setBankBin(request.bankBin().trim());
        member.setAccountNo(request.accountNo().trim());
        members.save(member);
        return UserInfo.of(account);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        UserAccount account = current(jwt);
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        accounts.save(account);
    }

    private UserAccount current(Jwt jwt) {
        return accounts.findByUsername(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không còn tồn tại"));
    }

    private String issueToken(UserAccount account) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(account.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                // SecurityConfig đọc claim "roles" thành quyền ROLE_*
                .claim("roles", List.of(account.getRole()));
        if (account.getMember() != null) {
            claims.claim("memberId", account.getMember().getId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
