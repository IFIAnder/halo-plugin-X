package run.halo.pluginx;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import run.halo.app.core.extension.User;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 扩展资料的读写。
 *
 * <p>数据不新建资源，直接挂在 {@link User} 的 {@code metadata.annotations} 上 —— 这样
 * halo-theme-X 只要读注解就能用，两边不需要额外的接口约定：
 *
 * <ul>
 *   <li>{@code theme-x/cover} 个人主页背景图</li>
 *   <li>{@code theme-x/location} 位置</li>
 *   <li>{@code website} 个人网站（沿用通行写法，评论插件也读这个键）</li>
 * </ul>
 *
 * <p>写入用的是 {@link ReactiveExtensionClient}，跑在系统身份下，不受「用户只能改自己
 * 的 displayName / bio」那条限制；越权由本类自己把关 —— 更新永远只作用于当前登录用户。
 */
@Service
public class ProfileService {

    public static final String ANNO_COVER = "theme-x/cover";
    public static final String ANNO_LOCATION = "theme-x/location";
    public static final String ANNO_WEBSITE = "website";

    private static final int MAX_URL_LENGTH = 512;
    private static final int MAX_LOCATION_LENGTH = 64;

    private final ReactiveExtensionClient client;

    public ProfileService(ReactiveExtensionClient client) {
        this.client = client;
    }

    /**
     * 当前登录用户的用户名，匿名时直接 401，避免后面拿着 anonymousUser 去查用户。
     */
    public Mono<String> currentUsername() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication())
            .filter(auth -> auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName()))
            .map(Authentication::getName)
            .switchIfEmpty(Mono.error(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "需要登录后才能修改资料")));
    }

    public Mono<User> currentUser() {
        return currentUsername().flatMap(name -> client.fetch(User.class, name));
    }

    /**
     * 读取指定用户的扩展资料，只返回非敏感的公开字段。
     */
    public Mono<Map<String, String>> read(String username) {
        return client.fetch(User.class, username)
            .map(ProfileService::toMap)
            .switchIfEmpty(Mono.error(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")));
    }

    public Mono<Map<String, String>> update(ProfileRequest request) {
        if (request == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求正文不能为空"));
        }
        return currentUser()
            .flatMap(user -> {
                var annotations = MetadataUtil.nullSafeAnnotations(user);
                apply(annotations, ANNO_COVER, request.cover(), MAX_URL_LENGTH);
                apply(annotations, ANNO_LOCATION, request.location(), MAX_LOCATION_LENGTH);
                apply(annotations, ANNO_WEBSITE, request.website(), MAX_URL_LENGTH);
                // User 是热点资源，并发更新会撞乐观锁，退避重试几次
                return client.update(user)
                    .retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                        .filter(OptimisticLockingFailureException.class::isInstance));
            })
            .map(ProfileService::toMap);
    }

    /**
     * 把 User 上的注解整理成对外返回的结构。字段缺失时给空串，前端不用处理 null。
     */
    public static Map<String, String> toMap(User user) {
        var annotations = MetadataUtil.nullSafeAnnotations(user);
        var map = new LinkedHashMap<String, String>(4);
        map.put("username", user.getMetadata().getName());
        map.put("cover", annotations.getOrDefault(ANNO_COVER, ""));
        map.put("location", annotations.getOrDefault(ANNO_LOCATION, ""));
        map.put("website", annotations.getOrDefault(ANNO_WEBSITE, ""));
        return map;
    }

    private static void apply(Map<String, String> annotations, String key, String value, int maxLength) {
        if (value == null) {
            return;
        }
        var trimmed = value.strip();
        if (trimmed.isEmpty()) {
            annotations.remove(key);
            return;
        }
        if (trimmed.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                key + " 长度不能超过 " + maxLength + " 个字符");
        }
        if (trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " 不能包含换行");
        }
        annotations.put(key, trimmed);
    }
}
