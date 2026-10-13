package run.halo.pluginx;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import java.util.Map;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * 扩展资料接口。
 *
 * <p>主题用 GET 探测本插件是否可用：拿不到（404 / 网络错误）就不显示相关入口，
 * 所以不装插件时主题仍然完整可用，只是没有这几项自助设置。
 */
@Component
public class ProfileEndpoint implements CustomEndpoint {

    private final ProfileService profiles;

    public ProfileEndpoint(ProfileService profiles) {
        this.profiles = profiles;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "XProfileV1alpha1";
        return SpringdocRouteBuilder.route()
            .GET("my-profile", this::getMine, builder -> builder
                .operationId("getMyProfile")
                .tag(tag)
                .description("获取当前登录用户的扩展资料（背景图 / 位置 / 网站）")
                .response(responseBuilder().implementation(Map.class)))
            .PUT("my-profile", this::updateMine, builder -> builder
                .operationId("updateMyProfile")
                .tag(tag)
                .description("更新当前登录用户的扩展资料。字段为 null 表示不修改，空字符串表示删除")
                .requestBody(requestBodyBuilder().required(true).implementation(ProfileRequest.class))
                .response(responseBuilder().implementation(Map.class)))
            .GET("profiles/{username}", this::getOne, builder -> builder
                .operationId("getProfile")
                .tag(tag)
                .description("按用户名读取公开的扩展资料")
                .response(responseBuilder().implementation(Map.class)))
            .build();
    }

    private Mono<ServerResponse> getMine(ServerRequest request) {
        return profiles.currentUser()
            .map(ProfileService::toMap)
            .flatMap(map -> ServerResponse.ok().bodyValue(map));
    }

    private Mono<ServerResponse> updateMine(ServerRequest request) {
        return request.bodyToMono(ProfileRequest.class)
            .flatMap(profiles::update)
            .flatMap(map -> ServerResponse.ok().bodyValue(map));
    }

    private Mono<ServerResponse> getOne(ServerRequest request) {
        return profiles.read(request.pathVariable("username"))
            .flatMap(map -> ServerResponse.ok().bodyValue(map));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("x.api.halo.run/v1alpha1");
    }
}
