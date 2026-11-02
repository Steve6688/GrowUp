# 栖养 · Wellnest 健康管理网页

这是与 GrowUp 主分支隔离的 **wellnest-public-demo** 演示分支。该分支的 `index.html` 是带内嵌样式及脚本的独立静态网站。

**演示内容**：首次打开自动填充 14 天虚构的饮食、运动、饮水、步数、睡眠和身心状态记录，支持添加/修改/删除、趋势图、CSV 周报、JSON 备份、目标设置及深色模式。

**隐私**：公开仓库内没有任何真实的用户健康资料。所有后来填写的记录只存储在访问者的浏览器 `localStorage`，不会上传至 GitHub 或在其他设备自动同步。顶部会持续显示“演示模式”提示；点击“清除示例，开始记录”可删除演示数据。请定期备份。

## 发布 GitHub Pages

当前分支已准备好作为 Pages 的发布源。在 `Steve6688/GrowUp` 的 Settings → Pages → Build and deployment → Source 选择 **Deploy from a branch**，Branch 选择 **wellnest-public-demo**，目录选 **/(root)**，保存。Pages 发布后预期网址：`https://steve6688.github.io/GrowUp/`。

注意：如果该仓库已在使用 GitHub Pages，切换发布分支可能会覆盖现有网站。更推荐将这套文件放到新建的独立 `wellnest` 仓库中再启用 Pages。

## 使用方式

打开 `index.html` 或部署后的站点。iPhone Safari 访问后通过分享菜单中的“添加到主屏幕”固定网页。在 HTTPS 下会注册离线缓存的 Service Worker。

**免责声明**：演示热量及目标数字仅为参考，不能替代个体化医疗、营养或运动建议。
