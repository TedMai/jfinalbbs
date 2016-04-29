package com.jfinalbbs.module.notification;

import com.jfinal.aop.Before;
import com.jfinalbbs.common.BaseController;
import com.jfinalbbs.common.Constants;
import com.jfinalbbs.interceptor.UserInterceptor;
import com.jfinalbbs.module.user.User;
import com.jfinalbbs.utils.ext.route.ControllerBind;

/**
 * Created by Tomoya.
 * Copyright (c) 2016, All Rights Reserved.
 * http://jfinalbbs.com
 */
@ControllerBind(controllerKey = "/notification", viewPath = "page")
public class NotificationController extends BaseController {

    @Before(UserInterceptor.class)
    public void countnotread() {
        User user = getSessionAttr(Constants.USER_SESSION);
        if (user == null) {
            error(Constants.DESC_FAILURE);
        } else {
            try {
                int count = Notification.me.countNotRead(user.getStr("id"));
                success(count);
            } catch (Exception e) {
                e.printStackTrace();
                error(Constants.DESC_FAILURE);
            }
        }
    }
}
