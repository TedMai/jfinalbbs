e## 友情提醒: 使用jfinalbbs的代码搭建自己的社区网站,不能删除页面底部的 powered by [JFinalbbs](http://jfinalbbs.com) ,如果嫌位置不好看,可以在页面其他显眼的地方加上提供者:[JFinalbbs](http://jfinalbbs.com), 谢谢配合

# 如何开始


- clone代码
- 将`doc/最新版SQL.sql`脚本在mysql数据库里运行，创建jfinalbbs数据库
- 修改`src/main/resources`下的`config.properties`文件里的数据库连接
- 使用jetty运行,命令:`mvn jetty:run`(前提是电脑上安装了maven)
- 打开浏览器,输入`http://localhost:8080`回车
- 后台访问路径`http://localhost:8080/admin/index`,默认密码: 123123

#### war包放在tomcat里运行

- clone代码
- 将`doc/最新版SQL.sql`脚本在mysql数据库里运行，创建jfinalbbs数据库
- 修改`src/main/resources`下的`config.properties`文件里的数据库连接
- 使用maven命令:`mvn clean package`,等待编译打包,成功后打开target文件夹,会有一个jfinalbbs.war
- [下载tomcat](http://tomcat.apache.org),解压
- 将war包放到`tomcat/webapp`下,启动tomcat(`./bin/startup.sh`)
- 打开浏览器,输入`http://localhost:8080/jfinalbbs`回车
- 后台访问路径`http://localhost:8080/jfinalbbs/admin/index`,默认密码: 123123

### 开发环境部署

- 使用git将master分支的代码down下来
- 使用maven编译
- 运行`JFinalbbsConfig.java`类里的main方法
- 浏览器访问`http://localhost:8080/`
- 后台访问路径`http://localhost:8080/admin/index`,默认密码: 123123

# 碰到问题怎么办?

1. 加QQ群 419343003 ,阅读群公告后可以提问题
2. 在Github上提 Issues
3. 提问题的时候请将问题重现步骤描述清楚,对于截个图什么也不说,发一句话就了事的,我会看心情回答(你懂的!)

# 贡献代码

目前jfinalbbs就一个`master`分支,就朋也一个人在开发维护,如果想分享自己拓展的功能等,可以给朋也的`master`分支推送`pull request`

# 2016年3月24日 V2.2更新内容

1. 后台增加shiro权限控制（使用了jfinal-ext）
2. 后台标签可以增加/编辑
3. 前台登录另起一个页面
4. 编辑器升级,可以直接拷贝word文档粘贴
5. controller, table 增加了自动扫描注入（使用了jfinal-ext）
6. 删除baseUrl从后台设置, 改用`me.add(new ContextPathHandler("baseUrl"));`方式

## 数据库更新

- 增加表: jfbbs_role, jfbbs_user_role, jfbbs_permission, jfbbs_role_permission, jfbbs_admin_log
- 修改了: jfbbs_admin_user增加字段: salt, in_time
- 修改了: jfbbs_topic删除字段: original_url, reposted, 增加字段: isdelete
- **具体对照最新数据库脚本 `doc/最新版SQL.sql`**

# 截图欣赏

![](http://7xj5k8.com1.z0.glb.clouddn.com/QQ20160324-0.png)

**更新体验[传送门](http://jfinalbbs.com)**

其他版本更新日志参见 `doc/更新日志.md`

![](http://jfinalbbs.com/static/upload/imgs/381938b1861da719.jpeg)

以上
