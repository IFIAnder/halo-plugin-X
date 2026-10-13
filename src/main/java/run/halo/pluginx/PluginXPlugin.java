package run.halo.pluginx;

import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * 插件入口。
 *
 * <p>Halo 会为插件里的 {@link BasePlugin} 实现类自动生成组件索引，
 * 所以这里不需要 {@code @Component} 注解。
 */
public class PluginXPlugin extends BasePlugin {

    public PluginXPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }
}
