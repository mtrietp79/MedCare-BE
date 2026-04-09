package com.medcare.clinic_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Khóa bí mật để ký JWT (Giống như con dấu chống làm giả của phòng khám)
    // Cần ít nhất 256-bit (32 ký tự). Mình cấu hình mặc định luôn cho bạn dễ chạy.
    @Value("${jwt.secret:MotChuoiBaoMatCucKyDaiVaKhoDoanChoMedCareClinic123456789}")
    private String jwtSecret;

    // Thời gian sống của Token (Ví dụ: 24 giờ = 86400000 milliseconds)
    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationDate;

    // Hàm tạo "con dấu" từ Khóa bí mật
    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // 1. In thẻ: Tạo Token khi đăng nhập thành công
    public String generateToken(String username) {
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationDate);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date()) // Ngày tạo
                .setExpiration(expireDate) // Ngày hết hạn
                .signWith(key(), SignatureAlgorithm.HS256) // Đóng dấu bảo mật
                .compact();
    }

    // 2. Đọc thẻ: Lấy Username từ Token
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 3. Kiểm tra thẻ: Xem thẻ có hợp lệ không
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // Token bị làm giả, bị sửa đổi, hoặc đã hết hạn
            return false;
        }
    }
}