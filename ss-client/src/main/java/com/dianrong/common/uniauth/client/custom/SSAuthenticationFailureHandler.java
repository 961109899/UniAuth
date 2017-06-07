package com.dianrong.common.uniauth.client.custom;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import com.dianrong.common.uniauth.client.custom.model.JsonResponseModel;
import com.dianrong.common.uniauth.common.client.DomainDefine;
import com.dianrong.common.uniauth.common.util.HttpRequestUtil;
import com.dianrong.common.uniauth.common.util.JsonUtil;
import com.dianrong.common.uniauth.common.util.ZkNodeUtils;

/**
 * cas生成st(service ticket)跳转到业务系统，业务系统拿到st去cas做验证的时候验证失败的处理<br/>
 * 如：1.返回json告知失败;2.跳转到业务系统的认证失败页面；
 *
 * @author xiaofeng.chen@dianrong.com
 * @date 2016年12月15日
 * @see CasAuthenticationFilter#setAuthenticationFailureHandler(org.springframework.security.web.authentication.AuthenticationFailureHandler)
 * CasAuthenticationFilter#setAuthenticationFailureHandler
 * @since jdk1.7
 */

public class SSAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private final static Logger logger = LoggerFactory
      .getLogger(SSAuthenticationFailureHandler.class);
  @Resource(name = "uniauthConfig")
  private Map<String, String> allZkNodeMap;
  @Autowired(required = false)
  private DomainDefine domainDefine;

  public void setAllZkNodeMap(Map<String, String> allZkNodeMap) {
    this.allZkNodeMap = allZkNodeMap;
  }

  public void setDomainDefine(DomainDefine domainDefine) {
    this.domainDefine = domainDefine;
  }

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {
    logger.error("-----------Service Ticket Authentication Failed-------------------:{}",
        exception.getMessage());
    SecurityContextHolder.getContext().setAuthentication(null);
    if (HttpRequestUtil.isAjaxRequest(request) || HttpRequestUtil.isCORSRequest(request)) {
      response.setContentType("application/json;charset=UTF-8");
      response.addHeader("Cache-Control", "no-store");
      response.getWriter().write(
          JsonUtil.object2Jason(JsonResponseModel.failure("Service ticket validation failed")));
      response.flushBuffer();
    } else {
      String authFailureUrl = authFailureUrl();
      if (!StringUtils.isBlank(authFailureUrl)) {
        response.sendRedirect(authFailureUrl);
      } else {
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().print(
            "Service ticket validation failed.  <a href='" + request.getContextPath()
                + "'>Retry</a>");
        response.flushBuffer();
      }
    }
  }

  private String authFailureUrl() {
    String authFailureUrl = null;
    if (domainDefine != null) {
      String failUrlNodeKey = ZkNodeUtils.domainAuthFailUrlNodeKey(domainDefine.getDomainCode());
      authFailureUrl = allZkNodeMap.get(failUrlNodeKey);
    }
    return authFailureUrl;
  }

}
