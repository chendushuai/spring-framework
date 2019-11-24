package com.chenss.controller;

import com.chenss.dao.UserInfoParam;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/test")
public class ChenssTestController {
    @RequestMapping("/test.do")
    @ResponseBody
    public UserInfoParam test(String name, HttpServletRequest httpRequest, HttpServletResponse httpResponse, UserInfoParam userInfoParam) {
        System.out.println(String.format("name:%s", name));
        System.out.println(String.format("HttpServletRequest:%s", httpRequest));
        System.out.println(String.format("HttpServletResponse:%s", httpResponse));
        System.out.println(String.format("userInfoParam:%s", userInfoParam));
        return userInfoParam;
    }
    @RequestMapping("/model.do")
    public String model() {
        return "index";
    }

	@RequestMapping("/modelandview.do")
    public ModelAndView modelAndView() {
		System.out.println("this is ModelAndView");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("index");
		modelAndView.getModel().put("chenss","smart");
		return modelAndView;
	}
}
