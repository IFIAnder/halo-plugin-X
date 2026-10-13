package run.halo.pluginx;

/**
 * 扩展资料的更新请求。
 *
 * <p>字段语义：{@code null} 表示「不修改」，空字符串表示「删除该字段」。
 *
 * @param cover 个人主页背景图，可以是完整 URL，也可以是 Halo 附件路径（/upload/xxx.png）
 * @param location 位置，纯文本
 * @param website 个人网站
 */
public record ProfileRequest(String cover, String location, String website) {
}
