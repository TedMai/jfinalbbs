package com.jfinalbbs.module.index;

import com.jfinal.kit.HashKit;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.upload.UploadFile;
import com.jfinalbbs.common.BaseController;
import com.jfinalbbs.common.Constants;
import com.jfinalbbs.module.label.Label;
import com.jfinalbbs.module.reply.Reply;
import com.jfinalbbs.module.section.Section;
import com.jfinalbbs.module.system.Donate;
import com.jfinalbbs.module.topic.Topic;
import com.jfinalbbs.module.user.User;
import com.jfinalbbs.module.valicode.Valicode;
import com.jfinalbbs.utils.DateUtil;
import com.jfinalbbs.utils.EmailSender;
import com.jfinalbbs.utils.StrUtil;
import com.jfinalbbs.utils.ext.route.ControllerBind;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Tomoya.
 * Copyright (c) 2016, All Rights Reserved.
 * http://jfinalbbs.com
 */
@ControllerBind(controllerKey = "/", viewPath = "page")
public class IndexController extends BaseController {

    /**
     * 首页
     */
    public void index() {
        String tab = getPara("tab", "all");
        String q = getPara("q");
        Integer l = getParaToInt("l", 0);
        if (l != null && l > 0) {
            tab = "all";
            setAttr("_label", Label.me.findById(l));
        }
        if (tab.equals("all") || tab.equals("good")) {
            setAttr("sectionName", "板块");
        } else {
            Section section = Section.me.findByTab(tab);
            setAttr("sectionName", section != null ? section.get("name") : "板块");
        }
        Page<Topic> page = Topic.me.paginate(getParaToInt("p", 1),
                getParaToInt("size", defaultPageSize()), tab, q, 1, l);
        setAttr("page", page);
        List<User> scoreTopTen = User.me.findBySize(10);
        setAttr("scoreTopTen", scoreTopTen);
        setAttr("tab", tab);
        setAttr("q", q);
        setAttr("l", l);
        //查询无人回复的话题
        List<Topic> notReplyTopics = Topic.me.findNotReply(5);
        setAttr("notReplyTopics", notReplyTopics);
        //社区运行状态
        int userCount = User.me.countUsers();
        int topicCount = Topic.me.topicCount();
        int replyCount = Reply.me.replyCount();
        setAttr("userCount", userCount);
        setAttr("topicCount", topicCount);
        setAttr("replyCount", replyCount);
        render("front/index.ftl");
    }

    /**
     * 登出
     */
    public void logout() {
        removeCookie(Constants.USER_COOKIE);
        removeSessionAttr(Constants.USER_SESSION);
        SecurityUtils.getSubject().logout();
        redirect("/");
    }

    /**
     * 后台管理登录
     * 默认账号admin
     * 默认密码123123
     * 对应表 admin_user
     */
    public void adminlogin() {
        String method = getRequest().getMethod();
        if (method.equalsIgnoreCase(Constants.GET)) {
            render("front/adminlogin.ftl");
        } else if (method.equalsIgnoreCase(Constants.POST)) {
            String username = getPara("username");
            String password = getPara("password");
            Subject subject = SecurityUtils.getSubject();
            UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password);
            try {
                subject.login(usernamePasswordToken);
                setSessionAttr(Constants.SESSION_ADMIN_USERNAME, username);
                redirect("/admin/index");
            } catch (AuthenticationException e) {
                e.printStackTrace();
                setAttr("error", "用户名或密码错误");
                render("front/adminlogin.ftl");
            }
        }
    }

    public void login() {
        String method = getRequest().getMethod();
        if (method.equalsIgnoreCase(Constants.GET)) {
            render("front/user/login.ftl");
        } else if (method.equalsIgnoreCase(Constants.POST)) {
            String email = getPara("email");
            String password = getPara("password");
            if (StrUtil.isBlank(email) || StrUtil.isBlank(password)) {
                error("邮箱/密码都不能为空");
            } else {
                User user = User.me.localLogin(email, HashKit.md5(password));
                if (user == null) {
                    error("邮箱/密码错误");
                } else {
                    setSessionAttr(Constants.USER_SESSION, user);
                    setCookie(Constants.USER_COOKIE, StrUtil.getEncryptionToken(user.getStr("token")), 30 * 24 * 60 * 60);
                    success();
                }
            }
        }
    }

    public void reg() {
        String method = getRequest().getMethod();
        if (method.equalsIgnoreCase(Constants.GET)) {
            String third = getPara("third");
            if (StrUtil.isBlank(third)) {
                removeSessionAttr("open_id");
                removeSessionAttr("thirdlogin_type");
                removeSessionAttr("unsave_user");
            }
            render("front/user/reg.ftl");
        } else if (method.equalsIgnoreCase(Constants.POST)) {
            String email = getPara("reg_email");
            String password = getPara("reg_password");
            String nickname = getPara("reg_nickname");
            String valicode = getPara("valicode");
            String open_id = (String) getSession().getAttribute("open_id");
            if (StrUtil.isBlank(email) || StrUtil.isBlank(password) || StrUtil.isBlank(nickname) || StrUtil.isBlank(valicode)) {
                error("请完善注册信息");
            } else {
                if (!StrUtil.isEmail(email)) {
                    error("请输入正确的邮箱地址");
                } else {
                    Valicode code = Valicode.me.findByCodeAndEmail(valicode, email, Constants.REG);
                    if (code == null) {
                        error("验证码不存在或已使用(已过期)");
                    } else {
                        User user = User.me.findByEmail(email);
                        if (user != null) {
                            error("邮箱已经注册，请直接登录");
                        } else if (User.me.findByNickname(nickname) != null) {
                            error("昵称已经被注册，请更换其他昵称");
                        } else {
                            String uuid = StrUtil.getUUID();
                            String token = StrUtil.getUUID();
                            Date date = new Date();
                            if (StrUtil.isBlank(open_id)) {
                                user = new User();
                                user.set("id", uuid)
                                        .set("nickname", StrUtil.noHtml(nickname).trim())
                                        .set("password", HashKit.md5(password))
                                        .set("score", 0)
                                        .set("mission", date)
                                        .set("in_time", date)
                                        .set("email", email)
                                        .set("token", token)
                                        .set("avatar", "/static/img/default_avatar.png")
                                        .save();
                            } else {
                                user = getSessionAttr("unsave_user");
                                if (user == null) {
                                    user = new User();
                                    user.set("id", uuid)
                                            .set("nickname", StrUtil.noHtml(nickname).trim())
                                            .set("password", HashKit.md5(password))
                                            .set("score", 0)
                                            .set("mission", date)
                                            .set("in_time", date)
                                            .set("email", email)
                                            .set("token", token)
                                            .set("avatar", "/static/img/default_avatar.png")
                                            .save();
                                } else {
                                    user.set("nickname", StrUtil.noHtml(nickname).trim())
                                            .set("password", HashKit.md5(password))
                                            .set("mission", date)
                                            .set("email", email)
                                            .set("token", token)
                                            .set("in_time", date)
                                            .set("score", 0)
                                            .set("avatar", "/static/img/default_avatar.png")
                                            .save();
                                }
                                removeSessionAttr("unsave_user");
                                removeSessionAttr("open_id");
                                removeSessionAttr("thirdlogin_type");
                            }
                            setSessionAttr(Constants.USER_SESSION, user);
                            setCookie(Constants.USER_COOKIE, StrUtil.getEncryptionToken(user.getStr("token")), 30 * 24 * 60 * 60);
                            //更新验证状态
                            code.set("status", 1).update();
                            success();
                        }
                    }
                }
            }
        }
    }

    public void sendValiCode() {
        String email = getPara("email");
        if (StrUtil.isBlank(email)) {
            error("邮箱不能为空");
        } else if (!StrUtil.isEmail(email)) {
            error("邮箱格式不正确");
        } else {
            String type = getPara("type");
            String valicode = StrUtil.randomString(6);
            if (type.equalsIgnoreCase(Constants.FORGET_PWD)) {
                User user = User.me.findByEmail(email);
                if (user == null) {
                    error("该邮箱未被注册，请先注册");
                } else {
                    Valicode code = new Valicode();
                    code.set("code", valicode)
                            .set("type", type)
                            .set("in_time", new Date())
                            .set("status", 0)
                            .set("expire_time", DateUtil.getMinuteAfter(new Date(), 30))
                            .set("target", email)
                            .save();
                    StringBuffer mailBody = new StringBuffer();
                    mailBody.append("You retrieve the password verification code is: ")
                            .append(valicode)
                            .append("<br/>The code can only be used once, and only valid for 30 minutes.");
                    EmailSender.sendMail("JFinalbbs－Forgot password codes",
                            new String[]{email}, mailBody.toString());
                    success();
                }
            } else if (type.equalsIgnoreCase(Constants.REG)) {
                User user = User.me.findByEmail(email);
                if (user != null) {
                    error("邮箱已经注册，请直接登录");
                } else {
                    Valicode code = new Valicode();
                    code.set("code", valicode)
                            .set("type", type)
                            .set("in_time", new Date())
                            .set("status", 0)
                            .set("expire_time", DateUtil.getMinuteAfter(new Date(), 30))
                            .set("target", email)
                            .save();
                    StringBuffer mailBody = new StringBuffer();
                    mailBody.append("Register your account verification code is: ")
                            .append(valicode)
                            .append("<br/>The code can only be used once, and only valid for 30 minutes.");
                    EmailSender.sendMail("JFinalbbs－Registered Account codes", new String[]{email}, mailBody.toString());
                    success();
                }
            }
        }
    }

    public void forgetpwd() {
        String method = getRequest().getMethod();
        if (method.equalsIgnoreCase(Constants.GET)) {
            render("front/user/forgetpwd.ftl");
        } else if (method.equalsIgnoreCase(Constants.POST)) {
            String email = getPara("email");
            String valicode = getPara("valicode");
            String newpwd = getPara("newpwd");
            if (StrUtil.isBlank(email) || StrUtil.isBlank(valicode) || StrUtil.isBlank(newpwd)) {
                error("请完善信息");
            } else {
                Valicode code = Valicode.me.findByCodeAndEmail(valicode, email, Constants.FORGET_PWD);
                if (code == null) {
                    error("验证码不存在或已使用(已过期)");
                } else {
                    User user = User.me.findByEmail(email);
                    if (user == null) {
                        error("该邮箱未被注册，请先注册");
                    } else {
                        user.set("password", HashKit.md5(newpwd)).update();
                        code.set("status", 1).update();
                        success();
                    }
                }
            }
        }
    }

    public void upload() {
        List<UploadFile> uploadFiles = getFiles("imgs");
//        System.out.println(uploadFile.getOriginalFileName());//图片原来的名字
//        System.out.println(uploadFile.getFileName());//图片保存到服务器的名字
        List<String> imgFiles = new ArrayList<String>();
        for (UploadFile uf : uploadFiles) {
            String contentType = uf.getContentType();
            String suffix = "." + contentType.split("/")[1];
            String newName = StrUtil.randomString(16);
            File file = new File(uf.getUploadPath() + "/" + uf.getFileName());
            file.renameTo(new File(uf.getUploadPath() + "/" + newName + suffix));
            imgFiles.add("/static/upload/imgs/" + newName + suffix);
        }
        if (imgFiles.size() == 1) {
            renderText(imgFiles.get(0));
        } else {
            renderText(imgFiles.toString());
        }
    }

    public void api() {
        render("front/api.ftl");
    }

    public void service() {
        render("front/service.ftl");
    }

    public void donate() {
        List<Donate> donates = Donate.me.find("select * from jfbbs_donate order by in_time desc");
        setAttr("donates", donates);
        render("front/donate.ftl");
    }
}