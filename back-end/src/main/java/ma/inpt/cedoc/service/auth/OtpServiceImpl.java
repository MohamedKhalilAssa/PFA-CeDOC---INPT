package ma.inpt.cedoc.service.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final RedisTemplate<String, String> redisTemplate;
    private final int expirationInMinutes = 5;

    private String generateOtp(String characters, Integer length) {
        StringBuilder otp = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(characters.length());
            otp.append(characters.charAt(index));
        }
        return otp.toString();
    }

    private String getCacheKey(long id) {
        return "otp:%s".formatted(id);
    }

    public boolean canResendOtp(long id) {
        final var cacheKey = getCacheKey(id);
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);

        if (ttl == null || ttl == -2) {
            return true;
        }

        if (ttl > 0 && ttl > (expirationInMinutes - 1) * 60) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isOtpValid(final long id, final String otp) {
        final var cacheKey = getCacheKey(id);
        return Objects.equals(
                redisTemplate.opsForValue().get(cacheKey), otp);
    }

    @Override
    public String generateAndStoreOtp(final long id) {
        final var otp = generateOtp("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789", 10);
        final var cacheKey = getCacheKey(id);

        redisTemplate.opsForValue().set(
                cacheKey, otp, Duration.ofMinutes(expirationInMinutes));

        return otp;
    }

    @Override
    public void deleteOtp(final long id) {
        final var cacheKey = getCacheKey(id);
        redisTemplate.delete(cacheKey);
    }

}
