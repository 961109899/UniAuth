package com.dianrong.common.uniauth.cas.controller;

import com.dianrong.common.uniauth.cas.model.ExpiredSessionObj;
import com.dianrong.common.uniauth.cas.util.CasConstants;
import com.dianrong.common.uniauth.cas.util.UniBundleUtil;
import com.dianrong.common.uniauth.cas.util.WebScopeUtil;
import com.dianrong.common.uniauth.common.bean.Info;
import com.dianrong.common.uniauth.common.bean.InfoName;
import com.dianrong.common.uniauth.common.bean.Response;
import com.dianrong.common.uniauth.common.util.StringUtil;
import com.dianrong.common.uniauth.sharerw.notification.EmailNotification;
import com.dianrong.common.uniauth.sharerw.notification.SmsNotification;
import com.dianrong.common.uniauth.sharerw.notification.exp.NotificationNotAvailableException;
import com.dianrong.common.uniauth.sharerw.notification.exp.SendNotificationFailedException;
import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.WaterRipple;
import com.google.code.kaptcha.util.Config;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 验证相关的controller
 *
 * @author wanglin
 */
@Slf4j
@Controller
@RequestMapping("verification")
public class VerificationController {

  // 邮箱或者短信验证码的长度
  private static final int VERIFICATION_LENGHT = 6;

  @Autowired
  private SmsNotification smsNotify;

  @Autowired
  private EmailNotification emailNotify;

  @Autowired
  private MessageSource messageSource;

  private Producer captchaProducer;

  @PostConstruct
  public void initCaptcha() {
    Properties props = new Properties();
    props.put(Constants.KAPTCHA_BORDER, "no");
    props.put(Constants.KAPTCHA_TEXTPRODUCER_CHAR_STRING, "02345689");
    props.put(Constants.KAPTCHA_BACKGROUND_CLR_TO, Color.WHITE);
    props.put(Constants.KAPTCHA_BACKGROUND_CLR_FROM, Color.WHITE);
    props.put(Constants.KAPTCHA_OBSCURIFICATOR_IMPL, WaterRipple.class.getName());
    props.put(Constants.KAPTCHA_NOISE_COLOR, Color.BLACK);
    Config config = new Config(props);
    captchaProducer = config.getProducerImpl();
  }

  /**
   * 根据session中的identity来发送邮件
   *
   * @return 结果
   */
  @ResponseBody
  @RequestMapping(value = "send/session", method = RequestMethod.GET)
  public Response<Void> sendVerification(HttpServletRequest request, HttpServletResponse response) {
    String identity = WebScopeUtil
        .getValFromSession(request.getSession(), CasConstants.PSWDFORGET_MAIL_VAL_KEY);
    if (!StringUtils.hasText(identity)) {
      return Response.failure(Info.build(InfoName.IDENTITY_REQUIRED, "lack of identity"));
    } else {
      return sendVerification(request, response, identity);
    }
  }

  /**
   * 根据session中的identity来验证验证码
   *
   * @param verifyCode 验证码
   * @return 结果
   */
  @ResponseBody
  @RequestMapping(value = "verify/session", method = RequestMethod.POST)
  public Response<Void> checkVerification(HttpServletRequest request, HttpServletResponse response,
      @RequestParam(value = "verifyCode", required = true) String verifyCode) {
    String identity = WebScopeUtil
        .getValFromSession(request.getSession(), CasConstants.PSWDFORGET_MAIL_VAL_KEY);
    if (!StringUtils.hasText(identity)) {
      return Response.failure(Info.build(InfoName.IDENTITY_REQUIRED, "lack of identity"));
    } else {
      return checkVerification(request, response, identity, verifyCode);
    }
  }

  /**
   * 发送短信或邮件验证码
   *
   * @param identity 发送标识
   * @return 结果
   */
  @ResponseBody
  @RequestMapping(value = "send", method = RequestMethod.POST)
  public Response<Void> sendVerification(HttpServletRequest request, HttpServletResponse response,
      @RequestParam(value = "identity", required = true) String identity) {
    String verification = getVerificationNumber();
    String verificationMsg = UniBundleUtil
        .getMsg(messageSource, "verification.controller.verification.content", verification,
            CasConstants.VERIFICATION_EXPIRED_MINUTES);

    // send email
    if (StringUtil.isEmailAddress(identity)) {
      try {
        String subject = UniBundleUtil
            .getMsg(messageSource, "verification.controller.verification.title");
        emailNotify.send(subject, identity, verificationMsg);
      } catch (NotificationNotAvailableException e1) {
        log.warn("send email is not available", e1);
        return Response.failure(Info.build(InfoName.INTERNAL_ERROR, UniBundleUtil
            .getMsg(messageSource, "verification.controller.verification.not.available", "Email")));
      } catch (SendNotificationFailedException e2) {
        log.warn("send email failed", e2);
        return Response.failure(Info.build(InfoName.INTERNAL_ERROR, UniBundleUtil
            .getMsg(messageSource, "verification.controller.verification.send.failed", "Email")));
      }
      // after sending successfully, set flag to session
      WebScopeUtil.putEmailVerificationToSession(request.getSession(),
          ExpiredSessionObj.build(verification, CasConstants.VERIFICATION_EXPIRED_MILLES));
      return Response.success();
    }

    // send short message
    if (StringUtil.isPhoneNumber(identity)) {
      try {
        smsNotify.send(identity, verificationMsg);
      } catch (NotificationNotAvailableException e1) {
        log.warn("send sms is not available", e1);
        return Response.failure(Info.build(InfoName.INTERNAL_ERROR, UniBundleUtil
            .getMsg(messageSource, "verification.controller.verification.not.available", "SMS")));
      } catch (SendNotificationFailedException e2) {
        log.warn("send sms failed", e2);
        return Response.failure(Info.build(InfoName.INTERNAL_ERROR, UniBundleUtil
            .getMsg(messageSource, "verification.controller.verification.send.failed", "SMS")));
      }
      // after sending successfully, set flag to session
      WebScopeUtil.putSmsVerificationToSession(request.getSession(),
          ExpiredSessionObj.build(verification, CasConstants.VERIFICATION_EXPIRED_MILLES));
      return Response.success();
    }

    // error
    log.error(String.format("%s is not a valid email address or phone number", identity));
    return Response.failure(Info.build(InfoName.BAD_REQUEST, UniBundleUtil
        .getMsg(messageSource, "verification.controller.verification.invalid.indentiy", identity)));
  }

  /**
   * 验证短信或邮件验证码
   *
   * @param identity 标识
   * @param verifyCode 验证码
   * @return 结果
   */
  @ResponseBody
  @RequestMapping(value = "verify", method = RequestMethod.POST)
  public Response<Void> checkVerification(HttpServletRequest request, HttpServletResponse response,
      @RequestParam(value = "identity", required = true) String identity,
      @RequestParam(value = "verifyCode", required = true) String verifyCode) {
    // email
    if (StringUtil.isEmailAddress(identity)) {
      ExpiredSessionObj<String> obj = WebScopeUtil
          .getEmailVerificationFromSession(request.getSession());
      if (obj != null && !obj.isExpired()) {
        if (verifyCode.equals(obj.getContent())) {
          // validation successfully

          // set flag
          WebScopeUtil.setVerificationChecked(request.getSession(), identity);

          // remove email verification
          WebScopeUtil.removeEmailVerification(request.getSession());
          return Response.success();
        }
      }
      // validation failed
      return Response.failure(Info.build(InfoName.BAD_REQUEST, UniBundleUtil
          .getMsg(messageSource, "verification.controller.verification.validate.failed")));
    }

    // short message
    if (StringUtil.isPhoneNumber(identity)) {
      ExpiredSessionObj<String> obj = WebScopeUtil
          .getSmsVerificationFromSession(request.getSession());
      if (obj != null && !obj.isExpired()) {
        if (verifyCode.equals(obj.getContent())) {
          // validation successfully

          // set flag
          WebScopeUtil.setVerificationChecked(request.getSession(), identity);

          // remove email verification
          WebScopeUtil.removeSmsVerification(request.getSession());
          return Response.success();
        }
      }
      // validation failed
      return Response.failure(Info.build(InfoName.BAD_REQUEST, UniBundleUtil
          .getMsg(messageSource, "verification.controller.verification.validate.failed")));
    }

    // error
    log.error(String.format("%s is not a valid email address or phone number", identity));
    return Response.failure(Info.build(InfoName.BAD_REQUEST, UniBundleUtil
        .getMsg(messageSource, "verification.controller.verification.invalid.indentiy", identity)));
  }


  /**
   * image captcha
   */
  @RequestMapping("captcha")
  public void captcha(HttpServletRequest request, HttpServletResponse response) {
    String capText = captchaProducer.createText();
    WebScopeUtil.putCaptchaToSession(request.getSession(), capText);
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Cache-Control", "no-cache");
    response.setContentType("image/jpeg");
    try {
      BufferedImage bi = captchaProducer.createImage(capText);
      ImageIO.write(bi, "jpg", response.getOutputStream());
    } catch (IOException e) {
      log.warn("captcha creation failed", e);
      try {
        response.sendError(500, "captcha creation failed");
      } catch (IOException e1) {
        log.warn("captcha creation failed", e1);
      }
    }
  }

  // 生成数字验
  private String getVerificationNumber() {
    return StringUtil.generateNumberStr(VERIFICATION_LENGHT);
  }
}
