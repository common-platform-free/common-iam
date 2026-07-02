package com.huangjie.commoniam.service;

import com.huangjie.commoniam.common.ErrorCode;
import com.huangjie.commoniam.exception.BusinessException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 模拟验证码服务。
 *
 * <p>第一版不接短信/邮箱供应商，不落库、不用 Redis。
 * 验证码保存在本机内存中，并通过日志输出，方便本地复制到前端验证。</p>
 */
@Slf4j
@Service
public class MockCaptchaService {

    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final int CAPTCHA_BOUND = 1_000_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, CaptchaRecord> captchaStore = new ConcurrentHashMap<>();

    /**
     * 生成 6 位数字验证码，并输出到服务端日志。
     */
    public void generate(String username) {
        String normalizedUsername = normalizeUsername(username);
        String code = String.format("%06d", SECURE_RANDOM.nextInt(CAPTCHA_BOUND));
        Instant expiresAt = Instant.now().plus(CAPTCHA_TTL);
        captchaStore.put(normalizedUsername, new CaptchaRecord(code, expiresAt));
        log.info("模拟验证码 username={}, code={}, expiresAt={}", normalizedUsername, code, expiresAt);
    }

    /**
     * 校验验证码。
     *
     * <p>验证码校验成功后立即删除，避免同一个验证码重复使用。</p>
     */
    public void verify(String username, String code) {
        String normalizedUsername = normalizeUsername(username);
        CaptchaRecord record = captchaStore.get(normalizedUsername);
        if (record == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "验证码不存在或已过期");
        }
        if (Instant.now().isAfter(record.expiresAt())) {
            captchaStore.remove(normalizedUsername);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "验证码不存在或已过期");
        }
        if (!record.code().equals(code)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "验证码错误");
        }
        captchaStore.remove(normalizedUsername);
    }

    /**
     * 统一 username 格式，避免大小写或前后空格导致验证码无法匹配。
     */
    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private record CaptchaRecord(String code, Instant expiresAt) {
    }
}
